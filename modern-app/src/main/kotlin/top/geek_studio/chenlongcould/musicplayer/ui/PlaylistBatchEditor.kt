package top.geek_studio.chenlongcould.musicplayer.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.geek_studio.chenlongcould.musicplayer.data.resolvePlaylistSongs
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkImage

@Composable
fun PlaylistBatchEditorOverlay(
    state: PlaylistUiState,
    librarySongs: List<Song>,
    onMoveToStart: (String, Collection<String>) -> Unit,
    onMoveToEnd: (String, Collection<String>) -> Unit,
    onRemoveSongs: (String, Collection<String>) -> Unit,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePlaylist = state.activePlaylist
    val playlistSongs =
        remember(activePlaylist, librarySongs) {
            activePlaylist?.let { resolvePlaylistSongs(librarySongs, it) }.orEmpty()
        }
    val snackbarHostState = remember { SnackbarHostState() }

    var editorVisible by rememberSaveable(activePlaylist?.id) { mutableStateOf(false) }
    var query by rememberSaveable(activePlaylist?.id) { mutableStateOf("") }
    var selectedMediaIds by remember(activePlaylist?.id) {
        mutableStateOf(emptyList<String>())
    }

    LaunchedEffect(activePlaylist?.id) {
        editorVisible = false
        query = ""
        selectedMediaIds = emptyList()
    }

    LaunchedEffect(activePlaylist?.mediaIds) {
        val storedOrder = activePlaylist?.mediaIds.orEmpty()
        val stillStored = selectedMediaIds.toHashSet()
        selectedMediaIds = storedOrder.filter(stillStored::contains)
    }

    LaunchedEffect(state.undoToken) {
        val message = state.undoMessage ?: return@LaunchedEffect
        when (
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "撤销",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        ) {
            SnackbarResult.ActionPerformed -> onUndo()
            SnackbarResult.Dismissed -> onDismissUndo()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (activePlaylist != null && playlistSongs.isNotEmpty() && !state.isWorking) {
            SmallFloatingActionButton(
                onClick = { editorVisible = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
            ) {
                Text(
                    text = "批量",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 84.dp),
        )
    }

    if (editorVisible && activePlaylist != null) {
        val visibleSongs =
            remember(playlistSongs, query) {
                filterSongs(playlistSongs, query)
            }
        val selectedSet = selectedMediaIds.toHashSet()
        val visibleIds = visibleSongs.map { it.id.toString() }
        val visibleIdSet = visibleIds.toHashSet()
        val allVisibleSelected =
            visibleIds.isNotEmpty() && visibleIds.all(selectedSet::contains)

        fun normalizeSelection(ids: Collection<String>): List<String> {
            val requested = ids.toHashSet()
            return activePlaylist.mediaIds.filter(requested::contains)
        }

        fun toggleSelection(mediaId: String) {
            selectedMediaIds =
                if (mediaId in selectedSet) {
                    selectedMediaIds.filterNot { it == mediaId }
                } else {
                    normalizeSelection(selectedMediaIds + mediaId)
                }
        }

        fun finishBatchAction(action: () -> Unit) {
            action()
            selectedMediaIds = emptyList()
            editorVisible = false
        }

        AlertDialog(
            onDismissRequest = {
                if (!state.isWorking) {
                    editorVisible = false
                }
            },
            title = {
                Column {
                    Text(
                        text = "批量编辑“${activePlaylist.name}”",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "已选 ${selectedMediaIds.size} 首",
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
                            .heightIn(min = 340.dp, max = 620.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索歌单内歌曲") },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                TextButton(onClick = { query = "" }) {
                                    Text("清除")
                                }
                            }
                        },
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                selectedMediaIds =
                                    if (allVisibleSelected) {
                                        selectedMediaIds.filterNot(visibleIdSet::contains)
                                    } else {
                                        normalizeSelection(selectedMediaIds + visibleIds)
                                    }
                            },
                            enabled = visibleSongs.isNotEmpty() && !state.isWorking,
                        ) {
                            Text(if (allVisibleSelected) "取消当前" else "全选当前")
                        }
                        TextButton(
                            onClick = { selectedMediaIds = emptyList() },
                            enabled = selectedMediaIds.isNotEmpty() && !state.isWorking,
                        ) {
                            Text("清空选择")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(
                            onClick = {
                                finishBatchAction {
                                    onMoveToStart(activePlaylist.id, selectedMediaIds)
                                }
                            },
                            enabled = selectedMediaIds.isNotEmpty() && !state.isWorking,
                        ) {
                            Text("移到顶部")
                        }
                        TextButton(
                            onClick = {
                                finishBatchAction {
                                    onMoveToEnd(activePlaylist.id, selectedMediaIds)
                                }
                            },
                            enabled = selectedMediaIds.isNotEmpty() && !state.isWorking,
                        ) {
                            Text("移到底部")
                        }
                        TextButton(
                            onClick = {
                                finishBatchAction {
                                    onRemoveSongs(activePlaylist.id, selectedMediaIds)
                                }
                            },
                            enabled = selectedMediaIds.isNotEmpty() && !state.isWorking,
                        ) {
                            Text(
                                text = "移出",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    if (visibleSongs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text =
                                    if (query.isBlank()) {
                                        "歌单中没有当前可用歌曲"
                                    } else {
                                        "没有匹配的歌曲"
                                    },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
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
                                val selected = mediaId in selectedSet
                                ListItem(
                                    modifier = Modifier.clickable { toggleSelection(mediaId) },
                                    leadingContent = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Checkbox(
                                                checked = selected,
                                                onCheckedChange = { toggleSelection(mediaId) },
                                                enabled = !state.isWorking,
                                            )
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
                                        }
                                    },
                                    headlineContent = {
                                        Text(
                                            text = song.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight =
                                                if (selected) {
                                                    FontWeight.SemiBold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${song.artist} · ${song.album}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { editorVisible = false },
                    enabled = !state.isWorking,
                ) {
                    Text("完成")
                }
            },
        )
    }
}
