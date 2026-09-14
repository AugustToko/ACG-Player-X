package top.geek_studio.chenlongcould.musicplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import top.geek_studio.chenlongcould.musicplayer.ui.components.MiniPlayer

private enum class Destination(
    val label: String,
    val glyph: String,
) {
    LIBRARY("音乐库", "♫"),
    NOW_PLAYING("播放", "▶"),
    SETTINGS("设置", "⚙"),
}

@Composable
fun AcgPlayerApp(
    state: MainUiState,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
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

    LaunchedEffect(audioPermission) {
        viewModel.onAudioPermissionChanged(localPermissionGranted)
    }

    var destination by rememberSaveable {
        mutableStateOf(Destination.LIBRARY)
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            ExpandedLayout(
                destination = destination,
                onDestinationChange = { destination = it },
                state = state,
                viewModel = viewModel,
                notificationPermissionRequired = notificationPermissionRequired,
                onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission) },
                onRequestNotificationPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onImportLyrics = onImportLyrics,
                onAddAuthorizedFolder = onAddAuthorizedFolder,
            )
        } else {
            CompactLayout(
                destination = destination,
                onDestinationChange = { destination = it },
                state = state,
                viewModel = viewModel,
                notificationPermissionRequired = notificationPermissionRequired,
                onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission) },
                onRequestNotificationPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onImportLyrics = onImportLyrics,
                onAddAuthorizedFolder = onAddAuthorizedFolder,
            )
        }
    }
}

@Composable
private fun CompactLayout(
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
    state: MainUiState,
    viewModel: MainViewModel,
    notificationPermissionRequired: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onImportLyrics: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            Column {
                if (state.playback.mediaId != null && destination != Destination.NOW_PLAYING) {
                    MiniPlayer(
                        playback = state.playback,
                        onOpenPlayer = { onDestinationChange(Destination.NOW_PLAYING) },
                        onPlayPause = viewModel::togglePlayPause,
                    )
                }

                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { onDestinationChange(item) },
                            icon = { Text(item.glyph) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        DestinationContent(
            destination = destination,
            state = state,
            viewModel = viewModel,
            notificationPermissionRequired = notificationPermissionRequired,
            onRequestAudioPermission = onRequestAudioPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onImportLyrics = onImportLyrics,
            onAddAuthorizedFolder = onAddAuthorizedFolder,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun ExpandedLayout(
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
    state: MainUiState,
    viewModel: MainViewModel,
    notificationPermissionRequired: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onImportLyrics: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
        ) {
            Destination.entries.forEach { item ->
                NavigationRailItem(
                    selected = destination == item,
                    onClick = { onDestinationChange(item) },
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
                DestinationContent(
                    destination = destination,
                    state = state,
                    viewModel = viewModel,
                    notificationPermissionRequired = notificationPermissionRequired,
                    onRequestAudioPermission = onRequestAudioPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onImportLyrics = onImportLyrics,
                    onAddAuthorizedFolder = onAddAuthorizedFolder,
                )
            }

            if (state.playback.mediaId != null && destination != Destination.NOW_PLAYING) {
                MiniPlayer(
                    playback = state.playback,
                    onOpenPlayer = { onDestinationChange(Destination.NOW_PLAYING) },
                    onPlayPause = viewModel::togglePlayPause,
                )
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: Destination,
    state: MainUiState,
    viewModel: MainViewModel,
    notificationPermissionRequired: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onImportLyrics: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        Destination.LIBRARY -> {
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

        Destination.NOW_PLAYING -> {
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

        Destination.SETTINGS -> {
            SettingsScreen(
                themeMode = state.themeMode,
                notificationPermissionRequired = notificationPermissionRequired,
                authorizedFolders = state.authorizedFolders,
                libraryWarnings = state.libraryWarnings,
                authorizedFolderError = state.authorizedFolderError,
                isManagingAuthorizedFolders = state.isManagingAuthorizedFolders,
                mediaStoreSongCount = state.mediaStoreSongCount,
                authorizedFolderSongCount = state.authorizedFolderSongCount,
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
