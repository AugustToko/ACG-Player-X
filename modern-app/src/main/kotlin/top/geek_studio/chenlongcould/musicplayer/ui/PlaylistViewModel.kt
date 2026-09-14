package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistRepository
import top.geek_studio.chenlongcould.musicplayer.data.UserPlaylist
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.playback.PlayerConnection

data class PlaylistUiState(
    val playlists: List<UserPlaylist> = emptyList(),
    val activePlaylistId: String? = null,
    val errorMessage: String? = null,
    val isWorking: Boolean = false,
) {
    val activePlaylist: UserPlaylist?
        get() = playlists.firstOrNull { it.id == activePlaylistId }
}

class PlaylistViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = PlaylistRepository(application)
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
            )
        }
    }

    fun closePlaylist() {
        _uiState.update { it.copy(activePlaylistId = null, errorMessage = null) }
    }

    fun createPlaylist(name: String) {
        launchMutation {
            val created = repository.createPlaylist(name)
            _uiState.update { it.copy(activePlaylistId = created.id) }
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

    override fun onCleared() {
        playerConnection.close()
        super.onCleared()
    }

    private fun launchMutation(block: suspend () -> Unit) {
        viewModelScope.launch {
            pendingOperations += 1
            _uiState.update { it.copy(isWorking = true, errorMessage = null) }
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
