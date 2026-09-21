package top.geek_studio.chenlongcould.musicplayer.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.geek_studio.chenlongcould.musicplayer.data.displayMusicFolderPath
import top.geek_studio.chenlongcould.musicplayer.data.isAuthorizedFolderPath
import top.geek_studio.chenlongcould.musicplayer.data.progressPercent
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkImage
import top.geek_studio.chenlongcould.musicplayer.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: MainUiState,
    notificationPermissionRequired: Boolean,
    onRequestPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSectionChange: (LibrarySection) -> Unit,
    onClearCollectionFilter: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onClearRecentHistory: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACG Player X",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = librarySummary(state),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = onRefresh,
                    enabled = state.hasLibraryAccess && !state.isLoading,
                ) {
                    Text("刷新")
                }
            },
        )

        LibrarySearchField(
            query = state.query,
            onQueryChange = onQueryChange,
        )

        LibrarySectionSelector(
            selected = state.section,
            onSelected = onSectionChange,
        )

        if (notificationPermissionRequired) {
            NotificationPermissionCard(onRequest = onRequestNotificationPermission)
        }
        if (!state.hasAudioPermission && state.hasAuthorizedFolderAccess) {
            MediaPermissionCard(onRequest = onRequestPermission)
        }

        state.activeFilter?.let { filter ->
            ActiveCollectionFilterCard(
                filter = filter,
                onClear = onClearCollectionFilter,
            )
        }

        Box(Modifier.weight(1f)) {
            when {
                !state.permissionChecked -> {
                    LoadingState(message = "正在检查音乐来源…")
                }

                !state.hasLibraryAccess -> {
                    PermissionState(
                        onRequestPermission = onRequestPermission,
                        onAddAuthorizedFolder = onAddAuthorizedFolder,
                    )
                }

                state.isLoading -> {
                    LoadingState(message = "正在读取音乐库…")
                }

                state.errorMessage != null -> {
                    ErrorState(
                        message = state.errorMessage,
                        onRetry = onRefresh,
                    )
                }

                state.songs.isEmpty() -> {
                    EmptyLibraryState(
                        section = state.section,
                        hasQuery = state.query.isNotBlank() || state.activeFilter != null,
                        libraryIsEmpty = state.totalSongCount == 0,
                        onAddAuthorizedFolder = onAddAuthorizedFolder,
                    )
                }

                else -> {
                    LibraryContent(
                        state = state,
                        onPlaySong = onPlaySong,
                        onToggleFavorite = onToggleFavorite,
                        onClearRecentHistory = onClearRecentHistory,
                        onOpenAlbum = onOpenAlbum,
                        onOpenArtist = onOpenArtist,
                        onOpenFolder = onOpenFolder,
                    )
                }
            }
        }
    }
}

private fun librarySummary(state: MainUiState): String =
    when (state.section) {
        LibrarySection.FAVORITES ->
            "收藏 ${state.songs.size} 首 · 总库 ${state.totalSongCount} 首"

        LibrarySection.RECENT ->
            "最近播放 ${state.songs.size} 首 · 最多保留 100 首"

        LibrarySection.RECENT_WEEK ->
            "近 7 天 ${state.songs.size} 首 · 按最近播放排序"

        LibrarySection.RECENTLY_ADDED ->
            "最近添加 ${state.songs.size} 首 · 按可用时间排序"

        LibrarySection.MOST_PLAYED ->
            "常听 ${state.songs.size} 首 · 累计 ${state.totalPlayCount} 次 · ${formatListenTime(state.totalListenTimeMs)}"

        LibrarySection.IN_PROGRESS ->
            "继续听 ${state.songs.size} 首 · 全库 ${state.inProgressSongCount} 首"

        LibrarySection.COMPLETED ->
            "已听完 ${state.completedSongCount} 首 · 共 ${state.totalCompletedCount} 次"

        LibrarySection.LONG_FORM ->
            "长音频 ${state.songs.size} 首 · 20 分钟以上"

        LibrarySection.UNPLAYED ->
            "未播放 ${state.songs.size} 首 · 已播放 ${state.playedSongCount} 首"

        else ->
            when {
                state.totalSongCount == 0 -> "本地音乐库"
                state.songs.size != state.totalSongCount ->
                    "显示 ${state.songs.size} / ${state.totalSongCount} 首"
                state.authorizedFolderSongCount > 0 ->
                    "系统 ${state.mediaStoreSongCount} · 授权目录 ${state.authorizedFolderSongCount} · 共 ${state.totalSongCount} 首"
                else -> "${state.totalSongCount} 首本地音乐"
            }
    }

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        placeholder = {
            Text("搜索歌曲、艺术家、专辑或文件夹")
        },
        leadingIcon = {
            Text(
                text = "⌕",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text("清除")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(
                onSearch = { focusManager.clearFocus() },
            ),
    )
}

