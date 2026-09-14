package top.geek_studio.chenlongcould.musicplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import top.geek_studio.chenlongcould.musicplayer.data.UserPlaylist
import top.geek_studio.chenlongcould.musicplayer.data.m3uExportFileName
import top.geek_studio.chenlongcould.musicplayer.ui.components.MiniPlayer

private val AppDestination.label: String
    get() =
        when (this) {
            AppDestination.LIBRARY -> "音乐库"
            AppDestination.NOW_PLAYING -> "播放"
            AppDestination.SETTINGS -> "设置"
        }

private val AppDestination.glyph: String
    get() =
        when (this) {
            AppDestination.LIBRARY -> "♫"
            AppDestination.NOW_PLAYING -> "▶"
            AppDestination.SETTINGS -> "⚙"
        }

@Composable
fun AcgPlayerApp(
    state: MainUiState,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val playlistViewModel: PlaylistViewModel = viewModel()
    val playlistState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val latestLibrarySongs by rememberUpdatedState(state.songs)
    val audioPermission =
        remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

    var localPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var playlistsVisible by rememberSaveable { mutableStateOf(false) }
    var pendingExportPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }

    val audioPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            localPermissionGranted = granted
            viewModel.onAudioPermissionChanged(granted)
        }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            notificationPermissionGranted = granted
        }
    val lyricsFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let(viewModel::importLyrics)
        }
    val musicFolderLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let(viewModel::addAuthorizedFolder)
        }
    val playlistImportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let { selected ->
                playlistViewModel.importPlaylist(selected, latestLibrarySongs)
            }
        }
    val playlistExportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/vnd.apple.mpegurl"),
        ) { uri ->
            val playlistId = pendingExportPlaylistId
            pendingExportPlaylistId = null
            if (uri != null && playlistId != null) {
                playlistViewModel.exportPlaylist(
                    playlistId = playlistId,
                    uri = uri,
                    librarySongs = latestLibrarySongs,
                )
            }
        }

    LaunchedEffect(audioPermission) {
        viewModel.onAudioPermissionChanged(localPermissionGranted)
    }
    LaunchedEffect(state.section) {
        if (playlistsVisible && state.section != LibrarySection.SONGS) {
            playlistsVisible = false
        }
    }

    BackHandler(
        enabled = playlistsVisible && playlistState.activePlaylist == null,
    ) {
        playlistsVisible = false
    }

    val notificationPermissionRequired =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionGranted
    val onImportLyrics = {
        lyricsFileLauncher.launch(
            arrayOf("text/*", "application/octet-stream", "application/x-lrc"),
        )
    }
    val onAddAuthorizedFolder = {
        musicFolderLauncher.launch(null)
    }
    val onImportPlaylist = {
        playlistImportLauncher.launch(
            arrayOf(
                "application/vnd.apple.mpegurl",
                "audio/x-mpegurl",
                "audio/mpegurl",
                "application/x-mpegurl",
                "text/plain",
                "application/octet-stream",
            ),
        )
    }
    val onExportPlaylist: (UserPlaylist) -> Unit = { playlist ->
        pendingExportPlaylistId = playlist.id
        playlistExportLauncher.launch(m3uExportFileName(playlist.name))
    }
    val onOpenPlaylists = {
        viewModel.showSection(LibrarySection.SONGS)
        viewModel.updateQuery("")
        viewModel.clearCollectionFilter()
        playlistsVisible = true
    }
    val onClosePlaylists = {
        playlistsVisible = false
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            ExpandedLayout(
                destination = state.destination,
                playlistsVisible = playlistsVisible,
                onDestinationChange = viewModel::navigateTo,
                onOpenPlaylists = onOpenPlaylists,
                onClosePlaylists = onClosePlaylists,
                state = state,
                viewModel = viewModel,
                playlistState = playlistState,
                playlistViewModel = playlistViewModel,
                notificationPermissionRequired = notificationPermissionRequired,
                onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission) },
                onRequestNotificationPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onImportLyrics = onImportLyrics,
                onAddAuthorizedFolder = onAddAuthorizedFolder,
                onImportPlaylist = onImportPlaylist,
                onExportPlaylist = onExportPlaylist,
            )
        } else {
            CompactLayout(
                destination = state.destination,
                playlistsVisible = playlistsVisible,
                onDestinationChange = viewModel::navigateTo,
                onOpenPlaylists = onOpenPlaylists,
                onClosePlaylists = onClosePlaylists,
                state = state,
                viewModel = viewModel,
                playlistState = playlistState,
                playlistViewModel = playlistViewModel,
                notificationPermissionRequired = notificationPermissionRequired,
                onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission) },
                onRequestNotificationPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onImportLyrics = onImportLyrics,
                onAddAuthorizedFolder = onAddAuthorizedFolder,
                onImportPlaylist = onImportPlaylist,
                onExportPlaylist = onExportPlaylist,
            )
        }
    }
}

