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
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportPreview
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportResult
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistMoveDestination
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistTransferRepository
import top.geek_studio.chenlongcould.musicplayer.data.UserPlaylist
import top.geek_studio.chenlongcould.musicplayer.data.finalizePlaylistImport
import top.geek_studio.chenlongcould.musicplayer.data.movePlaylistMediaIds
import top.geek_studio.chenlongcould.musicplayer.data.removePlaylistMediaIds
import top.geek_studio.chenlongcould.musicplayer.data.swapPlaylistMediaIds
import top.geek_studio.chenlongcould.musicplayer.data.updatePlaylistImportName
import top.geek_studio.chenlongcould.musicplayer.data.updatePlaylistImportSelection
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.playback.PlayerConnection

data class PlaylistUiState(
    val playlists: List<UserPlaylist> = emptyList(),
    val activePlaylistId: String? = null,
    val importPreview: PlaylistImportPreview? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val undoMessage: String? = null,
    val undoToken: Long = 0L,
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
    private var undoSequence = 0L
    private var pendingUndo: PlaylistUndoSnapshot? = null

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
                pendingUndo?.let { undo ->
                    if (playlists.none { it.id == undo.playlistId }) {
                        clearUndoState()
                    }
                }
            }
        }
    }

    fun openPlaylist(playlistId: String) {
        clearUndoState()
        _uiState.update { state ->
            state.copy(
                activePlaylistId = playlistId.takeIf { id -> state.playlists.any { it.id == id } },
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun closePlaylist() {
        clearUndoState()
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
            val preview = transferRepository.prepareImport(uri, librarySongs)
            _uiState.update {
                it.copy(
                    importPreview = preview,
                    infoMessage = null,
                )
            }
        }
    }

    fun updateImportPreviewName(name: String) {
        _uiState.update { state ->
            val preview = state.importPreview ?: return@update state
            state.copy(
                importPreview = updatePlaylistImportName(preview, name),
                errorMessage = null,
            )
        }
    }

    fun updateImportEntrySelection(
        entryIndex: Int,
        mediaId: String?,
    ) {
        _uiState.update { state ->
            val preview = state.importPreview ?: return@update state
            state.copy(
                importPreview =
                    updatePlaylistImportSelection(
                        preview = preview,
                        entryIndex = entryIndex,
                        mediaId = mediaId,
                    ),
                errorMessage = null,
            )
        }
    }

    fun cancelImportPreview() {
        _uiState.update {
            it.copy(
                importPreview = null,
                errorMessage = null,
            )
        }
    }

    fun confirmImportPreview() {
        val preview = _uiState.value.importPreview ?: return
        launchMutation {
            val imported = finalizePlaylistImport(preview)
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
                    importPreview = null,
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
        removeSongs(playlistId, listOf(mediaId))
    }

    fun removeSongs(
        playlistId: String,
        mediaIds: Collection<String>,
    ) {
        val requested = mediaIds.asSequence().filter(String::isNotBlank).toSet()
        if (requested.isEmpty()) return

        mutatePlaylistMediaIds(playlistId) { current ->
            val updated = removePlaylistMediaIds(current, requested)
            val removedCount = current.size - updated.size
            PlaylistMediaEdit(
                mediaIds = updated,
                undoMessage =
                    if (removedCount == 1) {
                        "已从歌单移出 1 首歌曲"
                    } else {
                        "已从歌单移出 $removedCount 首歌曲"
                    },
            )
        }
    }

    fun swapSongs(
        playlistId: String,
        firstMediaId: String,
        secondMediaId: String,
    ) {
        mutatePlaylistMediaIds(playlistId) { current ->
            PlaylistMediaEdit(
                mediaIds =
                    swapPlaylistMediaIds(
                        current = current,
                        firstMediaId = firstMediaId,
                        secondMediaId = secondMediaId,
                    ),
                undoMessage = "已调整歌曲顺序",
            )
        }
    }

    fun moveSongsToStart(
        playlistId: String,
        mediaIds: Collection<String>,
    ) {
        moveSongs(
            playlistId = playlistId,
            mediaIds = mediaIds,
            destination = PlaylistMoveDestination.START,
            undoMessage = "已将所选歌曲移到歌单顶部",
        )
    }

    fun moveSongsToEnd(
        playlistId: String,
        mediaIds: Collection<String>,
    ) {
        moveSongs(
            playlistId = playlistId,
            mediaIds = mediaIds,
            destination = PlaylistMoveDestination.END,
            undoMessage = "已将所选歌曲移到歌单底部",
        )
    }

    fun reorderSongs(
        playlistId: String,
        orderedMediaIds: List<String>,
    ) {
        mutatePlaylistMediaIds(playlistId) { current ->
            val normalized =
                orderedMediaIds
                    .asSequence()
                    .filter(String::isNotBlank)
                    .distinct()
                    .toList()
            require(
                normalized.size == current.size &&
                    normalized.toSet() == current.toSet(),
            ) { "排序结果与当前歌单内容不一致" }

            PlaylistMediaEdit(
                mediaIds = normalized,
                undoMessage = "已通过拖拽调整歌单顺序",
            )
        }
    }

    fun clearSongs(playlistId: String) {
        mutatePlaylistMediaIds(playlistId) { _ ->
            PlaylistMediaEdit(
                mediaIds = emptyList(),
                undoMessage = "已清空歌单",
            )
        }
    }

    fun removeUnavailableSongs(
        playlistId: String,
        availableMediaIds: Set<String>,
    ) {
        mutatePlaylistMediaIds(playlistId) { current ->
            val updated = current.filter(availableMediaIds::contains)
            val removedCount = current.size - updated.size
            PlaylistMediaEdit(
                mediaIds = updated,
                undoMessage = "已清理 $removedCount 个当前不可用项目",
            )
        }
    }

    fun undoLastPlaylistMutation() {
        val undo = pendingUndo ?: return
        launchMutation(clearUndoAtStart = false) {
            repository.replaceSongs(undo.playlistId, undo.mediaIds)
            pendingUndo = null
            _uiState.update {
                it.copy(
                    undoMessage = null,
                    infoMessage = "已撤销上一步歌单修改",
                )
            }
        }
    }

    fun clearUndoMessage() {
        clearUndoState()
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

    private fun moveSongs(
        playlistId: String,
        mediaIds: Collection<String>,
        destination: PlaylistMoveDestination,
        undoMessage: String,
    ) {
        val requested = mediaIds.asSequence().filter(String::isNotBlank).toSet()
        if (requested.isEmpty()) return

        mutatePlaylistMediaIds(playlistId) { current ->
            PlaylistMediaEdit(
                mediaIds =
                    movePlaylistMediaIds(
                        current = current,
                        movingMediaIds = requested,
                        destination = destination,
                    ),
                undoMessage = undoMessage,
            )
        }
    }

    private fun mutatePlaylistMediaIds(
        playlistId: String,
        transform: (List<String>) -> PlaylistMediaEdit,
    ) {
        launchMutation {
            val before =
                _uiState.value.playlists.firstOrNull { it.id == playlistId }
                    ?: throw IllegalArgumentException("要编辑的歌单已不存在")
            val edit = transform(before.mediaIds)
            if (edit.mediaIds == before.mediaIds) return@launchMutation

            repository.replaceSongs(
                playlistId = playlistId,
                mediaIds = edit.mediaIds,
            )
            publishUndo(
                playlist = before,
                message = edit.undoMessage,
            )
        }
    }

    private fun publishUndo(
        playlist: UserPlaylist,
        message: String,
    ) {
        undoSequence =
            if (undoSequence == Long.MAX_VALUE) {
                1L
            } else {
                undoSequence + 1L
            }
        pendingUndo =
            PlaylistUndoSnapshot(
                playlistId = playlist.id,
                mediaIds = playlist.mediaIds,
            )
        _uiState.update {
            it.copy(
                undoMessage = message,
                undoToken = undoSequence,
            )
        }
    }

    private fun clearUndoState() {
        pendingUndo = null
        _uiState.update { it.copy(undoMessage = null) }
    }

    private fun launchMutation(
        clearUndoAtStart: Boolean = true,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            pendingOperations += 1
            if (clearUndoAtStart) {
                clearUndoState()
            }
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

    private data class PlaylistMediaEdit(
        val mediaIds: List<String>,
        val undoMessage: String,
    )

    private data class PlaylistUndoSnapshot(
        val playlistId: String,
        val mediaIds: List<String>,
    )
}

private fun buildImportSuccessMessage(result: PlaylistImportResult): String =
    buildString {
        append("已导入 ${result.mediaIds.size} 项")
        if (result.matchedCount > 0) append("，自动匹配 ${result.matchedCount} 首")
        if (result.manuallyMappedCount > 0) append("，手工映射 ${result.manuallyMappedCount} 首")
        if (result.preservedUnavailableCount > 0) {
            append("，保留 ${result.preservedUnavailableCount} 个暂不可用 ACG 项")
        }
        val unresolved = result.unmatchedCount + result.ambiguousCount
        if (unresolved > 0) append("，跳过 $unresolved 条仍未匹配的外部路径")
        if (result.skippedCount > 0) append("，排除 ${result.skippedCount} 条")
        if (result.duplicateCount > 0) append("，去重 ${result.duplicateCount} 条")
        if (result.truncated) append("；文件条目超过上限，已截断")
    }

private fun buildImportFailureMessage(result: PlaylistImportResult): String =
    buildString {
        append("预览中没有选择可导入的歌曲")
        if (result.ambiguousCount > 0) append("；${result.ambiguousCount} 条仍存在多个候选")
        if (result.unmatchedCount > 0) append("；${result.unmatchedCount} 条路径或元数据未匹配")
        append("。请手工映射至少一项，或恢复对应 MediaStore / SAF 音乐来源")
    }
