package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.geek_studio.chenlongcould.musicplayer.data.MusicRepository
import top.geek_studio.chenlongcould.musicplayer.data.SettingsRepository
import top.geek_studio.chenlongcould.musicplayer.data.ThemeMode
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.playback.PlaybackUiState
import top.geek_studio.chenlongcould.musicplayer.playback.PlayerConnection

enum class LibrarySection {
    SONGS,
    ALBUMS,
    ARTISTS,
}

data class MainUiState(
    val songs: List<Song> = emptyList(),
    val totalSongCount: Int = 0,
    val query: String = "",
    val section: LibrarySection = LibrarySection.SONGS,
    val permissionChecked: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val playback: PlaybackUiState = PlaybackUiState(),
)

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val musicRepository = MusicRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val playerConnection = PlayerConnection(application, viewModelScope)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var library: List<Song> = emptyList()
    private var hasLoadedLibrary = false

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            playerConnection.state.collect { playback ->
                _uiState.update { it.copy(playback = playback) }
            }
        }
    }

    fun onAudioPermissionChanged(granted: Boolean) {
        _uiState.update {
            it.copy(
                permissionChecked = true,
                hasAudioPermission = granted,
                errorMessage = if (granted) it.errorMessage else null,
            )
        }

        if (granted && !hasLoadedLibrary) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        if (!_uiState.value.hasAudioPermission || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching { musicRepository.loadSongs() }
                .onSuccess { songs ->
                    library = songs
                    hasLoadedLibrary = true
                    publishFilteredLibrary()
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "无法读取本地音乐库",
                        )
                    }
                }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        publishFilteredLibrary()
    }

    fun showSection(section: LibrarySection) {
        _uiState.update { it.copy(section = section) }
    }

    fun openAlbum(album: String) {
        _uiState.update {
            it.copy(
                query = album,
                section = LibrarySection.SONGS,
            )
        }
        publishFilteredLibrary()
    }

    fun openArtist(artist: String) {
        _uiState.update {
            it.copy(
                query = artist,
                section = LibrarySection.SONGS,
            )
        }
        publishFilteredLibrary()
    }

    fun play(song: Song) {
        playerConnection.play(song, library)
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()

    fun skipNext() = playerConnection.skipNext()

    fun skipPrevious() = playerConnection.skipPrevious()

    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

    fun toggleShuffle() = playerConnection.toggleShuffle()

    fun cycleRepeatMode() = playerConnection.cycleRepeatMode()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        playerConnection.close()
        super.onCleared()
    }

    private fun publishFilteredLibrary() {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                songs = filterSongs(library, current.query),
                totalSongCount = library.size,
            )
        }
    }
}