@Composable
private fun CompactLayout(
    destination: AppDestination,
    playlistsVisible: Boolean,
    onDestinationChange: (AppDestination) -> Unit,
    onOpenPlaylists: () -> Unit,
    onClosePlaylists: () -> Unit,
    state: MainUiState,
    viewModel: MainViewModel,
    playlistState: PlaylistUiState,
    playlistViewModel: PlaylistViewModel,
    notificationPermissionRequired: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onImportLyrics: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: (UserPlaylist) -> Unit,
) {
    Scaffold(
        bottomBar = {
            Column {
                if (
                    state.playback.mediaId != null &&
                    (playlistsVisible || destination != AppDestination.NOW_PLAYING)
                ) {
                    MiniPlayer(
                        playback = state.playback,
                        onOpenPlayer = {
                            onClosePlaylists()
                            onDestinationChange(AppDestination.NOW_PLAYING)
                        },
                        onPlayPause = viewModel::togglePlayPause,
                    )
                }

                NavigationBar {
                    NavigationBarItem(
                        selected = playlistsVisible,
                        onClick = onOpenPlaylists,
                        icon = { Text("☷") },
                        label = { Text("歌单") },
                    )
                    AppDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = !playlistsVisible && destination == item,
                            onClick = {
                                onClosePlaylists()
                                onDestinationChange(item)
                            },
                            icon = { Text(item.glyph) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppContent(
            playlistsVisible = playlistsVisible,
            destination = destination,
            state = state,
            viewModel = viewModel,
            playlistState = playlistState,
            playlistViewModel = playlistViewModel,
            notificationPermissionRequired = notificationPermissionRequired,
            onRequestAudioPermission = onRequestAudioPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onImportLyrics = onImportLyrics,
            onAddAuthorizedFolder = onAddAuthorizedFolder,
            onImportPlaylist = onImportPlaylist,
            onExportPlaylist = onExportPlaylist,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun ExpandedLayout(
    destination: AppDestination,
    playlistsVisible: Boolean,
    onDestinationChange: (AppDestination) -> Unit,
    onOpenPlaylists: () -> Unit,
    onClosePlaylists: () -> Unit,
    state: MainUiState,
    viewModel: MainViewModel,
    playlistState: PlaylistUiState,
    playlistViewModel: PlaylistViewModel,
    notificationPermissionRequired: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onImportLyrics: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: (UserPlaylist) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
        ) {
            NavigationRailItem(
                selected = playlistsVisible,
                onClick = onOpenPlaylists,
                icon = { Text("☷") },
                label = { Text("歌单") },
            )
            AppDestination.entries.forEach { item ->
                NavigationRailItem(
                    selected = !playlistsVisible && destination == item,
                    onClick = {
                        onClosePlaylists()
                        onDestinationChange(item)
                    },
                    icon = { Text(item.glyph) },
                    label = { Text(item.label) },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )

        Column(Modifier.weight(1f)) {
            Box(Modifier.weight(1f)) {
                AppContent(
                    playlistsVisible = playlistsVisible,
                    destination = destination,
                    state = state,
                    viewModel = viewModel,
                    playlistState = playlistState,
                    playlistViewModel = playlistViewModel,
                    notificationPermissionRequired = notificationPermissionRequired,
                    onRequestAudioPermission = onRequestAudioPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onImportLyrics = onImportLyrics,
                    onAddAuthorizedFolder = onAddAuthorizedFolder,
                    onImportPlaylist = onImportPlaylist,
                    onExportPlaylist = onExportPlaylist,
                )
            }

            if (
                state.playback.mediaId != null &&
                (playlistsVisible || destination != AppDestination.NOW_PLAYING)
            ) {
                MiniPlayer(
                    playback = state.playback,
                    onOpenPlayer = {
                        onClosePlaylists()
                        onDestinationChange(AppDestination.NOW_PLAYING)
                    },
                    onPlayPause = viewModel::togglePlayPause,
                )
            }
        }
    }
}

@Composable
private fun AppContent(
    playlistsVisible: Boolean,
    destination: AppDestination,
    state: MainUiState,
    viewModel: MainViewModel,
    playlistState: PlaylistUiState,
    playlistViewModel: PlaylistViewModel,
    notificationPermissionRequired: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onImportLyrics: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: (UserPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlistsVisible) {
        Column(modifier.fillMaxSize()) {
            PlaylistTransferBar(
                state = playlistState,
                onImport = onImportPlaylist,
                onExport = onExportPlaylist,
                onDismissInfo = playlistViewModel::clearInfoMessage,
            )
            PlaylistsScreen(
                playlistState = playlistState,
                librarySongs = state.songs,
                currentMediaId = state.playback.mediaId,
                onOpenPlaylist = playlistViewModel::openPlaylist,
                onClosePlaylist = playlistViewModel::closePlaylist,
                onCreatePlaylist = playlistViewModel::createPlaylist,
                onRenamePlaylist = playlistViewModel::renamePlaylist,
                onDeletePlaylist = playlistViewModel::deletePlaylist,
                onAddSongs = playlistViewModel::addSongs,
                onRemoveSong = playlistViewModel::removeSong,
                onSwapSongs = playlistViewModel::swapSongs,
                onClearSongs = playlistViewModel::clearSongs,
                onRemoveUnavailableSongs = playlistViewModel::removeUnavailableSongs,
                onPlaySong = playlistViewModel::playSong,
                onPlayAll = playlistViewModel::playAll,
                onClearError = playlistViewModel::clearError,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        DestinationContent(
            destination = destination,
            state = state,
            viewModel = viewModel,
            playlistCount = playlistState.playlists.size,
            notificationPermissionRequired = notificationPermissionRequired,
            onRequestAudioPermission = onRequestAudioPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onImportLyrics = onImportLyrics,
            onAddAuthorizedFolder = onAddAuthorizedFolder,
            modifier = modifier,
        )
    }
}

@Composable
private fun DestinationContent(
    destination: AppDestination,
    state: MainUiState,
    viewModel: MainViewModel,
    playlistCount: Int,
    notificationPermissionRequired: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onImportLyrics: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        AppDestination.LIBRARY -> {
            LibraryScreen(
                state = state,
                notificationPermissionRequired = notificationPermissionRequired,
                onRequestPermission = onRequestAudioPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onAddAuthorizedFolder = onAddAuthorizedFolder,
                onRefresh = viewModel::refreshLibrary,
                onQueryChange = viewModel::updateQuery,
                onSectionChange = viewModel::showSection,
                onClearCollectionFilter = viewModel::clearCollectionFilter,
                onPlaySong = viewModel::play,
                onToggleFavorite = viewModel::toggleFavorite,
                onClearRecentHistory = viewModel::clearRecentHistory,
                onOpenAlbum = viewModel::openAlbum,
                onOpenArtist = viewModel::openArtist,
                onOpenFolder = viewModel::openFolder,
                modifier = modifier,
            )
        }

        AppDestination.NOW_PLAYING -> {
            NowPlayingScreen(
                playback = state.playback,
                lyrics = state.lyrics,
                onSeek = viewModel::seekTo,
                onPrevious = viewModel::skipPrevious,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::skipNext,
                onShuffle = viewModel::toggleShuffle,
                onRepeat = viewModel::cycleRepeatMode,
                onImportLyrics = onImportLyrics,
                onAdjustLyricsOffset = viewModel::adjustLyricsOffset,
                onResetLyricsOffset = viewModel::resetLyricsOffset,
                onDeleteLyrics = viewModel::deleteLyrics,
                onJumpToQueueItem = viewModel::jumpToQueueItem,
                onMoveQueueItem = viewModel::moveQueueItem,
                onRemoveQueueItem = viewModel::removeQueueItem,
                onClearQueue = viewModel::clearQueue,
                modifier = modifier,
            )
        }

        AppDestination.SETTINGS -> {
            SettingsScreen(
                themeMode = state.themeMode,
                notificationPermissionRequired = notificationPermissionRequired,
                authorizedFolders = state.authorizedFolders,
                libraryWarnings = state.libraryWarnings,
                authorizedFolderError = state.authorizedFolderError,
                isManagingAuthorizedFolders = state.isManagingAuthorizedFolders,
                mediaStoreSongCount = state.mediaStoreSongCount,
                authorizedFolderSongCount = state.authorizedFolderSongCount,
                favoriteSongCount = state.favoriteMediaIds.size,
                recentSongCount = state.recentMediaIds.size,
                playlistCount = playlistCount,
                onThemeModeChange = viewModel::setThemeMode,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onAddAuthorizedFolder = onAddAuthorizedFolder,
                onRemoveAuthorizedFolder = viewModel::removeAuthorizedFolder,
                onRefreshLibrary = viewModel::refreshLibrary,
                modifier = modifier,
            )
        }
    }
}
