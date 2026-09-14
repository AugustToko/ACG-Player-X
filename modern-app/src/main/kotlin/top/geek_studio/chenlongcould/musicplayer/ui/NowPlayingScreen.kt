package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.geek_studio.chenlongcould.musicplayer.lyrics.LyricsUiState
import top.geek_studio.chenlongcould.musicplayer.lyrics.activeLyricIndex
import top.geek_studio.chenlongcould.musicplayer.lyrics.effectiveTimestampMs
import top.geek_studio.chenlongcould.musicplayer.playback.PlaybackUiState
import top.geek_studio.chenlongcould.musicplayer.playback.QueueItemUi
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkImage
import top.geek_studio.chenlongcould.musicplayer.ui.components.PlaybackControls
import top.geek_studio.chenlongcould.musicplayer.ui.components.formatDuration

private enum class PlayerPane(
    val label: String,
) {
    COVER("封面"),
    LYRICS("歌词"),
    QUEUE("队列"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    lyrics: LyricsUiState,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onImportLyrics: () -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit,
    onDeleteLyrics: () -> Unit,
    onJumpToQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPane by rememberSaveable { mutableStateOf(PlayerPane.COVER) }

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

        PlayerPaneSelector(
            selected = selectedPane,
            onSelected = { selectedPane = it },
        )

        when (selectedPane) {
            PlayerPane.COVER -> {
                CoverPane(
                    playback = playback,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onShuffle = onShuffle,
                    onRepeat = onRepeat,
                )
            }

            PlayerPane.LYRICS -> {
                LyricsPane(
                    playback = playback,
                    lyrics = lyrics,
                    onSeek = onSeek,
                    onImportLyrics = onImportLyrics,
                    onAdjustOffset = onAdjustLyricsOffset,
                    onResetOffset = onResetLyricsOffset,
                    onDeleteLyrics = onDeleteLyrics,
                )
            }

            PlayerPane.QUEUE -> {
                QueuePane(
                    playback = playback,
                    onJump = onJumpToQueueItem,
                    onMove = onMoveQueueItem,
                    onRemove = onRemoveQueueItem,
                    onClear = onClearQueue,
                )
            }
        }
    }
}

@Composable
private fun PlayerPaneSelector(
    selected: PlayerPane,
    onSelected: (PlayerPane) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlayerPane.entries.forEach { pane ->
            FilterChip(
                selected = selected == pane,
                onClick = { onSelected(pane) },
                label = {
                    Text(
                        text =
                            if (pane == PlayerPane.QUEUE) {
                                "${pane.label}"
                            } else {
                                pane.label
                            },
                    )
                },
            )
        }
    }
}

