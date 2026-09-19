package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import top.geek_studio.chenlongcould.musicplayer.data.ListeningGroupInsight
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsEntry
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsExportFormat
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsSnapshot
import top.geek_studio.chenlongcould.musicplayer.data.buildPlaybackStatisticsSnapshot
import top.geek_studio.chenlongcould.musicplayer.data.playbackStatisticsFileName
import top.geek_studio.chenlongcould.musicplayer.data.topAlbumInsights
import top.geek_studio.chenlongcould.musicplayer.data.topArtistInsights

@Composable
fun ListeningStatisticsCard(
    modifier: Modifier = Modifier,
) {
    val mainViewModel: MainViewModel = viewModel()
    val statisticsViewModel: StatisticsViewModel = viewModel()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val statisticsState by statisticsViewModel.uiState.collectAsStateWithLifecycle()
    val snapshot =
        remember(mainState.songs, mainState.playbackStats) {
            buildPlaybackStatisticsSnapshot(
                songs = mainState.songs,
                playbackStats = mainState.playbackStats,
            )
        }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    val jsonLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(PlaybackStatisticsExportFormat.JSON.mimeType),
        ) { uri ->
            uri?.let {
                statisticsViewModel.export(
                    uri = it,
                    format = PlaybackStatisticsExportFormat.JSON,
                    snapshot =
                        buildPlaybackStatisticsSnapshot(
                            songs = mainState.songs,
                            playbackStats = mainState.playbackStats,
                        ),
                )
            }
        }
    val csvLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(PlaybackStatisticsExportFormat.CSV.mimeType),
        ) { uri ->
            uri?.let {
                statisticsViewModel.export(
                    uri = it,
                    format = PlaybackStatisticsExportFormat.CSV,
                    snapshot =
                        buildPlaybackStatisticsSnapshot(
                            songs = mainState.songs,
                            playbackStats = mainState.playbackStats,
                        ),
                )
            }
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "收听统计与隐私",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "统计仅保存在本机。导出文件包含媒体标识、曲目信息、次数、完成度、进度和累计收听时长。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            StatisticsOverview(snapshot)

            if (snapshot.entries.isNotEmpty()) {
                HorizontalDivider()
                RankedSongs(snapshot.entries.take(TOP_ITEM_LIMIT))
                RankedGroups(
                    title = "最常听艺术家",
                    groups = topArtistInsights(snapshot, TOP_ITEM_LIMIT),
                )
                RankedGroups(
                    title = "最常听专辑",
                    groups = topAlbumInsights(snapshot, TOP_ITEM_LIMIT),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        jsonLauncher.launch(
                            playbackStatisticsFileName(PlaybackStatisticsExportFormat.JSON),
                        )
                    },
                    enabled = !statisticsState.isWorking,
                ) {
                    Text("导出 JSON")
                }
                TextButton(
                    onClick = {
                        csvLauncher.launch(
                            playbackStatisticsFileName(PlaybackStatisticsExportFormat.CSV),
                        )
                    },
                    enabled = !statisticsState.isWorking,
                ) {
                    Text("导出 CSV")
                }
                TextButton(
                    onClick = { showClearDialog = true },
                    enabled = !statisticsState.isWorking,
                ) {
                    Text("管理本地数据")
                }
            }

            if (statisticsState.isWorking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator()
                    Text("正在处理本地收听数据…")
                }
            }
            statisticsState.infoMessage?.let { message ->
                DismissibleMessage(
                    message = message,
                    isError = false,
                    onDismiss = statisticsViewModel::clearMessage,
                )
            }
            statisticsState.errorMessage?.let { message ->
                DismissibleMessage(
                    message = message,
                    isError = true,
                    onDismiss = statisticsViewModel::clearMessage,
                )
            }
        }
    }

    if (showClearDialog) {
        ClearListeningDataDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = { clearRecent, clearStats ->
                showClearDialog = false
                statisticsViewModel.clearListeningData(
                    clearRecent = clearRecent,
                    clearPlaybackStats = clearStats,
                )
            },
        )
    }
}

@Composable
private fun StatisticsOverview(snapshot: PlaybackStatisticsSnapshot) {
    StatisticValueRow("追踪项目", "${snapshot.summary.trackedMediaCount} 项")
    StatisticValueRow("当前可用", "${snapshot.summary.availableMediaCount} 首")
    StatisticValueRow("播放启动", "${snapshot.summary.totalPlayCount} 次")
    StatisticValueRow("完整听完", "${snapshot.summary.totalCompletedCount} 次")
    StatisticValueRow("累计收听", formatLongDuration(snapshot.summary.totalListenTimeMs))
}

@Composable
private fun RankedSongs(entries: List<PlaybackStatisticsEntry>) {
    Text(
        text = "收听时长最高",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    entries.forEachIndexed { index, entry ->
        val label =
            if (entry.available) {
                "${entry.title} · ${entry.artist}"
            } else {
                "当前不可用 · ${entry.mediaId}"
            }
        StatisticValueRow(
            label = "${index + 1}. $label",
            value = formatLongDuration(entry.totalListenTimeMs),
        )
    }
}

@Composable
private fun RankedGroups(
    title: String,
    groups: List<ListeningGroupInsight>,
) {
    if (groups.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    groups.forEachIndexed { index, group ->
        StatisticValueRow(
            label = "${index + 1}. ${group.name}",
            value = "${formatLongDuration(group.listenTimeMs)} · ${group.songCount} 首",
        )
    }
}

@Composable
private fun StatisticValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DismissibleMessage(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color =
                if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
        TextButton(onClick = onDismiss) {
            Text("关闭")
        }
    }
}

@Composable
private fun ClearListeningDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (clearRecent: Boolean, clearStats: Boolean) -> Unit,
) {
    var clearRecent by rememberSaveable { mutableStateOf(true) }
    var clearStats by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理本地收听数据") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionRow(
                    checked = clearRecent,
                    label = "清除最近播放顺序",
                    onCheckedChange = { clearRecent = it },
                )
                SelectionRow(
                    checked = clearStats,
                    label = "清除次数、完成度、时长和恢复位置",
                    onCheckedChange = { clearStats = it },
                )
                Text(
                    text = "收藏、自定义歌单、歌词文件、主题设置和设备音乐不会被删除。正在播放的歌曲之后仍可能生成新的统计。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(clearRecent, clearStats) },
                enabled = clearRecent || clearStats,
            ) {
                Text("确认清除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun SelectionRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(label)
    }
}

private fun formatLongDuration(durationMs: Long): String {
    val totalMinutes = durationMs.coerceAtLeast(0L) / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val minutes = totalMinutes % 60L
    return when {
        days > 0L -> "${days}天 ${hours}小时"
        hours > 0L -> "${hours}小时 ${minutes}分钟"
        else -> "${minutes}分钟"
    }
}

private const val TOP_ITEM_LIMIT = 5
