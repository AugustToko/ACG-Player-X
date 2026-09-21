package top.geek_studio.chenlongcould.musicplayer.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportEntryStatus
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportPreview
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportPreviewEntry
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkImage
import top.geek_studio.chenlongcould.musicplayer.ui.components.formatDuration

@Composable
fun PlaylistImportPreviewDialog(
    preview: PlaylistImportPreview?,
    librarySongs: List<Song>,
    isWorking: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onSelectionChange: (Int, String?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    if (preview == null) return

    var mappingEntryIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val mappingEntry = preview.entries.firstOrNull { it.index == mappingEntryIndex }

    LaunchedEffect(preview.entries.size) {
        if (mappingEntryIndex != null && mappingEntry == null) {
            mappingEntryIndex = null
        }
    }

    if (mappingEntry != null) {
        PlaylistImportSongPickerDialog(
            preview = preview,
            entry = mappingEntry,
            librarySongs = librarySongs,
            onSelect = { mediaId ->
                onSelectionChange(mappingEntry.index, mediaId)
                mappingEntryIndex = null
            },
            onDismiss = { mappingEntryIndex = null },
        )
        return
    }

    PlaylistImportOverviewDialog(
        preview = preview,
        librarySongs = librarySongs,
        isWorking = isWorking,
        errorMessage = errorMessage,
        onNameChange = onNameChange,
        onOpenMapping = { mappingEntryIndex = it },
        onSelectionChange = onSelectionChange,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError,
    )
}

@Composable
private fun PlaylistImportOverviewDialog(
    preview: PlaylistImportPreview,
    librarySongs: List<Song>,
    isWorking: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onOpenMapping: (Int) -> Unit,
    onSelectionChange: (Int, String?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    val songsById = remember(librarySongs) { librarySongs.associateBy { it.id.toString() } }
    var query by rememberSaveable { mutableStateOf("") }
    var attentionOnly by rememberSaveable { mutableStateOf(false) }

    val visibleEntries =
        remember(preview.entries, query, attentionOnly) {
            preview.entries.filter { entry ->
                (!attentionOnly || entry.needsImportAttention()) &&
                    entry.matchesImportQuery(query)
            }
        }

    AlertDialog(
        onDismissRequest = {
            if (!isWorking) onDismiss()
        },
        title = {
            Column {
                Text("M3U 导入预览")
                Text(
                    text = "已选择 ${preview.selectedCount} / ${preview.entries.size} 项",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 440.dp, max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = preview.preferredName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("歌单名称") },
                    singleLine = true,
                    enabled = !isWorking,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = attentionOnly,
                        onClick = { attentionOnly = !attentionOnly },
                        label = { Text("仅看需处理 ${preview.attentionCount}") },
                    )
                    if (preview.truncated) {
                        Text(
                            text = "文件超过 20,000 项，预览已截断",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索原始路径、标题或艺术家") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            TextButton(onClick = { query = "" }) {
                                Text("清除")
                            }
                        }
                    },
                    singleLine = true,
                )

                errorMessage?.let { message ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            TextButton(onClick = onClearError) {
                                Text("关闭")
                            }
                        }
                    }
                }

                if (isWorking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "正在创建歌单…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (visibleEntries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "没有符合当前筛选条件的导入条目",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = visibleEntries,
                            key = PlaylistImportPreviewEntry::index,
                        ) { entry ->
                            PlaylistImportEntryCard(
                                entry = entry,
                                selectedSong = entry.selectedMediaId?.let(songsById::get),
                                enabled = !isWorking,
                                onOpenMapping = { onOpenMapping(entry.index) },
                                onSkip = { onSelectionChange(entry.index, null) },
                                onRestoreAutomatic = {
                                    onSelectionChange(entry.index, entry.automaticMediaId)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled =
                    !isWorking &&
                        preview.selectedCount > 0 &&
                        preview.preferredName.isNotBlank(),
            ) {
                Text("创建歌单")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isWorking,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun PlaylistImportEntryCard(
    entry: PlaylistImportPreviewEntry,
    selectedSong: Song?,
    enabled: Boolean,
    onOpenMapping: () -> Unit,
    onSkip: () -> Unit,
    onRestoreAutomatic: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImportStatusBadge(entry.status)
                Text(
                    text = "#${entry.index + 1} ${entry.importDisplayTitle()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val sourceDetails = entry.importSourceDetails()
            if (sourceDetails.isNotBlank()) {
                Text(
                    text = sourceDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = entry.location,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text =
                        when {
                            selectedSong != null -> {
                                "导入为：${selectedSong.title} · ${selectedSong.artist}"
                            }

                            entry.selectedMediaId != null -> {
                                "保留离线媒体 ID：${entry.selectedMediaId}"
                            }

                            else -> "此条目将被跳过"
                        },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (entry.candidateCount > 0) {
                    Text(
                        text = "${entry.candidateCount} 个候选",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (
                    entry.selectedMediaId == null &&
                    entry.automaticMediaId != null &&
                    entry.status != PlaylistImportEntryStatus.DUPLICATE
                ) {
                    TextButton(
                        onClick = onRestoreAutomatic,
                        enabled = enabled,
                    ) {
                        Text("恢复自动")
                    }
                }
                if (entry.selectedMediaId != null) {
                    TextButton(
                        onClick = onSkip,
                        enabled = enabled,
                    ) {
                        Text("排除")
                    }
                }
                TextButton(
                    onClick = onOpenMapping,
                    enabled = enabled,
                ) {
                    Text(if (entry.selectedMediaId == null) "匹配" else "更改")
                }
            }
        }
    }
}

@Composable
private fun PlaylistImportSongPickerDialog(
    preview: PlaylistImportPreview,
    entry: PlaylistImportPreviewEntry,
    librarySongs: List<Song>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val songsById = remember(librarySongs) { librarySongs.associateBy { it.id.toString() } }
    val candidateSongs =
        remember(entry.candidateMediaIds, librarySongs) {
            entry.candidateMediaIds.mapNotNull(songsById::get)
        }
    val usedByOtherEntries =
        remember(preview.entries, entry.index) {
            preview.entries
                .asSequence()
                .filter { it.index != entry.index }
                .mapNotNull(PlaylistImportPreviewEntry::selectedMediaId)
                .toHashSet()
        }
    var query by rememberSaveable(entry.index) { mutableStateOf("") }
    val visibleSongs =
        remember(query, candidateSongs, librarySongs) {
            if (query.isBlank()) {
                candidateSongs.take(MAX_MAPPING_RESULTS)
            } else {
                filterSongs(librarySongs, query).take(MAX_MAPPING_RESULTS)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("选择对应歌曲")
                Text(
                    text = "#${entry.index + 1} ${entry.importDisplayTitle()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 420.dp, max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text =
                        if (candidateSongs.isNotEmpty()) {
                            "已找到 ${entry.candidateCount} 个自动候选；也可以搜索整个音乐库。"
                        } else {
                            "输入歌曲、艺术家、专辑或目录关键词，在当前音乐库中手工映射。"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索当前音乐库") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            TextButton(onClick = { query = "" }) {
                                Text("清除")
                            }
                        }
                    },
                    singleLine = true,
                )

                if (visibleSongs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text =
                                if (query.isBlank()) {
                                    "没有自动候选，请输入关键词搜索"
                                } else {
                                    "当前音乐库中没有匹配结果"
                                },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(
                            items = visibleSongs,
                            key = Song::id,
                        ) { song ->
                            val mediaId = song.id.toString()
                            val alreadyUsed = mediaId in usedByOtherEntries
                            ListItem(
                                leadingContent = {
                                    ArtworkImage(
                                        artworkUri = song.albumArtUri?.let(Uri::parse),
                                        fallbackUri = Uri.parse(song.contentUri),
                                        seed = song.id,
                                        modifier = Modifier.size(44.dp),
                                        cornerRadius = 12.dp,
                                        glyphSize = 18,
                                        requestSize = 88.dp,
                                        contentDescription = song.album,
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        text = song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = "${song.artist} · ${song.album}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingContent = {
                                    if (alreadyUsed) {
                                        Text(
                                            text = "已用于其他条目",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    } else {
                                        TextButton(onClick = { onSelect(mediaId) }) {
                                            Text("选择")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                if (query.isNotBlank() && visibleSongs.size >= MAX_MAPPING_RESULTS) {
                    Text(
                        text = "仅显示前 $MAX_MAPPING_RESULTS 条结果，请继续缩小关键词范围。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            val automaticMediaId = entry.automaticMediaId
            if (
                automaticMediaId != null &&
                automaticMediaId !in usedByOtherEntries
            ) {
                TextButton(onClick = { onSelect(automaticMediaId) }) {
                    Text(
                        if (songsById[automaticMediaId] == null) {
                            "保留离线 ID"
                        } else {
                            "恢复自动"
                        },
                    )
                }
            } else {
                TextButton(onClick = { onSelect(null) }) {
                    Text("跳过此项")
                }
            }
        },
        dismissButton = {
            Row {
                if (
                    entry.automaticMediaId != null &&
                    entry.automaticMediaId !in usedByOtherEntries
                ) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text("跳过此项")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("返回预览")
                }
            }
        },
    )
}

@Composable
private fun ImportStatusBadge(status: PlaylistImportEntryStatus) {
    val containerColor =
        when (status) {
            PlaylistImportEntryStatus.MATCHED -> MaterialTheme.colorScheme.primaryContainer
            PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE ->
                MaterialTheme.colorScheme.secondaryContainer
            PlaylistImportEntryStatus.AMBIGUOUS -> MaterialTheme.colorScheme.tertiaryContainer
            PlaylistImportEntryStatus.UNMATCHED -> MaterialTheme.colorScheme.errorContainer
            PlaylistImportEntryStatus.DUPLICATE -> MaterialTheme.colorScheme.surfaceContainerHighest
        }
    val contentColor =
        when (status) {
            PlaylistImportEntryStatus.MATCHED -> MaterialTheme.colorScheme.onPrimaryContainer
            PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE ->
                MaterialTheme.colorScheme.onSecondaryContainer
            PlaylistImportEntryStatus.AMBIGUOUS -> MaterialTheme.colorScheme.onTertiaryContainer
            PlaylistImportEntryStatus.UNMATCHED -> MaterialTheme.colorScheme.onErrorContainer
            PlaylistImportEntryStatus.DUPLICATE -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
    ) {
        Text(
            text =
                when (status) {
                    PlaylistImportEntryStatus.MATCHED -> "自动匹配"
                    PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE -> "离线保留"
                    PlaylistImportEntryStatus.AMBIGUOUS -> "多个候选"
                    PlaylistImportEntryStatus.UNMATCHED -> "未匹配"
                    PlaylistImportEntryStatus.DUPLICATE -> "重复条目"
                },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

private fun PlaylistImportPreviewEntry.needsImportAttention(): Boolean =
    selectedMediaId == null

private fun PlaylistImportPreviewEntry.matchesImportQuery(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isEmpty()) return true
    return sequenceOf(
        title,
        artist,
        fileNameHint,
        folderHint,
        location,
        selectedMediaId,
    ).any { value -> value?.contains(normalized, ignoreCase = true) == true }
}

private fun PlaylistImportPreviewEntry.importDisplayTitle(): String =
    title
        ?.takeIf(String::isNotBlank)
        ?: fileNameHint?.takeIf(String::isNotBlank)
        ?: location
            .substringBefore('?')
            .replace('\\', '/')
            .substringAfterLast('/')
            .takeIf(String::isNotBlank)
        ?: "未命名条目"

private fun PlaylistImportPreviewEntry.importSourceDetails(): String =
    buildList {
        artist?.takeIf(String::isNotBlank)?.let(::add)
        durationSeconds?.takeIf { it >= 0L }?.let { add(formatDuration(it * 1_000L)) }
        folderHint?.takeIf(String::isNotBlank)?.let(::add)
    }.joinToString(" · ")

private const val MAX_MAPPING_RESULTS = 100
