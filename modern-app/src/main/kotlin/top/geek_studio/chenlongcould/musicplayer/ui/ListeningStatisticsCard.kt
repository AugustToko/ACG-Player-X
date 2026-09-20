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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.geek_studio.chenlongcould.musicplayer.data.ListeningGroupInsight
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsEntry
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsExportFormat
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportMode
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportPreview
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
    val folderSignature =
        remember(mainState.authorizedFolders) {
            mainState.authorizedFolders
                .sortedBy { it.uriString }
                .map { it.uriString to it.isAvailable }
        }

    LaunchedEffect(mainState.hasAudioPermission, folderSignature, mainState.totalSongCount) {
        statisticsViewModel.refreshLibraryMetadata(
            includeMediaStore = mainState.hasAudioPermission,
            authorizedFolders = mainState.authorizedFolders,
            expectedSongCount = mainState.totalSongCount,
        )
    }

    val hasCompleteLibraryMetadata =
        !statisticsState.isRefreshingLibraryMetadata &&
            (mainState.totalSongCount == 0 || statisticsState.librarySongs.size == mainState.totalSongCount)
    val statisticsSongs = if (hasCompleteLibraryMetadata) statisticsState.librarySongs else mainState.songs
    val snapshot =
        remember(statisticsSongs, mainState.playbackStats) {
            buildPlaybackStatisticsSnapshot(
                songs = statisticsSongs,
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
                    snapshot = buildPlaybackStatisticsSnapshot(statisticsState.librarySongs, mainState.playbackStats),
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
                    snapshot = buildPlaybackStatisticsSnapshot(statisticsState.librarySongs, mainState.playbackStats),
                )
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(statisticsViewModel::prepareImport)
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("收听统计与隐私", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "统计仅保存在本机。导出文件包含媒体标识、曲目信息、次数、完成度、进度和累计收听时长。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            StatisticsOverview(snapshot)
            if (snapshot.entries.isNotEmpty()) {
                HorizontalDivider()
                RankedSongs(snapshot.entries.take(TOP_ITEM_LIMIT))
                RankedGroups("最常听艺术家", topArtistInsights(snapshot, TOP_ITEM_LIMIT))
                RankedGroups("最常听专辑", topAlbumInsights(snapshot, TOP_ITEM_LIMIT))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { jsonLauncher.launch(playbackStatisticsFileName(PlaybackStatisticsExportFormat.JSON)) },
                        enabled = !statisticsState.isWorking && hasCompleteLibraryMetadata,
                    ) { Text("导出 JSON") }
                    TextButton(
                        onClick = { csvLauncher.launch(playbackStatisticsFileName(PlaybackStatisticsExportFormat.CSV)) },
                        enabled = !statisticsState.isWorking && hasCompleteLibraryMetadata,
                    ) { Text("导出 CSV") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        enabled = !statisticsState.isWorking && hasCompleteLibraryMetadata,
                    ) { Text("导入 JSON") }
                    TextButton(
                        onClick = { showClearDialog = true },
                        enabled = !statisticsState.isWorking,
                    ) { Text("管理本地数据") }
                }
            }

            if (statisticsState.isRefreshingLibraryMetadata) {
                Text(
                    "正在同步完整音乐库元数据，导入和导出将在完成后启用…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (statisticsState.isWorking) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text("正在处理本地收听数据…")
                }
            }
            statisticsState.infoMessage?.let {
                DismissibleMessage(it, false, statisticsViewModel::clearMessage)
            }
            statisticsState.errorMessage?.let {
                DismissibleMessage(it, true, statisticsViewModel::clearMessage)
            }
        }
    }

    if (showClearDialog) {
        ClearListeningDataDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = { clearRecent, clearStats ->
                showClearDialog = false
                statisticsViewModel.clearListeningData(clearRecent, clearStats)
            },
        )
    }
    statisticsState.importPreview?.let { preview ->
        PlaybackStatisticsImportDialog(
            preview = preview,
            mode = statisticsState.importMode,
            retainUnavailable = statisticsState.retainUnavailable,
            isWorking = statisticsState.isWorking,
            onModeChange = statisticsViewModel::setImportMode,
            onRetainUnavailableChange = statisticsViewModel::setRetainUnavailable,
            onConfirm = statisticsViewModel::confirmImport,
            onDismiss = statisticsViewModel::cancelImport,
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
    Text("收听时长最高", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    entries.forEachIndexed { index, entry ->
        val label = if (entry.available) "${entry.title} · ${entry.artist}" else "当前不可用 · ${entry.mediaId}"
        StatisticValueRow("${index + 1}. $label", formatLongDuration(entry.totalListenTimeMs))
    }
}

@Composable
private fun RankedGroups(title: String, groups: List<ListeningGroupInsight>) {
    if (groups.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    groups.forEachIndexed { index, group ->
        StatisticValueRow("${index + 1}. ${group.name}", "${formatLongDuration(group.listenTimeMs)} · ${group.songCount} 首")
    }
}

@Composable
private fun StatisticValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DismissibleMessage(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            message,
            modifier = Modifier.weight(1f),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onDismiss) { Text("关闭") }
    }
}

@Composable
private fun PlaybackStatisticsImportDialog(
    preview: PlaybackStatisticsImportPreview,
    mode: PlaybackStatisticsImportMode,
    retainUnavailable: Boolean,
    isWorking: Boolean,
    onModeChange: (PlaybackStatisticsImportMode) -> Unit,
    onRetainUnavailableChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedCount = preview.selectedStats(retainUnavailable).size
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("预览收听统计导入") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "schema v${preview.schemaVersion} · ${formatImportTimestamp(preview.generatedAtMs)} · 源文件 ${preview.sourceEntryCount} 项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatisticValueRow("精确 ID + 元数据", "${preview.exactMatchCount} 项")
                StatisticValueRow("跨设备元数据匹配", "${preview.portableMatchCount} 项")
                StatisticValueRow("当前不可用", "${preview.unavailableEntryCount} 项")
                StatisticValueRow("歧义 / 跳过", "${preview.ambiguousEntryCount + preview.skippedEntryCount} 项")
                StatisticValueRow("最终可导入", "$selectedCount 项")
                HorizontalDivider()
                Text("导入方式", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == PlaybackStatisticsImportMode.MERGE,
                        onClick = { onModeChange(PlaybackStatisticsImportMode.MERGE) },
                        label = { Text("幂等合并") },
                    )
                    FilterChip(
                        selected = mode == PlaybackStatisticsImportMode.REPLACE,
                        onClick = { onModeChange(PlaybackStatisticsImportMode.REPLACE) },
                        label = { Text("完全替换") },
                    )
                }
                Text(
                    if (mode == PlaybackStatisticsImportMode.MERGE) {
                        "相同项目的次数、完成数和时长取较大值；重复导入同一备份不会翻倍。"
                    } else {
                        "只保留本次选中的导入项目，当前设备上不在文件中的播放统计将被删除。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mode == PlaybackStatisticsImportMode.REPLACE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (preview.unavailableEntryCount > 0) {
                    SelectionRow(retainUnavailable, "保留当前不可用媒体 ID", onRetainUnavailableChange)
                }
                preview.warnings.forEach { warning ->
                    Text("注意：$warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "导入只修改收听统计，不会改动收藏、最近播放顺序、歌单、歌词或音乐文件。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking && selectedCount > 0) {
                Text(if (mode == PlaybackStatisticsImportMode.MERGE) "合并导入" else "替换导入")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isWorking) { Text("取消") } },
    )
}

@Composable
private fun ClearListeningDataDialog(onDismiss: () -> Unit, onConfirm: (Boolean, Boolean) -> Unit) {
    var clearRecent by rememberSaveable { mutableStateOf(true) }
    var clearStats by rememberSaveable { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理本地收听数据") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionRow(clearRecent, "清除最近播放顺序") { clearRecent = it }
                SelectionRow(clearStats, "清除次数、完成度、时长和恢复位置") { clearStats = it }
                Text(
                    "收藏、自定义歌单、歌词文件、主题设置和设备音乐不会被删除。正在播放的歌曲之后仍可能生成新的统计。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(clearRecent, clearStats) }, enabled = clearRecent || clearStats) { Text("确认清除") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SelectionRow(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

private fun formatImportTimestamp(timestampMs: Long): String {
    if (timestampMs <= 0L) return "导出时间未知"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestampMs))
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
