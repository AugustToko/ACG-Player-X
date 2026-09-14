package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.geek_studio.chenlongcould.musicplayer.data.AuthorizedFolder
import top.geek_studio.chenlongcould.musicplayer.data.AuthorizedFolderRepository
import top.geek_studio.chenlongcould.musicplayer.data.LibraryStateRepository
import top.geek_studio.chenlongcould.musicplayer.data.MusicRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStats
import top.geek_studio.chenlongcould.musicplayer.data.SettingsRepository
import top.geek_studio.chenlongcould.musicplayer.data.ThemeMode
import top.geek_studio.chenlongcould.musicplayer.data.resolveMostPlayedSongs
import top.geek_studio.chenlongcould.musicplayer.data.resolveRecentSongs
import top.geek_studio.chenlongcould.musicplayer.data.resolveRecentlyAddedSongs
import top.geek_studio.chenlongcould.musicplayer.data.resolveUnplayedSongs
import top.geek_studio.chenlongcould.musicplayer.lyrics.LyricsRepository
import top.geek_studio.chenlongcould.musicplayer.lyrics.LyricsUiState
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.playback.PlaybackUiState
import top.geek_studio.chenlongcould.musicplayer.playback.PlayerConnection
import top.geek_studio.chenlongcould.musicplayer.shortcuts.AppShortcuts

enum class AppDestination {
    LIBRARY,
    NOW_PLAYING,
    SETTINGS,
}

enum class LibrarySection {
    SONGS,
    FAVORITES,
    RECENT,
    RECENTLY_ADDED,
    MOST_PLAYED,
    UNPLAYED,
    ALBUMS,
    ARTISTS,
    FOLDERS,
}

enum class CollectionType {
    ALBUM,
    ARTIST,
    FOLDER,
}

data class CollectionFilter(
    val type: CollectionType,
    val value: String,
    val label: String = value,
)

