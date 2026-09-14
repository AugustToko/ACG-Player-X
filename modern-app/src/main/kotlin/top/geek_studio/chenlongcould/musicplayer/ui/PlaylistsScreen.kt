package top.geek_studio.chenlongcould.musicplayer.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.geek_studio.chenlongcould.musicplayer.data.UserPlaylist
import top.geek_studio.chenlongcould.musicplayer.data.countUnavailablePlaylistSongs
import top.geek_studio.chenlongcould.musicplayer.data.resolvePlaylistSongs
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkImage
import top.geek_studio.chenlongcould.musicplayer.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlistState: PlaylistUiState,
    librarySongs: List<Song>,
    currentMediaId: String?,
    onOpenPlaylist: (String) -> Unit,
    onClosePlaylist: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddSongs: (String, Collection<String>) -> Unit,
    onRemoveSong: (String, String) -> Unit,
    onSwapSongs: (String, String, String) -> Unit,
    onClearSongs: (String) -> Unit,
    onRemoveUnavailableSongs: (String, Set<String>) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePlaylist = playlistState.activePlaylist
    if (activePlaylist == null) {
        PlaylistOverview(
            playlists = playlistState.playlists,
            librarySongs = librarySongs,
            errorMessage = playlistState.errorMessage,
            isWorking = playlistState.isWorking,
            onOpenPlaylist = onOpenPlaylist,
            onCreatePlaylist = onCreatePlaylist,
            onRenamePlaylist = onRenamePlaylist,
            onDeletePlaylist = onDeletePlaylist,
            onPlayAll = onPlayAll,
            onClearError = onClearError,
            modifier = modifier,
        )
    } else {
        PlaylistDetail(
            playlist = activePlaylist,
            librarySongs = librarySongs,
            currentMediaId = currentMediaId,
            errorMessage = playlistState.errorMessage,
            isWorking = playlistState.isWorking,
            onBack = onClosePlaylist,
            onRenamePlaylist = onRenamePlaylist,
            onAddSongs = onAddSongs,
            onRemoveSong = onRemoveSong,
            onSwapSongs = onSwapSongs,
            onClearSongs = onClearSongs,
            onRemoveUnavailableSongs = onRemoveUnavailableSongs,
            onPlaySong = onPlaySong,
            onPlayAll = onPlayAll,
            onClearError = onClearError,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlaylistOverview(
    playlists: List<UserPlaylist>,
    librarySongs: List<Song>,
    errorMessage: String?,
    isWorking: Boolean,
    onOpenPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<UserPlaylist?>(null) }
    var deleteTarget by remember { mutableStateOf<UserPlaylist?>(null) }

    Column(modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("歌单", fontWeight = FontWeight.Bold)
                    Text(
                        text = "${playlists.size} 个本地歌单",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { showCreateDialog = true },
                    enabled = !isWorking,
                ) {
                    Text("新建")
                }
            },
        )

        errorMessage?.let { message ->
            PlaylistErrorCard(message = message, onDismiss = onClearError)
        }

        if (isWorking) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = "正在保存歌单…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (playlists.isEmpty()) {
            EmptyPlaylists(onCreate = { showCreateDialog = true })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = playlists,
                    key = UserPlaylist::id,
                ) { playlist ->
                    val songs = remember(librarySongs, playlist) {
                        resolvePlaylistSongs(librarySongs, playlist)
                    }
                    PlaylistCard(
                        playlist = playlist,
                        songs = songs,
                        unavailableCount =
                            remember(librarySongs, playlist) {
                                countUnavailablePlaylistSongs(librarySongs, playlist)
                            },
                        onOpen = { onOpenPlaylist(playlist.id) },
                        onPlay = { onPlayAll(songs, false) },
                        onRename = { renameTarget = playlist },
                        onDelete = { deleteTarget = playlist },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "新建歌单",
            initialName = "",
            confirmLabel = "创建",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onCreatePlaylist(name)
            },
        )
    }

    renameTarget?.let { playlist ->
        PlaylistNameDialog(
            title = "重命名歌单",
            initialName = playlist.name,
            confirmLabel = "保存",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                onRenamePlaylist(playlist.id, name)
            },
        )
    }

    deleteTarget?.let { playlist ->
        ConfirmationDialog(
            title = "删除歌单？",
            message = "将删除“${playlist.name}”及其歌曲顺序，但不会删除设备上的音乐文件。",
            confirmLabel = "删除",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeletePlaylist(playlist.id)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetail(
    playlist: UserPlaylist,
    librarySongs: List<Song>,
    currentMediaId: String?,
    errorMessage: String?,
    isWorking: Boolean,
    onBack: () -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onAddSongs: (String, Collection<String>) -> Unit,
    onRemoveSong: (String, String) -> Unit,
    onSwapSongs: (String, String, String) -> Unit,
    onClearSongs: (String) -> Unit,
    onRemoveUnavailableSongs: (String, Set<String>) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)

    var query by rememberSaveable(playlist.id) { mutableStateOf("") }
    var showAddSongs by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var showRename by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var showClearConfirmation by rememberSaveable(playlist.id) { mutableStateOf(false) }

    val playlistSongs = remember(librarySongs, playlist) {
        resolvePlaylistSongs(librarySongs, playlist)
    }
    val visibleSongs = remember(playlistSongs, query) {
        filterSongs(playlistSongs, query)
    }
    val unavailableCount = remember(librarySongs, playlist) {
        countUnavailablePlaylistSongs(librarySongs, playlist)
    }
    val availableMediaIds = remember(librarySongs) {
        librarySongs.mapTo(hashSetOf()) { it.id.toString() }
    }

    Column(modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = playlist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${playlistSongs.size} 首可用歌曲",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                TextButton(onClick = { showRename = true }, enabled = !isWorking) {
                    Text("重命名")
                }
                TextButton(onClick = { showAddSongs = true }, enabled = !isWorking) {
                    Text("加歌")
                }
            },
        )

        errorMessage?.let { message ->
            PlaylistErrorCard(message = message, onDismiss = onClearError)
        }

        PlaylistSummaryCard(
            playlist = playlist,
            songs = playlistSongs,
            unavailableCount = unavailableCount,
            enabled = !isWorking,
            onPlayAll = { onPlayAll(playlistSongs, false) },
            onShuffle = { onPlayAll(playlistSongs, true) },
            onRemoveUnavailable = {
                onRemoveUnavailableSongs(playlist.id, availableMediaIds)
            },
            onClear = { showClearConfirmation = true },
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("搜索歌单内歌曲") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(onClick = { query = "" }) {
                        Text("清除")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
        )

        if (visibleSongs.isEmpty()) {
            EmptyPlaylistDetail(
                hasStoredSongs = playlist.mediaIds.isNotEmpty(),
                hasQuery = query.isNotBlank(),
                onAddSongs = { showAddSongs = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(
                    items = visibleSongs,
                    key = Song::id,
                ) { song ->
                    val fullIndex = playlistSongs.indexOfFirst { it.id == song.id }
                    val previousSong = playlistSongs.getOrNull(fullIndex - 1)
                    val nextSong = playlistSongs.getOrNull(fullIndex + 1)
                    PlaylistSongRow(
                        song = song,
                        isPlaying = currentMediaId == song.id.toString(),
                        onPlay = { onPlaySong(song, playlistSongs) },
                        onMoveUp =
                            previousSong?.let { previous ->
                                {
                                    onSwapSongs(
                                        playlist.id,
                                        song.id.toString(),
                                        previous.id.toString(),
                                    )
                                }
                            },
                        onMoveDown =
                            nextSong?.let { next ->
                                {
                                    onSwapSongs(
                                        playlist.id,
                                        song.id.toString(),
                                        next.id.toString(),
                                    )
                                }
                            },
                        onRemove = {
                            onRemoveSong(playlist.id, song.id.toString())
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 82.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }

    if (showAddSongs) {
        SongPickerDialog(
            playlist = playlist,
            librarySongs = librarySongs,
            onAddSongs = { mediaIds -> onAddSongs(playlist.id, mediaIds) },
            onDismiss = { showAddSongs = false },
        )
    }

    if (showRename) {
        PlaylistNameDialog(
            title = "重命名歌单",
            initialName = playlist.name,
            confirmLabel = "保存",
            onDismiss = { showRename = false },
            onConfirm = { name ->
                showRename = false
                onRenamePlaylist(playlist.id, name)
            },
        )
    }

    if (showClearConfirmation) {
        ConfirmationDialog(
            title = "清空歌单？",
            message = "将移除“${playlist.name}”中的全部歌曲，但不会删除设备上的音乐文件。",
            confirmLabel = "清空",
            onDismiss = { showClearConfirmation = false },
            onConfirm = {
                showClearConfirmation = false
                onClearSongs(playlist.id)
            },
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: UserPlaylist,
    songs: List<Song>,
    unavailableCount: Int,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val representative = songs.firstOrNull()
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ArtworkImage(
                    artworkUri = representative?.albumArtUri?.let(Uri::parse),
                    fallbackUri = representative?.contentUri?.let(Uri::parse),
                    seed = playlist.id.hashCode().toLong(),
                    modifier = Modifier.size(68.dp),
                    cornerRadius = 20.dp,
                    glyphSize = 30,
                    requestSize = 136.dp,
                    contentDescription = playlist.name,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = playlistCardSummary(playlist, songs, unavailableCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onPlay, enabled = songs.isNotEmpty()) {
                    Text("播放")
                }
                TextButton(onClick = onRename) {
                    Text("重命名")
                }
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun PlaylistSummaryCard(
    playlist: UserPlaylist,
    songs: List<Song>,
    unavailableCount: Int,
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onRemoveUnavailable: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${songs.size} 首 · ${formatDuration(safeTotalDuration(songs))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "更新于 ${formatPlaylistDate(playlist.updatedAtMs)}" +
                    if (unavailableCount > 0) " · $unavailableCount 首当前不可用" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onPlayAll, enabled = enabled && songs.isNotEmpty()) {
                    Text("播放全部")
                }
                TextButton(onClick = onShuffle, enabled = enabled && songs.isNotEmpty()) {
                    Text("随机播放")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (unavailableCount > 0) {
                    TextButton(onClick = onRemoveUnavailable, enabled = enabled) {
                        Text("清理失效项")
                    }
                }
                TextButton(
                    onClick = onClear,
                    enabled = enabled && playlist.mediaIds.isNotEmpty(),
                ) {
                    Text("清空歌单")
                }
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(
    song: Song,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onPlay),
        leadingContent = {
            ArtworkImage(
                artworkUri = song.albumArtUri?.let(Uri::parse),
                fallbackUri = Uri.parse(song.contentUri),
                seed = song.id,
                modifier = Modifier.size(52.dp),
                cornerRadius = 14.dp,
                glyphSize = 22,
                requestSize = 104.dp,
                contentDescription = song.album,
            )
        },
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                color =
                    if (isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null,
                    contentPadding = PaddingValues(horizontal = 5.dp),
                ) {
                    Text("↑")
                }
                TextButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null,
                    contentPadding = PaddingValues(horizontal = 5.dp),
                ) {
                    Text("↓")
                }
                TextButton(
                    onClick = onRemove,
                    contentPadding = PaddingValues(horizontal = 5.dp),
                ) {
                    Text("移出")
                }
            }
        },
    )
}

@Composable
private fun SongPickerDialog(
    playlist: UserPlaylist,
    librarySongs: List<Song>,
    onAddSongs: (Collection<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable(playlist.id) { mutableStateOf("") }
    val storedIds = remember(playlist.mediaIds) { playlist.mediaIds.toHashSet() }
    val candidates = remember(librarySongs, storedIds, query) {
        filterSongs(
            songs = librarySongs.filter { it.id.toString() !in storedIds },
            query = query,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("向“${playlist.name}”添加歌曲") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索可添加歌曲") },
                    singleLine = true,
                )
                if (candidates.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onAddSongs(candidates.map { it.id.toString() })
                        },
                    ) {
                        Text("添加当前 ${candidates.size} 首")
                    }
                }
                if (candidates.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (query.isBlank()) "没有可添加的歌曲" else "没有匹配的歌曲",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(candidates, key = Song::id) { song ->
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
                                        song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        "${song.artist} · ${song.album}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingContent = {
                                    TextButton(
                                        onClick = {
                                            onAddSongs(listOf(song.id.toString()))
                                        },
                                    ) {
                                        Text("添加")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
    )
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable(title, initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { candidate ->
                    if (candidate.length <= 80) value = candidate
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("歌单名称") },
                supportingText = { Text("${value.length} / 80") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) {
                Text(confirmLabel)
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
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
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
private fun PlaylistErrorCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}

@Composable
private fun EmptyPlaylists(onCreate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "☷",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "还没有自定义歌单",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "创建歌单后，可以混合添加系统媒体库和 SAF 授权目录中的歌曲。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onCreate) {
                Text("创建第一个歌单")
            }
        }
    }
}

@Composable
private fun EmptyPlaylistDetail(
    hasStoredSongs: Boolean,
    hasQuery: Boolean,
    onAddSongs: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text =
                    when {
                        hasQuery -> "没有匹配的歌曲"
                        hasStoredSongs -> "歌单歌曲当前不可用"
                        else -> "这个歌单还是空的"
                    },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    when {
                        hasQuery -> "换一个关键词，或清除搜索条件。"
                        hasStoredSongs -> "重新连接对应目录或恢复媒体权限后，歌曲会重新出现。"
                        else -> "从当前音乐库中选择歌曲加入歌单。"
                    },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (!hasQuery) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onAddSongs) {
                    Text("添加歌曲")
                }
            }
        }
    }
}

private fun playlistCardSummary(
    playlist: UserPlaylist,
    songs: List<Song>,
    unavailableCount: Int,
): String =
    buildString {
        append("${songs.size} 首")
        if (songs.isNotEmpty()) append(" · ${formatDuration(safeTotalDuration(songs))}")
        if (unavailableCount > 0) append(" · $unavailableCount 首不可用")
        append("\n更新于 ${formatPlaylistDate(playlist.updatedAtMs)}")
    }

private fun safeTotalDuration(songs: List<Song>): Long =
    songs.fold(0L) { total, song ->
        val duration = song.durationMs.coerceAtLeast(0L)
        if (Long.MAX_VALUE - total < duration) Long.MAX_VALUE else total + duration
    }

private fun formatPlaylistDate(timestampMs: Long): String =
    if (timestampMs <= 0L) {
        "未知时间"
    } else {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestampMs))
    }