@Composable
private fun LibrarySectionSelector(
    selected: LibrarySection,
    onSelected: (LibrarySection) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibrarySection.entries.forEach { section ->
            FilterChip(
                selected = selected == section,
                onClick = { onSelected(section) },
                label = {
                    Text(
                        when (section) {
                            LibrarySection.SONGS -> "歌曲"
                            LibrarySection.FAVORITES -> "收藏"
                            LibrarySection.RECENT -> "最近"
                            LibrarySection.RECENT_WEEK -> "近 7 天"
                            LibrarySection.RECENTLY_ADDED -> "新添加"
                            LibrarySection.MOST_PLAYED -> "常听"
                            LibrarySection.IN_PROGRESS -> "继续听"
                            LibrarySection.COMPLETED -> "已听完"
                            LibrarySection.LONG_FORM -> "长音频"
                            LibrarySection.UNPLAYED -> "未播放"
                            LibrarySection.ALBUMS -> "专辑"
                            LibrarySection.ARTISTS -> "艺术家"
                            LibrarySection.FOLDERS -> "文件夹"
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun LibraryContent(
    state: MainUiState,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onClearRecentHistory: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String, String) -> Unit,
) {
    when (state.section) {
        LibrarySection.SONGS,
        LibrarySection.FAVORITES,
        -> {
            SongList(
                songs = state.songs,
                currentMediaId = state.playback.mediaId,
                favoriteMediaIds = state.favoriteMediaIds,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
            )
        }

        LibrarySection.RECENT -> {
            Column(Modifier.fillMaxSize()) {
                RecentHistoryHeader(
                    visibleCount = state.songs.size,
                    onClear = onClearRecentHistory,
                )
                SongList(
                    songs = state.songs,
                    currentMediaId = state.playback.mediaId,
                    favoriteMediaIds = state.favoriteMediaIds,
                    onPlaySong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        LibrarySection.RECENT_WEEK -> {
            SmartSongSection(
                title = "近 7 天播放",
                message = "只显示过去 7 天产生有效播放会话的歌曲。",
                state = state,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                contextLabel = { song ->
                    val timestamp = state.playbackStats[song.id.toString()]?.lastPlayedAtMs ?: 0L
                    formatRelativePlaybackTime(timestamp)
                },
            )
        }

        LibrarySection.RECENTLY_ADDED -> {
            SmartSongSection(
                title = "最近添加",
                message = "MediaStore 使用入库时间；授权目录使用文档最后修改时间。",
                state = state,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                contextLabel = { song -> formatAddedDate(song.dateAddedMs) },
            )
        }

        LibrarySection.MOST_PLAYED -> {
            SmartSongSection(
                title = "最常播放",
                message = "按本机记录的播放次数排序；次数相同则最近播放优先。",
                state = state,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                contextLabel = { song ->
                    val count = state.playbackStats[song.id.toString()]?.playCount ?: 0
                    "$count 次播放"
                },
            )
        }

        LibrarySection.IN_PROGRESS -> {
            SmartSongSection(
                title = "继续听",
                message = "保留已实际收听至少 10 秒、进度位于 10% 到 89% 的歌曲。",
                state = state,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                contextLabel = { song ->
                    state.playbackStats[song.id.toString()]?.let { stats ->
                        "继续 ${stats.progressPercent()}% · ${formatDuration(stats.lastPositionMs)} / ${formatDuration(stats.durationMs)}"
                    }
                },
            )
        }

        LibrarySection.COMPLETED -> {
            SmartSongSection(
                title = "已听完",
                message = "达到曲尾阈值且满足有效收听时间后才计为完成，快进跳转不会虚增。",
                state = state,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                contextLabel = { song ->
                    state.playbackStats[song.id.toString()]?.let { stats ->
                        buildString {
                            append("听完 ${stats.completedCount} 次")
                            if (stats.lastCompletedAtMs > 0L) {
                                append(" · ")
                                append(formatRelativePlaybackTime(stats.lastCompletedAtMs))
                            }
                        }
                    }
                },
            )
        }

        LibrarySection.LONG_FORM -> {
            SmartSongSection(
                title = "长音频",
                message = "时长不少于 20 分钟，适合播客、广播剧、现场和长篇合集。",
                state = state,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                contextLabel = { song -> "时长 ${formatDuration(song.durationMs)}" },
            )
        }

        LibrarySection.UNPLAYED -> {
            SmartSongSection(
                title = "尚未播放",
                message = "当前可用音乐库中还没有进入播放状态的歌曲。",
                state = state,
                onPlaySong = onPlaySong,
                onToggleFavorite = onToggleFavorite,
                contextLabel = { "尚未播放" },
            )
        }

        LibrarySection.ALBUMS -> {
            val albums by remember(state.songs) {
                derivedStateOf {
                    state.songs
                        .groupBy(Song::album)
                        .map { (name, songs) ->
                            songs.toCollectionSummary(
                                key = "album:$name",
                                name = name,
                                subtitle = "${songs.size} 首 • ${songs.firstOrNull()?.artist.orEmpty()}",
                                value = name,
                            )
                        }
                        .sortedBy { it.name.lowercase() }
                }
            }

            CollectionList(
                collections = albums,
                onClick = { onOpenAlbum(it.value) },
            )
        }

        LibrarySection.ARTISTS -> {
            val artists by remember(state.songs) {
                derivedStateOf {
                    state.songs
                        .groupBy(Song::artist)
                        .map { (name, songs) ->
                            songs.toCollectionSummary(
                                key = "artist:$name",
                                name = name,
                                subtitle = "${songs.size} 首 • ${songs.map(Song::album).distinct().size} 张专辑",
                                value = name,
                            )
                        }
                        .sortedBy { it.name.lowercase() }
                }
            }

            CollectionList(
                collections = artists,
                onClick = { onOpenArtist(it.value) },
            )
        }

        LibrarySection.FOLDERS -> {
            val folders by remember(state.songs) {
                derivedStateOf {
                    state.songs
                        .groupBy(Song::folderPath)
                        .map { (path, songs) ->
                            val sourceLabel =
                                if (isAuthorizedFolderPath(path)) "授权目录" else "系统目录"
                            songs.toCollectionSummary(
                                key = "folder:$path",
                                name = songs.firstOrNull()?.folderName.orEmpty(),
                                subtitle =
                                    "${songs.size} 首 • $sourceLabel • ${displayMusicFolderPath(path)}",
                                value = path,
                            )
                        }
                        .sortedBy { it.name.lowercase() }
                }
            }

            CollectionList(
                collections = folders,
                onClick = { onOpenFolder(it.value, it.name) },
            )
        }
    }
}

@Composable
private fun SmartSongSection(
    title: String,
    message: String,
    state: MainUiState,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    contextLabel: (Song) -> String?,
) {
    Column(Modifier.fillMaxSize()) {
        SmartListHeader(
            title = title,
            message = message,
            visibleCount = state.songs.size,
        )
        SongList(
            songs = state.songs,
            currentMediaId = state.playback.mediaId,
            favoriteMediaIds = state.favoriteMediaIds,
            onPlaySong = onPlaySong,
            onToggleFavorite = onToggleFavorite,
            contextLabel = contextLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    currentMediaId: String?,
    favoriteMediaIds: Set<String>,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contextLabel: (Song) -> String? = { null },
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(
            items = songs,
            key = { it.id },
        ) { song ->
            val mediaId = song.id.toString()
            SongRow(
                song = song,
                isPlaying = currentMediaId == mediaId,
                isFavorite = mediaId in favoriteMediaIds,
                contextLabel = contextLabel(song),
                onClick = { onPlaySong(song) },
                onToggleFavorite = { onToggleFavorite(song) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 84.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    contextLabel: String?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val supportingText =
        buildList {
            add(song.artist)
            add(song.album)
            add(song.folderName)
            if (isAuthorizedFolderPath(song.folderPath)) add("SAF")
            contextLabel?.takeIf(String::isNotBlank)?.let(::add)
        }.joinToString(" • ")

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            ArtworkImage(
                artworkUri = song.albumArtUri?.let(Uri::parse),
                fallbackUri = Uri.parse(song.contentUri),
                seed = song.id,
                modifier = Modifier.size(54.dp),
                cornerRadius = 15.dp,
                glyphSize = 24,
                requestSize = 112.dp,
                contentDescription = song.album,
            )
        },
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = song.title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    color =
                        if (isPlaying) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatDuration(song.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supportingContent = {
            Text(
                text = supportingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (isPlaying) {
                    Text(
                        text = "播放中",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(
                    onClick = onToggleFavorite,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = if (isFavorite) "★" else "☆",
                        style = MaterialTheme.typography.titleLarge,
                        color =
                            if (isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        },
    )
}

@Composable
private fun RecentHistoryHeader(
    visibleCount: Int,
    onClear: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "最近播放",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$visibleCount 首可用歌曲，按最近播放排序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClear) {
                Text("清空")
            }
        }
    }
}

@Composable
private fun SmartListHeader(
    title: String,
    message: String,
    visibleCount: Int,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = "$title · $visibleCount 首",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatAddedDate(dateAddedMs: Long): String {
    if (dateAddedMs <= 0L) return "添加时间未知"
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateAddedMs))
}

private fun formatRelativePlaybackTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "时间未知"
    val elapsedMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val minuteMs = 60_000L
    val hourMs = 60L * minuteMs
    val dayMs = 24L * hourMs
    return when {
        elapsedMs < minuteMs -> "刚刚"
        elapsedMs < hourMs -> "${elapsedMs / minuteMs} 分钟前"
        elapsedMs < dayMs -> "${elapsedMs / hourMs} 小时前"
        elapsedMs < 7L * dayMs -> "${elapsedMs / dayMs} 天前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestampMs))
    }
}

private fun formatListenTime(durationMs: Long): String {
    val totalMinutes = durationMs.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "收听 ${hours} 小时 ${minutes} 分"
        hours > 0L -> "收听 ${hours} 小时"
        else -> "收听 ${minutes} 分钟"
    }
}

private data class CollectionSummary(
    val key: String,
    val name: String,
    val subtitle: String,
    val value: String,
    val seed: Long,
    val artworkUri: String?,
    val contentUri: String?,
)

private fun List<Song>.toCollectionSummary(
    key: String,
    name: String,
    subtitle: String,
    value: String,
): CollectionSummary {
    val representative = firstOrNull()
    return CollectionSummary(
        key = key,
        name = name,
        subtitle = subtitle,
        value = value,
        seed = representative?.id ?: 0L,
        artworkUri = representative?.albumArtUri,
        contentUri = representative?.contentUri,
    )
}

@Composable
private fun CollectionList(
    collections: List<CollectionSummary>,
    onClick: (CollectionSummary) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(
            items = collections,
            key = { it.key },
        ) { collection ->
            ListItem(
                modifier = Modifier.clickable { onClick(collection) },
                leadingContent = {
                    ArtworkImage(
                        artworkUri = collection.artworkUri?.let(Uri::parse),
                        fallbackUri = collection.contentUri?.let(Uri::parse),
                        seed = collection.seed,
                        modifier = Modifier.size(64.dp),
                        cornerRadius = 18.dp,
                        glyphSize = 28,
                        requestSize = 128.dp,
                        contentDescription = collection.name,
                    )
                },
                headlineContent = {
                    Text(
                        text = collection.name,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        text = collection.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    Text("›", style = MaterialTheme.typography.headlineSmall)
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 94.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun NotificationPermissionCard(onRequest: () -> Unit) {
    PermissionHintCard(
        title = "允许播放通知",
        message = "用于后台播放时显示系统媒体控件。",
        action = "授权",
        onAction = onRequest,
    )
}

@Composable
private fun MediaPermissionCard(onRequest: () -> Unit) {
    PermissionHintCard(
        title = "当前仅显示授权目录",
        message = "授予音乐权限后，还会合并系统 MediaStore 中的歌曲。",
        action = "授予权限",
        onAction = onRequest,
    )
}

@Composable
private fun PermissionHintCard(
    title: String,
    message: String,
    action: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            TextButton(onClick = onAction) {
                Text(action)
            }
        }
    }
}

@Composable
private fun ActiveCollectionFilterCard(
    filter: CollectionFilter,
    onClear: () -> Unit,
) {
    val typeLabel =
        when (filter.type) {
            CollectionType.ALBUM -> "专辑"
            CollectionType.ARTIST -> "艺术家"
            CollectionType.FOLDER -> "文件夹"
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$typeLabel：${filter.label}",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onClear) {
                Text("显示全部")
            }
        }
    }
}

@Composable
private fun LoadingState(message: String) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(18.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionState(
    onRequestPermission: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = "♫",
                modifier = Modifier.padding(horizontal = 34.dp, vertical = 24.dp),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "选择音乐访问方式",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "可以授予系统音乐权限，也可以只授权一个或多个目录。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRequestPermission) {
                Text("授予音乐权限")
            }
            TextButton(onClick = onAddAuthorizedFolder) {
                Text("选择音乐目录")
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "读取音乐库失败",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyLibraryState(
    section: LibrarySection,
    hasQuery: Boolean,
    libraryIsEmpty: Boolean,
    onAddAuthorizedFolder: () -> Unit,
) {
    val title =
        when {
            hasQuery -> "没有匹配的歌曲"
            libraryIsEmpty -> "音乐库为空"
            section == LibrarySection.FAVORITES -> "还没有收藏歌曲"
            section == LibrarySection.RECENT -> "还没有播放记录"
            section == LibrarySection.RECENT_WEEK -> "近 7 天还没有播放"
            section == LibrarySection.MOST_PLAYED -> "还没有播放统计"
            section == LibrarySection.IN_PROGRESS -> "没有待继续的歌曲"
            section == LibrarySection.COMPLETED -> "还没有听完的歌曲"
            section == LibrarySection.LONG_FORM -> "没有长音频"
            section == LibrarySection.UNPLAYED -> "当前歌曲都已播放过"
            section == LibrarySection.RECENTLY_ADDED -> "没有可排序的歌曲"
            else -> "音乐库为空"
        }
    val message =
        when {
            hasQuery -> "清除搜索或集合筛选后再试。"
            libraryIsEmpty -> "可以刷新系统媒体库，或授权另一个音乐目录。"
            section == LibrarySection.FAVORITES -> "点击歌曲右侧的星标即可加入收藏。"
            section == LibrarySection.RECENT -> "开始播放歌曲后，这里会按最近顺序记录。"
            section == LibrarySection.RECENT_WEEK -> "过去 7 天没有产生有效播放会话。"
            section == LibrarySection.MOST_PLAYED -> "开始播放歌曲后，这里会按累计次数排序。"
            section == LibrarySection.IN_PROGRESS -> "实际收听至少 10 秒并停在中间进度后，会出现在这里。"
            section == LibrarySection.COMPLETED -> "接近曲尾且满足有效收听时间后，才会计为听完。"
            section == LibrarySection.LONG_FORM -> "当前音乐库中没有 20 分钟以上的音频。"
            section == LibrarySection.UNPLAYED -> "当前音乐库里的歌曲已经全部产生播放记录。"
            section == LibrarySection.RECENTLY_ADDED -> "当前音乐来源没有提供可用的时间信息。"
            else -> "可以刷新系统媒体库，或授权另一个音乐目录。"
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!hasQuery && libraryIsEmpty) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAddAuthorizedFolder) {
                Text("添加授权目录")
            }
        }
    }
}
