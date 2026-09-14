package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.geek_studio.chenlongcould.musicplayer.playback.PlaybackUiState
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkPlaceholder
import top.geek_studio.chenlongcould.musicplayer.ui.components.PlaybackControls
import top.geek_studio.chenlongcould.musicplayer.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "正在播放",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = playback.album.ifBlank { "本地音乐" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        )

        if (playback.mediaId == null) {
            EmptyPlayer()
            return
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArtworkPlaceholder(
                seed = playback.mediaId.toLongOrNull() ?: 0L,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .sizeIn(maxWidth = 440.dp)
                        .aspectRatio(1f),
                cornerRadius = 36.dp,
                glyphSize = 88,
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = playback.title.ifBlank { "未知曲目" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = playback.artist.ifBlank { "未知艺术家" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(22.dp))

            SeekBar(
                playback = playback,
                onSeek = onSeek,
            )

            Spacer(Modifier.height(10.dp))

            PlaybackControls(
                playback = playback,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
            )

            Spacer(Modifier.height(30.dp))
            HorizontalDivider()
            Spacer(Modifier.height(22.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "播放状态",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    StatusLine(
                        label = "媒体会话",
                        value = if (playback.isConnected) "已连接" else "正在连接",
                    )
                    StatusLine(
                        label = "缓冲",
                        value = formatDuration(playback.bufferedPositionMs),
                    )
                    StatusLine(
                        label = "播放模式",
                        value =
                            when {
                                playback.shuffleEnabled -> "随机播放"
                                else -> "队列顺序"
                            },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SeekBar(
    playback: PlaybackUiState,
    onSeek: (Long) -> Unit,
) {
    val duration = playback.durationMs.coerceAtLeast(1L)
    var isDragging by remember(playback.mediaId) { mutableStateOf(false) }
    var draggedPosition by remember(playback.mediaId) {
        mutableFloatStateOf(playback.positionMs.toFloat())
    }
    val displayedPosition =
        if (isDragging) {
            draggedPosition
        } else {
            playback.positionMs.toFloat().coerceIn(0f, duration.toFloat())
        }

    Slider(
        value = displayedPosition.coerceIn(0f, duration.toFloat()),
        onValueChange = {
            isDragging = true
            draggedPosition = it
        },
        onValueChangeFinished = {
            onSeek(draggedPosition.toLong())
            isDragging = false
        },
        valueRange = 0f..duration.toFloat(),
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatDuration(displayedPosition.toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDuration(playback.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyPlayer() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "♫",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "还没有正在播放的歌曲",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "从音乐库选择一首歌曲开始播放。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
