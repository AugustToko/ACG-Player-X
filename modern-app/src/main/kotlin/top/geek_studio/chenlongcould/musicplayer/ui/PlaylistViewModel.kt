package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportResult
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistTransferRepository
import top.geek_studio.chenlongcould.musicplayer.data.UserPlaylist
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.playback.PlayerConnection

data class PlaylistUiState(
    val playlists: List<UserPlaylist> = emptyList(),
    val activePlaylistId: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isWorking: Boolean = false,
) {
    val activePlaylist: UserPlaylist?
        get() = playlists.firstOrNull { it.id == activePlaylistId }
}

class PlaylistViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = PlaylistRepository(application)
    private val transferRepository = PlaylistTransferRepository(application)
    private val playerConnection = PlayerConnection(application, viewModelScope)

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private var pendingOperations = 0

    init {
        viewModelScope.launch {
            repository.playlists.collect { playlists ->
                _uiState.update { state ->
                    state.copy(
                        playlists = playlists,
                        activePlaylistId =
                            state.activePlaylistId?.takeIf { activeId ->
                                playlists.any { it.id == activeId }
                            },
                    )
                }
            }
        }
    }

    fun openPlaylist(playlistId: String) {
        _uiState.update { state ->
            state.copy(
                activePlaylistId = playlistId.takeIf { id -> state.playlists.any { it.id == id } },
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun closePlaylist() {
        _uiState.update {
            it.copy(
                activePlaylistId = null,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun createPlaylist(name: String) {
        launchMutation {
            val created = repository.createPlaylist(name)
            _uiState.update { it.copy(activePlaylistId = created.id) }
        }
    }

    fun importPlaylist(
        uri: Uri,
        librarySongs: List<Song>,
    ) {
        launchMutation {
            val imported = transferRepository.importPlaylist(uri, librarySongs)
            require(imported.mediaIds.isNotEmpty()) {
                buildImportFailureMessage(imported)
            }
            val created =
                repository.createImportedPlaylist(
                    name = imported.preferredName,
                    mediaIds = imported.mediaIds,
                )
            _uiState.update {
                it.copy(
                    activePlaylistId = created.id,
                    infoMessage = buildImportSuccessMessage(imported),
                )
            }
        }
    }

    fun exportPlaylist(
        playlistId: String,
        uri: Uri,
        librarySongs: List<Song>,
    ) {
        launchMutation {
            val playlist =
                _uiState.value.playlists.firstOrNull { it.id == playlistId }
                    ?: throw IllegalArgumentException("要导出的歌单已不存在")
            val result = transferRepository.exportPlaylist(uri, playlist, librarySongs)
            _uiState.update {
                it.copy(
                    infoMessage =
                        buildString {
                            append("已导出 ${result.exportedCount} 项到 M3U8")
                            if (result.unavailableCount > 0) {
                                append("，其中 ${result.unavailableCount} 项当前不可用，已保留 ACG Player ID")
                            }
                        },
                )
            }
        }
    }

    fun renamePlaylist(
        playlistId: String,
        name: String,
    ) {
        launchMutation {
            repository.renamePlaylist(playlistId, name)
        }
    }

    fun deletePlaylist(playlistId: String) {
        launchMutation {
            repository.deletePlaylist(playlistId)
            _uiState.update { state ->
                state.copy(
                    activePlaylistId =
                        state.activePlaylistId.takeUnless { it == playlistId },
                )
            }
        }
    }

    fun addSongs(
        playlistId: String,
        mediaIds: Collection<String>,
    ) {
        launchMutation {
            repository.addSongs(playlistId, mediaIds)
        }
    }

    fun removeSong(
        playlistId: String,
        mediaId: String,
    ) {
        launchMutation {
            repository.removeSong(playlistId, mediaId)
        }
    }

    fun swapSongs(
        playlistId: String,
        firstMediaId: String,
        secondMediaId: String,
    ) {
        launchMutation {
            repository.swapSongs(playlistId, firstMediaId, secondMediaId)
        }
    }

    fun clearSongs(playlistId: String) {
        launchMutation {
            repository.clearSongs(playlistId)
        }
    }

    fun removeUnavailableSongs(
        playlistId: String,
        availableMediaIds: Set<String>,
    ) {
        launchMutation {
            repository.removeUnavailableSongs(playlistId, availableMediaIds)
        }
    }

    fun playSong(
        song: Song,
        queue: List<Song>,
    ) {
        playerConnection.play(song, queue.ifEmpty { listOf(song) })
    }

    fun playAll(
        queue: List<Song>,
        shuffle: Boolean,
    ) {
        val playableQueue = if (shuffle) queue.shuffled() else queue
        playableQueue.firstOrNull()?.let { first ->
            playerConnection.play(first, playableQueue)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    override fun onCleared() {
        playerConnection.close()
        super.onCleared()
    }

    private fun launchMutation(block: suspend () -> Unit) {
        viewModelScope.launch {
            pendingOperations += 1
            _uiState.update {
                it.copy(
                    isWorking = true,
                    errorMessage = null,
                    infoMessage = null,
                )
            }
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        errorMessage = throwable.localizedMessage ?: "歌单操作失败",
                    )
                }
            } finally {
                pendingOperations = (pendingOperations - 1).coerceAtLeast(0)
                _uiState.update { it.copy(isWorking = pendingOperations > 0) }
            }
        }
    }
}

private fun buildImportSuccessMessage(result: PlaylistImportResult): String =
    buildString {
        append("已导入 ${result.mediaIds.size} 项")
        if (result.matchedCount > 0) append("，匹配 ${result.matchedCount} 首当前歌曲")
        if (result.preservedUnavailableCount > 0) {
            append("，保留 ${result.preservedUnavailableCount} 个暂不可用 ACG 项")
        }
        val skipped = result.unmatchedCount + result.ambiguousCount
        if (skipped > 0) append("，跳过 $skipped 条无法可靠匹配的外部路径")
        if (result.duplicateCount > 0) append("，去重 ${result.duplicateCount} 条")
        if (result.truncated) append("；文件条目超过上限，已截断")
    }

private fun buildImportFailureMessage(result: PlaylistImportResult): String =
    buildString {
        append("未能把歌单条目匹配到当前音乐库")
        if (result.ambiguousCount > 0) append("；${result.ambiguousCount} 条存在多个候选")
        if (result.unmatchedCount > 0) append("；${result.unmatchedCount} 条路径或元数据不匹配")
        append("。请先恢复对应 MediaStore 或 SAF 音乐来源后重试")
    }
