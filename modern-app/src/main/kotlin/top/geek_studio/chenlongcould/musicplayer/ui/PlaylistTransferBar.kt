package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch
import top.geek_studio.chenlongcould.musicplayer.data.LegacyMediaStorePlaylist
import top.geek_studio.chenlongcould.musicplayer.data.LegacyMediaStorePlaylistRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistRepository
import top.geek_studio.chenlongcould.musicplayer.data.UserPlaylist

@Composable
fun PlaylistTransferBar(
    state: PlaylistUiState,
    onImport: () -> Unit,
    onExport: (UserPlaylist) -> Unit,
    onDismissInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val legacyRepository =
        remember(context.applicationContext) {
            LegacyMediaStorePlaylistRepository(context.applicationContext)
        }
    val playlistRepository =
        remember(context.applicationContext) {
            PlaylistRepository(context.applicationContext)
        }

    var showExportPicker by rememberSaveable { mutableStateOf(false) }
    var showLegacyPicker by rememberSaveable { mutableStateOf(false) }
    var legacyWorking by rememberSaveable { mutableStateOf(false) }
    var legacyMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var legacyError by rememberSaveable { mutableStateOf<String?>(null) }
    var legacyPlaylists by remember { mutableStateOf<List<LegacyMediaStorePlaylist>>(emptyList()) }

    val activePlaylist = state.activePlaylist
    val directExport = activePlaylist ?: state.playlists.singleOrNull()
    val working = state.isWorking || legacyWorking
    val visibleInfoMessage = legacyMessage ?: state.infoMessage

    fun discoverLegacyPlaylists() {
        scope.launch {
            legacyWorking = true
            legacyMessage = null
            legacyError = null
            showLegacyPicker = false
            try {
                val discovered = legacyRepository.loadPlaylists()
                legacyPlaylists = discovered
                if (discovered.isEmpty()) {
                    legacyMessage =
                        "未发现旧 MediaStore 歌单；Android 新版本已弃用该系统表，可继续使用 M3U/M3U8 导入"
                } else {
                    showLegacyPicker = true
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                legacyError = throwable.localizedMessage ?: "读取旧系统歌单失败"
            } finally {
                legacyWorking = false
            }
        }
    }

    fun importLegacyPlaylist(legacy: LegacyMediaStorePlaylist) {
        scope.launch {
            legacyWorking = true
            legacyMessage = null
            legacyError = null
            try {
                require(legacy.mediaIds.isNotEmpty()) { "该旧系统歌单没有可导入的媒体项目" }
                val created =
                    playlistRepository.createImportedPlaylist(
                        name = legacy.name,
                        mediaIds = legacy.mediaIds,
                    )
                showLegacyPicker = false
                legacyMessage =
                    "已只读导入“${created.name}”${legacy.mediaIds.size} 项；原系统歌单未被修改"
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                legacyError = throwable.localizedMessage ?: "导入旧系统歌单失败"
            } finally {
                legacyWorking = false
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "歌单迁移",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "M3U8 便携相对路径；旧 MediaStore 歌单仅做只读导入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = onImport,
                    enabled = !working,
                ) {
                    Text("M3U")
                }
                TextButton(
                    onClick = ::discoverLegacyPlaylists,
                    enabled = !working,
                ) {
                    Text(if (legacyWorking) "读取中" else "旧歌单")
                }
                TextButton(
                    onClick = {
                        if (directExport != null) {
                            onExport(directExport)
                        } else {
                            showExportPicker = true
                        }
                    },
                    enabled = !working && state.playlists.isNotEmpty(),
                ) {
                    Text("导出")
                }
            }
        }

        visibleInfoMessage?.let { message ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    TextButton(
                        onClick = {
                            legacyMessage = null
                            onDismissInfo()
                        },
                    ) {
                        Text("关闭")
                    }
                }
            }
        }

        legacyError?.let { message ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    TextButton(onClick = { legacyError = null }) {
                        Text("关闭")
                    }
                }
            }
        }
    }

    if (showExportPicker) {
        AlertDialog(
            onDismissRequest = { showExportPicker = false },
            title = { Text("选择要导出的歌单") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items = state.playlists, key = UserPlaylist::id) { playlist ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${playlist.mediaIds.size} 项",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        showExportPicker = false
                                        onExport(playlist)
                                    },
                                ) {
                                    Text("导出")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportPicker = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showLegacyPicker) {
        AlertDialog(
            onDismissRequest = {
                if (!legacyWorking) showLegacyPicker = false
            },
            title = { Text("导入旧系统歌单") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text =
                            "仅从已弃用的 MediaStore Playlist 表读取名称、顺序和音频 ID；不会修改或删除系统数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items = legacyPlaylists, key = LegacyMediaStorePlaylist::id) { playlist ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${playlist.mediaIds.size} 项 · 只读来源",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(
                                        onClick = { importLegacyPlaylist(playlist) },
                                        enabled = !legacyWorking && playlist.mediaIds.isNotEmpty(),
                                    ) {
                                        Text("导入")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLegacyPicker = false },
                    enabled = !legacyWorking,
                ) {
                    Text("关闭")
                }
            },
        )
    }
}