data class MainUiState(
    val destination: AppDestination = AppDestination.LIBRARY,
    val songs: List<Song> = emptyList(),
    val totalSongCount: Int = 0,
    val mediaStoreSongCount: Int = 0,
    val authorizedFolderSongCount: Int = 0,
    val favoriteMediaIds: Set<String> = emptySet(),
    val recentMediaIds: List<String> = emptyList(),
    val playbackStats: Map<String, PlaybackStats> = emptyMap(),
    val playedSongCount: Int = 0,
    val unplayedSongCount: Int = 0,
    val totalPlayCount: Long = 0L,
    val query: String = "",
    val section: LibrarySection = LibrarySection.SONGS,
    val activeFilter: CollectionFilter? = null,
    val permissionChecked: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val authorizedFolders: List<AuthorizedFolder> = emptyList(),
    val libraryWarnings: List<String> = emptyList(),
    val authorizedFolderError: String? = null,
    val isManagingAuthorizedFolders: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val playback: PlaybackUiState = PlaybackUiState(),
    val lyrics: LyricsUiState = LyricsUiState(),
) {
    val hasAuthorizedFolderAccess: Boolean
        get() = authorizedFolders.any(AuthorizedFolder::isAvailable)

    val hasLibraryAccess: Boolean
        get() = hasAudioPermission || hasAuthorizedFolderAccess
}

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val musicRepository = MusicRepository(application)
    private val authorizedFolderRepository = AuthorizedFolderRepository(application)
    private val libraryStateRepository = LibraryStateRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val lyricsRepository = LyricsRepository(application)
    private val playerConnection = PlayerConnection(application, viewModelScope)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var library: List<Song> = emptyList()
    private var authorizedFolders: List<AuthorizedFolder> = emptyList()
    private var hasLoadedLibrary = false
    private var libraryObserver: AutoCloseable? = null
    private var observerRefreshJob: Job? = null
    private var loadJob: Job? = null
    private var lyricsJob: Job? = null
    private var folderJob: Job? = null
    private var lastRecordedPlayingMediaId: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            libraryStateRepository.favoriteMediaIds.collect { mediaIds ->
                _uiState.update { it.copy(favoriteMediaIds = mediaIds) }
                publishFilteredLibrary()
            }
        }

        viewModelScope.launch {
            libraryStateRepository.recentMediaIds.collect { mediaIds ->
                _uiState.update { it.copy(recentMediaIds = mediaIds) }
                publishFilteredLibrary()
            }
        }

        viewModelScope.launch {
            libraryStateRepository.playbackStats.collect { stats ->
                _uiState.update { it.copy(playbackStats = stats) }
                publishFilteredLibrary()
            }
        }

        viewModelScope.launch {
            authorizedFolderRepository.folders.collect { folders ->
                val previousSignature = authorizedFolders.folderSignature()
                authorizedFolders = folders
                _uiState.update {
                    it.copy(
                        authorizedFolders = folders,
                        isManagingAuthorizedFolders = false,
                    )
                }

                if (previousSignature != folders.folderSignature()) {
                    loadLibrary(
                        showLoading = !hasLoadedLibrary,
                        refreshAuthorizedFolders = true,
                    )
                }
            }
        }

        viewModelScope.launch {
            playerConnection.state.collect { playback ->
                val previousPlayback = _uiState.value.playback
                _uiState.update { it.copy(playback = playback) }

                if (previousPlayback.mediaId != playback.mediaId) {
                    loadLyrics(playback.mediaId)
                }

                val mediaId = playback.mediaId
                if (
                    playback.isPlaying &&
                    mediaId != null &&
                    mediaId != lastRecordedPlayingMediaId
                ) {
                    lastRecordedPlayingMediaId = mediaId
                    libraryStateRepository.recordPlayed(mediaId)
                } else if (mediaId == null) {
                    lastRecordedPlayingMediaId = null
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(destination = destination) }
    }

    fun handleShortcutAction(action: String?) {
        val targetSection =
            when (action) {
                AppShortcuts.ACTION_OPEN_FAVORITES -> LibrarySection.FAVORITES
                AppShortcuts.ACTION_OPEN_RECENT -> LibrarySection.RECENT
                AppShortcuts.ACTION_OPEN_RECENTLY_ADDED -> LibrarySection.RECENTLY_ADDED
                AppShortcuts.ACTION_OPEN_MOST_PLAYED -> LibrarySection.MOST_PLAYED
                else -> null
            } ?: return

        _uiState.update {
            it.copy(
                destination = AppDestination.LIBRARY,
                section = targetSection,
                query = "",
                activeFilter = null,
            )
        }
        publishFilteredLibrary()
    }

    fun onAudioPermissionChanged(granted: Boolean) {
        val permissionChanged = _uiState.value.hasAudioPermission != granted
        _uiState.update {
            it.copy(
                permissionChecked = true,
                hasAudioPermission = granted,
                errorMessage = if (granted) it.errorMessage else null,
            )
        }

        if (granted) {
            ensureLibraryObserver()
        } else {
            stopLibraryObserver()
        }

        if (_uiState.value.hasLibraryAccess) {
            if (permissionChanged || !hasLoadedLibrary) {
                loadLibrary(
                    showLoading = true,
                    refreshAuthorizedFolders = false,
                )
            }
        } else {
            clearLibrary()
        }
    }

    fun refreshLibrary() {
        loadLibrary(
            showLoading = true,
            refreshAuthorizedFolders = true,
        )
    }

    fun addAuthorizedFolder(uri: Uri) {
        folderJob?.cancel()
        folderJob =
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isManagingAuthorizedFolders = true,
                        authorizedFolderError = null,
                    )
                }
                try {
                    authorizedFolderRepository.addFolder(uri)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _uiState.update {
                        it.copy(
                            isManagingAuthorizedFolders = false,
                            authorizedFolderError =
                                throwable.localizedMessage ?: "目录授权失败",
                        )
                    }
                }
            }
    }

    fun removeAuthorizedFolder(uriString: String) {
        folderJob?.cancel()
        folderJob =
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isManagingAuthorizedFolders = true,
                        authorizedFolderError = null,
                    )
                }
                try {
                    authorizedFolderRepository.removeFolder(uriString)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _uiState.update {
                        it.copy(
                            isManagingAuthorizedFolders = false,
                            authorizedFolderError =
                                throwable.localizedMessage ?: "移除目录失败",
                        )
                    }
                }
            }
    }

    fun clearAuthorizedFolderError() {
        _uiState.update { it.copy(authorizedFolderError = null) }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        publishFilteredLibrary()
    }

    fun showSection(section: LibrarySection) {
        _uiState.update {
            it.copy(
                section = section,
                activeFilter = if (section == LibrarySection.SONGS) it.activeFilter else null,
            )
        }
        publishFilteredLibrary()
    }

    fun openAlbum(album: String) {
        openCollection(CollectionType.ALBUM, album, album)
    }

    fun openArtist(artist: String) {
        openCollection(CollectionType.ARTIST, artist, artist)
    }

    fun openFolder(
        folderPath: String,
        folderName: String,
    ) {
        openCollection(CollectionType.FOLDER, folderPath, folderName)
    }

    fun clearCollectionFilter() {
        _uiState.update { it.copy(activeFilter = null) }
        publishFilteredLibrary()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            libraryStateRepository.toggleFavorite(song.id.toString())
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            libraryStateRepository.clearRecent()
        }
    }

    fun play(song: Song) {
        val visibleQueue = _uiState.value.songs.ifEmpty { library }
        playerConnection.play(song, visibleQueue)
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()

    fun skipNext() = playerConnection.skipNext()

    fun skipPrevious() = playerConnection.skipPrevious()

    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

    fun toggleShuffle() = playerConnection.toggleShuffle()

    fun cycleRepeatMode() = playerConnection.cycleRepeatMode()

    fun jumpToQueueItem(index: Int) = playerConnection.jumpToQueueItem(index)

    fun moveQueueItem(
        fromIndex: Int,
        toIndex: Int,
    ) = playerConnection.moveQueueItem(fromIndex, toIndex)

    fun removeQueueItem(index: Int) = playerConnection.removeQueueItem(index)

    fun clearQueue() = playerConnection.clearQueue()

    fun importLyrics(uri: Uri) {
        val mediaId = _uiState.value.playback.mediaId ?: return
        lyricsJob?.cancel()
        lyricsJob =
            viewModelScope.launch {
                _uiState.update { state ->
                    if (state.playback.mediaId == mediaId) {
                        state.copy(
                            lyrics = state.lyrics.copy(
                                mediaId = mediaId,
                                isLoading = true,
                                errorMessage = null,
                            ),
                        )
                    } else {
                        state
                    }
                }

                try {
                    val parsed = lyricsRepository.import(mediaId, uri)
                    val userOffsetMs = lyricsRepository.getUserOffsetMs(mediaId)
                    _uiState.update { state ->
                        if (state.playback.mediaId == mediaId) {
                            state.copy(
                                lyrics =
                                    LyricsUiState(
                                        mediaId = mediaId,
                                        lines = parsed.lines,
                                        fileOffsetMs = parsed.fileOffsetMs,
                                        userOffsetMs = userOffsetMs,
                                    ),
                            )
                        } else {
                            state
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _uiState.update { state ->
                        if (state.playback.mediaId == mediaId) {
                            state.copy(
                                lyrics =
                                    state.lyrics.copy(
                                        isLoading = false,
                                        errorMessage = throwable.localizedMessage ?: "歌词导入失败",
                                    ),
                            )
                        } else {
                            state
                        }
                    }
                }
            }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val current = _uiState.value.lyrics
        val mediaId = current.mediaId ?: return
        val newOffset =
            (current.userOffsetMs + deltaMs)
                .coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)
        lyricsRepository.setUserOffsetMs(mediaId, newOffset)
        _uiState.update { state ->
            if (state.lyrics.mediaId == mediaId) {
                state.copy(lyrics = state.lyrics.copy(userOffsetMs = newOffset))
            } else {
                state
            }
        }
    }

    fun resetLyricsOffset() {
        val mediaId = _uiState.value.lyrics.mediaId ?: return
        lyricsRepository.setUserOffsetMs(mediaId, 0L)
        _uiState.update { state ->
            if (state.lyrics.mediaId == mediaId) {
                state.copy(lyrics = state.lyrics.copy(userOffsetMs = 0L))
            } else {
                state
            }
        }
    }

    fun deleteLyrics() {
        val mediaId = _uiState.value.lyrics.mediaId ?: return
        lyricsJob?.cancel()
        lyricsJob =
            viewModelScope.launch {
                lyricsRepository.delete(mediaId)
                _uiState.update { state ->
                    if (state.playback.mediaId == mediaId) {
                        state.copy(lyrics = LyricsUiState(mediaId = mediaId))
                    } else {
                        state
                    }
                }
            }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        observerRefreshJob?.cancel()
        loadJob?.cancel()
        lyricsJob?.cancel()
        folderJob?.cancel()
        stopLibraryObserver()
        playerConnection.close()
        super.onCleared()
    }

    private fun openCollection(
        type: CollectionType,
        value: String,
        label: String,
    ) {
        _uiState.update {
            it.copy(
                query = "",
                section = LibrarySection.SONGS,
                activeFilter = CollectionFilter(type = type, value = value, label = label),
            )
        }
        publishFilteredLibrary()
    }

    private fun loadLibrary(
        showLoading: Boolean,
        refreshAuthorizedFolders: Boolean,
    ) {
        val current = _uiState.value
        if (!current.hasAudioPermission && authorizedFolders.none(AuthorizedFolder::isAvailable)) {
            clearLibrary()
            return
        }

        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                if (showLoading) {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }

                try {
                    val result =
                        musicRepository.loadSongs(
                            includeMediaStore = _uiState.value.hasAudioPermission,
                            authorizedFolders = authorizedFolders,
                            refreshAuthorizedFolders = refreshAuthorizedFolders,
                        )
                    library = result.songs
                    hasLoadedLibrary = true
                    publishFilteredLibrary()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            mediaStoreSongCount = result.mediaStoreSongCount,
                            authorizedFolderSongCount = result.authorizedFolderSongCount,
                            libraryWarnings = result.warnings,
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "无法读取本地音乐库",
                        )
                    }
                }
            }
    }

    private fun clearLibrary() {
        loadJob?.cancel()
        library = emptyList()
        hasLoadedLibrary = false
        _uiState.update {
            it.copy(
                songs = emptyList(),
                totalSongCount = 0,
                mediaStoreSongCount = 0,
                authorizedFolderSongCount = 0,
                playedSongCount = 0,
                unplayedSongCount = 0,
                totalPlayCount = 0L,
                libraryWarnings = emptyList(),
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    private fun loadLyrics(mediaId: String?) {
        lyricsJob?.cancel()
        if (mediaId == null) {
            _uiState.update { it.copy(lyrics = LyricsUiState()) }
            return
        }

        val userOffsetMs = lyricsRepository.getUserOffsetMs(mediaId)
        _uiState.update {
            it.copy(
                lyrics =
                    LyricsUiState(
                        mediaId = mediaId,
                        userOffsetMs = userOffsetMs,
                        isLoading = true,
                    ),
            )
        }
        lyricsJob =
            viewModelScope.launch {
                try {
                    val parsed = lyricsRepository.load(mediaId)
                    _uiState.update { state ->
                        if (state.playback.mediaId == mediaId) {
                            state.copy(
                                lyrics =
                                    LyricsUiState(
                                        mediaId = mediaId,
                                        lines = parsed?.lines.orEmpty(),
                                        fileOffsetMs = parsed?.fileOffsetMs ?: 0L,
                                        userOffsetMs = userOffsetMs,
                                    ),
                            )
                        } else {
                            state
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _uiState.update { state ->
                        if (state.playback.mediaId == mediaId) {
                            state.copy(
                                lyrics =
                                    LyricsUiState(
                                        mediaId = mediaId,
                                        userOffsetMs = userOffsetMs,
                                        errorMessage = throwable.localizedMessage ?: "歌词读取失败",
                                    ),
                            )
                        } else {
                            state
                        }
                    }
                }
            }
    }

    private fun ensureLibraryObserver() {
        if (libraryObserver != null) return
        libraryObserver =
            musicRepository.observeChanges {
                if (!hasLoadedLibrary || !_uiState.value.hasAudioPermission) return@observeChanges
                observerRefreshJob?.cancel()
                observerRefreshJob =
                    viewModelScope.launch {
                        delay(MEDIASTORE_REFRESH_DEBOUNCE_MS)
                        loadLibrary(
                            showLoading = false,
                            refreshAuthorizedFolders = false,
                        )
                    }
            }
    }

    private fun stopLibraryObserver() {
        runCatching { libraryObserver?.close() }
        libraryObserver = null
        observerRefreshJob?.cancel()
        observerRefreshJob = null
    }

    private fun publishFilteredLibrary() {
        val current = _uiState.value
        val sectionSongs =
            when (current.section) {
                LibrarySection.FAVORITES ->
                    library.filter { it.id.toString() in current.favoriteMediaIds }

                LibrarySection.RECENT ->
                    resolveRecentSongs(
                        songs = library,
                        recentMediaIds = current.recentMediaIds,
                    )

                LibrarySection.RECENTLY_ADDED ->
                    resolveRecentlyAddedSongs(library)

                LibrarySection.MOST_PLAYED ->
                    resolveMostPlayedSongs(
                        songs = library,
                        playbackStats = current.playbackStats,
                    )

                LibrarySection.UNPLAYED ->
                    resolveUnplayedSongs(
                        songs = library,
                        playbackStats = current.playbackStats,
                    )

                else ->
                    when (val filter = current.activeFilter) {
                        null -> library
                        else ->
                            library.filter { song ->
                                when (filter.type) {
                                    CollectionType.ALBUM -> song.album == filter.value
                                    CollectionType.ARTIST -> song.artist == filter.value
                                    CollectionType.FOLDER -> song.folderPath == filter.value
                                }
                            }
                    }
            }
        val visibleSongs = filterSongs(sectionSongs, current.query)
        val availableStats =
            library.mapNotNull { song ->
                current.playbackStats[song.id.toString()]
            }
        val playedSongCount = availableStats.count { it.playCount > 0 }
        val totalPlayCount = availableStats.sumOf { it.playCount.toLong() }

        _uiState.update {
            it.copy(
                songs = visibleSongs,
                totalSongCount = library.size,
                playedSongCount = playedSongCount,
                unplayedSongCount = (library.size - playedSongCount).coerceAtLeast(0),
                totalPlayCount = totalPlayCount,
            )
        }
    }

    private fun List<AuthorizedFolder>.folderSignature(): List<Pair<String, Boolean>> =
        map { it.uriString to it.isAvailable }

    private companion object {
        const val MEDIASTORE_REFRESH_DEBOUNCE_MS = 650L
        const val MAX_LYRICS_OFFSET_MS = 30_000L
    }
}
