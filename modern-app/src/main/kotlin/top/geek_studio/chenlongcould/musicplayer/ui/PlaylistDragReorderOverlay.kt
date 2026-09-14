package top.geek_studio.chenlongcould.musicplayer.ui

import android.net.Uri
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import top.geek_studio.chenlongcould.musicplayer.data.movePlaylistMediaIdToIndex
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.ui.components.ArtworkImage

@Composable
fun PlaylistDragReorderOverlay(
    state: PlaylistUiState,
    librarySongs: List<Song>,
    onCommitOrder: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePlaylist = state.activePlaylist
    val songsById = remember(librarySongs) {
        librarySongs.associateBy { it.id.toString() }
    }

    var editorVisible by rememberSaveable(activePlaylist?.id) { mutableStateOf(false) }
    var draftMediaIds by remember(activePlaylist?.id) {
        mutableStateOf(activePlaylist?.mediaIds.orEmpty())
    }

    LaunchedEffect(activePlaylist?.id) {
        editorVisible = false
        draftMediaIds = activePlaylist?.mediaIds.orEmpty()
    }
    LaunchedEffect(activePlaylist?.mediaIds, editorVisible) {
        if (!editorVisible) {
            draftMediaIds = activePlaylist?.mediaIds.orEmpty()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (
            activePlaylist != null &&
            activePlaylist.mediaIds.size > 1 &&
            !state.isWorking
        ) {
            SmallFloatingActionButton(
                onClick = {
                    draftMediaIds = activePlaylist.mediaIds
                    editorVisible = true
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 84.dp),
            ) {
                Text(
                    text = "拖拽",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (editorVisible && activePlaylist != null) {
        PlaylistDragReorderDialog(
            playlistName = activePlaylist.name,
            originalMediaIds = activePlaylist.mediaIds,
            mediaIds = draftMediaIds,
            songsById = songsById,
            isWorking = state.isWorking,
            onMediaIdsChange = { draftMediaIds = it },
            onDismiss = { editorVisible = false },
            onConfirm = { orderedIds ->
                editorVisible = false
                onCommitOrder(activePlaylist.id, orderedIds)
            },
        )
    }
}

@Composable
private fun PlaylistDragReorderDialog(
    playlistName: String,
    originalMediaIds: List<String>,
    mediaIds: List<String>,
    songsById: Map<String, Song>,
    isWorking: Boolean,
    onMediaIdsChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val latestMediaIds by rememberUpdatedState(mediaIds)
    val edgeScrollThresholdPx = with(density) { 72.dp.toPx() }
    val edgeScrollStepPx = with(density) { 30.dp.toPx() }
    val draggedShadowPx = with(density) { 8.dp.toPx() }

    var draggedMediaId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    fun finishDrag() {
        draggedMediaId = null
        dragOffsetY = 0f
    }

    AlertDialog(
        onDismissRequest = {
            if (!isWorking) onDismiss()
        },
        title = {
            Column {
                Text(
                    text = "拖拽排序“$playlistName”",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${mediaIds.size} 个歌单项目",
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
                        .heightIn(min = 360.dp, max = 660.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "长按右侧拖动柄，然后上下移动。暂不可用项目也保留在原歌单顺序中。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

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
                            text = "正在保存顺序…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(
                        items = mediaIds,
                        key = { _, mediaId -> mediaId },
                    ) { index, mediaId ->
                        val song = songsById[mediaId]
                        val isDragging = draggedMediaId == mediaId

                        ListItem(
                            modifier =
                                Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                        shadowElevation = if (isDragging) draggedShadowPx else 0f
                                        alpha = if (isDragging) 0.96f else 1f
                                    },
                            leadingContent = {
                                if (song != null) {
                                    ArtworkImage(
                                        artworkUri = song.albumArtUri?.let(Uri::parse),
                                        fallbackUri = Uri.parse(song.contentUri),
                                        seed = song.id,
                                        modifier = Modifier.size(46.dp),
                                        cornerRadius = 12.dp,
                                        glyphSize = 18,
                                        requestSize = 92.dp,
                                        contentDescription = song.album,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(46.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "?",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = "${index + 1}. ${song?.title ?: "当前不可用项目"}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight =
                                        if (isDragging) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        },
                                )
                            },
                            supportingContent = {
                                Text(
                                    text =
                                        song?.let { "${it.artist} · ${it.album}" }
                                            ?: "媒体 ID $mediaId · 恢复来源后可继续播放",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(48.dp)
                                            .semantics {
                                                contentDescription =
                                                    "长按拖动 ${song?.title ?: "不可用项目"}"
                                            }
                                            .pointerInput(mediaId) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedMediaId = mediaId
                                                        dragOffsetY = 0f
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.LongPress,
                                                        )
                                                    },
                                                    onDragCancel = ::finishDrag,
                                                    onDragEnd = ::finishDrag,
                                                    onDrag = drag@{ change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y

                                                        val layoutInfo = listState.layoutInfo
                                                        val draggedItem =
                                                            layoutInfo.visibleItemsInfo.firstOrNull {
                                                                it.key == mediaId
                                                            } ?: return@drag
                                                        val draggedCenter =
                                                            draggedItem.offset +
                                                                draggedItem.size / 2f +
                                                                dragOffsetY

                                                        val targetItem =
                                                            layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                                                item.key != mediaId &&
                                                                    draggedCenter >= item.offset &&
                                                                    draggedCenter <= item.offset + item.size
                                                            }
                                                        val targetMediaId = targetItem?.key as? String
                                                        if (targetMediaId != null) {
                                                            val current = latestMediaIds
                                                            val targetIndex = current.indexOf(targetMediaId)
                                                            if (targetIndex >= 0) {
                                                                val reordered =
                                                                    movePlaylistMediaIdToIndex(
                                                                        current = current,
                                                                        mediaId = mediaId,
                                                                        targetIndex = targetIndex,
                                                                    )
                                                                if (reordered != current) {
                                                                    onMediaIdsChange(reordered)
                                                                    dragOffsetY = 0f
                                                                }
                                                            }
                                                        }

                                                        when {
                                                            draggedCenter <
                                                                layoutInfo.viewportStartOffset +
                                                                edgeScrollThresholdPx -> {
                                                                coroutineScope.launch {
                                                                    listState.scrollBy(-edgeScrollStepPx)
                                                                }
                                                            }

                                                            draggedCenter >
                                                                layoutInfo.viewportEndOffset -
                                                                edgeScrollThresholdPx -> {
                                                                coroutineScope.launch {
                                                                    listState.scrollBy(edgeScrollStepPx)
                                                                }
                                                            }
                                                        }
                                                    },
                                                )
                                            },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "≡",
                                        fontSize = 28.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(mediaIds) },
                enabled = !isWorking && mediaIds != originalMediaIds,
            ) {
                Text("保存顺序")
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