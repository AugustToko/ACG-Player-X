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
import androidx.compose.foundation.layout.weight
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

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            localPermissionGranted = granted
            viewModel.onAudioPermissionChanged(granted)
        }

    LaunchedEffect(audioPermission) {
        viewModel.onAudioPermissionChanged(localPermissionGranted)
    }

    var destination by rememberSaveable {
        mutableStateOf(Destination.LIBRARY)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            ExpandedLayout(
                destination = destination,
                onDestinationChange = { destination = it },
                state = state,
                viewModel = viewModel,
                onRequestPermission = { permissionLauncher.launch(audioPermission) },
            )
        } else {
            CompactLayout(
                destination = destination,
                onDestinationChange = { destination = it },
                state = state,
                viewModel = viewModel,
                onRequestPermission = { permissionLauncher.launch(audioPermission) },
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
    onRequestPermission: () -> Unit,
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
            onRequestPermission = onRequestPermission,
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
    onRequestPermission: () -> Unit,
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
                    onRequestPermission = onRequestPermission,
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
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        Destination.LIBRARY -> {
            LibraryScreen(
                state = state,
                onRequestPermission = onRequestPermission,
                onRefresh = viewModel::refreshLibrary,
                onQueryChange = viewModel::updateQuery,
                onSectionChange = viewModel::showSection,
                onPlaySong = viewModel::play,
                onOpenAlbum = viewModel::openAlbum,
                onOpenArtist = viewModel::openArtist,
                modifier = modifier,
            )
        }

        Destination.NOW_PLAYING -> {
            NowPlayingScreen(
                playback = state.playback,
                onSeek = viewModel::seekTo,
                onPrevious = viewModel::skipPrevious,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::skipNext,
                onShuffle = viewModel::toggleShuffle,
                onRepeat = viewModel::cycleRepeatMode,
                modifier = modifier,
            )
        }

        Destination.SETTINGS -> {
            SettingsScreen(
                themeMode = state.themeMode,
                onThemeModeChange = viewModel::setThemeMode,
                modifier = modifier,
            )
        }
    }
}
