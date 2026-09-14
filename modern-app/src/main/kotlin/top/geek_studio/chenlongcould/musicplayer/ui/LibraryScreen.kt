package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkPlaceholder
import top.geek_studio.chenlongcould.musicplayer.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: MainUiState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSectionChange: (LibrarySection) -> Unit,
    onPlaySong: (Song) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
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
                        text = "${state.totalSongCount} 首本地音乐",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = onRefresh,
                    enabled = state.hasAudioPermission && !state.isLoading,
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

        when {
            !state.permissionChecked -> {
                LoadingState(message = "正在检查媒体权限…")
            }

            !state.hasAudioPermission -> {
                PermissionState(onRequestPermission = onRequestPermission)
            }

            state.isLoading -> {
                LoadingState(message = "正在读取本地音乐库…")
            }

            state.errorMessage != null -> {
                ErrorState(
                    message = state.errorMessage,
                    onRetry = onRefresh,
                )
            }

            state.songs.isEmpty() -> {
                EmptyLibraryState(hasQuery = state.query.isNotBlank())
            }

            else -> {
                LibraryContent(
                    state = state,
                    onPlaySong = onPlaySong,
                    onOpenAlbum = onOpenAlbum,
                    onOpenArtist = onOpenArtist,
                )
            }
        }
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
            Text("搜索歌曲、艺术家或专辑")
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
                            LibrarySection.ALBUMS -> "专辑"
                            LibrarySection.ARTISTS -> "艺术家"
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
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    when (state.section) {
        LibrarySection.SONGS -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(
                    items = state.songs,
                    key = { it.id },
                ) { song ->
                    SongRow(
                        song = song,
                        isPlaying = state.playback.mediaId == song.id.toString(),
                        onClick = { onPlaySong(song) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }

        LibrarySection.ALBUMS -> {
            val albums by remember(state.songs) {
                derivedStateOf {
                    state.songs
                        .groupBy(Song::album)
                        .map { (name, songs) ->
                            CollectionSummary(
                                name = name,
                                subtitle = "${songs.size} 首 • ${songs.firstOrNull()?.artist.orEmpty()}",
                                seed = songs.firstOrNull()?.id ?: 0L,
                            )
                        }
                        .sortedBy { it.name.lowercase() }
                }
            }

            CollectionList(
                collections = albums,
                onClick = { onOpenAlbum(it.name) },
            )
        }

        LibrarySection.ARTISTS -> {
            val artists by remember(state.songs) {
                derivedStateOf {
                    state.songs
                        .groupBy(Song::artist)
                        .map { (name, songs) ->
                            CollectionSummary(
                                name = name,
                                subtitle = "${songs.size} 首 • ${songs.map(Song::album).distinct().size} 张专辑",
                                seed = songs.firstOrNull()?.id ?: 0L,
                            )
                        }
                        .sortedBy { it.name.lowercase() }
                }
            }

            CollectionList(
                collections = artists,
                onClick = { onOpenArtist(it.name) },
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            ArtworkPlaceholder(
                seed = song.id,
                modifier = Modifier.size(54.dp),
                cornerRadius = 15.dp,
                glyphSize = 24,
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
                text = "${song.artist} • ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            if (isPlaying) {
                AssistChip(
                    onClick = onClick,
                    label = { Text("播放中") },
                )
            }
        },
    )
}

private data class CollectionSummary(
    val name: String,
    val subtitle: String,
    val seed: Long,
)

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
            key = { it.name },
        ) { collection ->
            ListItem(
                modifier = Modifier.clickable { onClick(collection) },
                leadingContent = {
                    ArtworkPlaceholder(
                        seed = collection.seed,
                        modifier = Modifier.size(64.dp),
                        cornerRadius = 18.dp,
                        glyphSize = 28,
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
private fun PermissionState(onRequestPermission: () -> Unit) {
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
            text = "允许访问设备上的音乐",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ACG Player X 只读取音频媒体索引，用于展示和播放本地歌曲。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onRequestPermission) {
            Text("授予音乐权限")
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
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyLibraryState(hasQuery: Boolean) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (hasQuery) "没有匹配的歌曲" else "本地音乐库为空",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                if (hasQuery) {
                    "换一个关键词试试。"
                } else {
                    "向设备添加音乐后点击刷新。"
                },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