@Composable
private fun CoverPane(
    playback: PlaybackUiState,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ArtworkImage(
            artworkUri = playback.artworkUri,
            fallbackUri = playback.mediaUri,
            seed = playback.mediaId?.toLongOrNull() ?: 0L,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 440.dp)
                    .aspectRatio(1f),
            cornerRadius = 36.dp,
            glyphSize = 88,
            requestSize = 512.dp,
            contentDescription = playback.album,
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
                    label = "播放队列",
                    value =
                        if (playback.queueSize > 0 && playback.currentIndex >= 0) {
                            "${playback.currentIndex + 1} / ${playback.queueSize}"
                        } else {
                            "空"
                        },
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

@Composable
private fun LyricsPane(
    playback: PlaybackUiState,
    lyrics: LyricsUiState,
    onSeek: (Long) -> Unit,
    onImportLyrics: () -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onDeleteLyrics: () -> Unit,
) {
    val activeIndex =
        remember(lyrics.lines, playback.positionMs, lyrics.totalOffsetMs) {
            activeLyricIndex(
                lines = lyrics.lines,
                positionMs = playback.positionMs,
                totalOffsetMs = lyrics.totalOffsetMs,
            )
        }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex, lyrics.mediaId) {
        if (activeIndex in lyrics.lines.indices) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Column(Modifier.fillMaxSize()) {
        LyricsToolbar(
            lyrics = lyrics,
            onImportLyrics = onImportLyrics,
            onAdjustOffset = onAdjustOffset,
            onResetOffset = onResetOffset,
            onDeleteLyrics = onDeleteLyrics,
        )

        lyrics.errorMessage?.let { message ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        when {
            lyrics.isLoading -> {
                CenteredPlayerState(
                    title = "正在读取歌词…",
                    showProgress = true,
                )
            }

            !lyrics.hasLyrics -> {
                CenteredPlayerState(
                    title = "当前歌曲没有本地歌词",
                    subtitle = "选择同名或对应的 .lrc 文件后，歌词会复制到应用内部并随歌曲自动加载。",
                    actionLabel = "导入 LRC",
                    onAction = onImportLyrics,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = lyrics.lines,
                        key = { index, line -> "${line.timestampMs}:$index" },
                    ) { index, line ->
                        val isActive = index == activeIndex
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSeek(line.effectiveTimestampMs(lyrics.totalOffsetMs))
                                    },
                            shape = RoundedCornerShape(22.dp),
                            color =
                                if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            tonalElevation = if (isActive) 2.dp else 0.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = line.text.ifBlank { "♪" },
                                    style =
                                        if (isActive) {
                                            MaterialTheme.typography.titleLarge
                                        } else {
                                            MaterialTheme.typography.bodyLarge
                                        },
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color =
                                        if (isActive) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    textAlign = TextAlign.Center,
                                )
                                if (isActive) {
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        text = formatDuration(
                                            line.effectiveTimestampMs(lyrics.totalOffsetMs),
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(120.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsToolbar(
    lyrics: LyricsUiState,
    onImportLyrics: () -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onDeleteLyrics: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Text(
            text = "校准 ${formatOffset(lyrics.totalOffsetMs)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onAdjustOffset(-500L) }) {
                Text("提前 0.5 秒")
            }
            TextButton(onClick = { onAdjustOffset(500L) }) {
                Text("延后 0.5 秒")
            }
            TextButton(onClick = onResetOffset) {
                Text("重置")
            }
            TextButton(onClick = onImportLyrics) {
                Text(if (lyrics.hasLyrics) "替换歌词" else "导入歌词")
            }
            if (lyrics.hasLyrics) {
                TextButton(onClick = onDeleteLyrics) {
                    Text("移除")
                }
            }
        }
    }
}

@Composable
private fun QueuePane(
    playback: PlaybackUiState,
    onJump: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空播放队列？") },
            text = { Text("当前播放会停止，已保存的恢复队列也会被清除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (playback.queue.isEmpty()) {
        CenteredPlayerState(
            title = "播放队列为空",
            subtitle = "从音乐库选择歌曲后会在这里显示当前队列。",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${playback.queue.size} 首歌曲",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "点击播放，使用箭头调整顺序",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { confirmClear = true }) {
                    Text("清空")
                }
            }
        }

        itemsIndexed(
            items = playback.queue,
            key = { index, item -> "$index:${item.mediaId}" },
        ) { index, item ->
            QueueRow(
                item = item,
                index = index,
                queueSize = playback.queue.size,
                isCurrent = index == playback.currentIndex,
                onJump = { onJump(index) },
                onMoveUp = { onMove(index, index - 1) },
                onMoveDown = { onMove(index, index + 1) },
                onRemove = { onRemove(index) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun QueueRow(
    item: QueueItemUi,
    index: Int,
    queueSize: Int,
    isCurrent: Boolean,
    onJump: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onJump),
        color =
            if (isCurrent) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(
                artworkUri = item.artworkUri,
                fallbackUri = item.mediaUri,
                seed = item.mediaId.toLongOrNull() ?: item.mediaId.hashCode().toLong(),
                modifier = Modifier.size(50.dp),
                cornerRadius = 14.dp,
                glyphSize = 22,
                requestSize = 96.dp,
                contentDescription = item.title,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "未知曲目" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color =
                        if (isCurrent) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = item.artist.ifBlank { "未知艺术家" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isCurrent) {
                    Text(
                        text = "正在播放",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    ) {
                        Text("↑")
                    }
                    TextButton(
                        onClick = onMoveDown,
                        enabled = index < queueSize - 1,
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    ) {
                        Text("↓")
                    }
                    TextButton(
                        onClick = onRemove,
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    ) {
                        Text("移除")
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredPlayerState(
    title: String,
    subtitle: String? = null,
    showProgress: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showProgress) {
                CircularProgressIndicator()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
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

private fun formatOffset(offsetMs: Long): String {
    val sign = if (offsetMs >= 0L) "+" else "-"
    val absoluteSeconds = kotlin.math.abs(offsetMs) / 1_000.0
    return "$sign${"%.1f".format(absoluteSeconds)} 秒"
}
