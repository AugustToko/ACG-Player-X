package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.geek_studio.chenlongcould.musicplayer.BuildConfig
import top.geek_studio.chenlongcould.musicplayer.data.AuthorizedFolder
import top.geek_studio.chenlongcould.musicplayer.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    notificationPermissionRequired: Boolean,
    authorizedFolders: List<AuthorizedFolder>,
    libraryWarnings: List<String>,
    authorizedFolderError: String?,
    isManagingAuthorizedFolders: Boolean,
    mediaStoreSongCount: Int,
    authorizedFolderSongCount: Int,
    favoriteSongCount: Int,
    recentSongCount: Int,
    playlistCount: Int,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onAddAuthorizedFolder: () -> Unit,
    onRemoveAuthorizedFolder: (String) -> Unit,
    onRefreshLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "设置",
                    fontWeight = FontWeight.Bold,
                )
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsCard(
                    title = "外观",
                    subtitle = "跟随系统动态配色，并保留明确的亮色与深色模式。",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "跟随系统"
                                            ThemeMode.LIGHT -> "亮色"
                                            ThemeMode.DARK -> "深色"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard(
                    title = "音乐来源",
                    subtitle = "SAF 授权目录不需要全盘存储权限，可读取 MediaStore 未索引的音乐。",
                ) {
                    StatusItem("系统媒体库", "$mediaStoreSongCount 首")
                    StatusItem("授权目录", "${authorizedFolders.size} 个 · $authorizedFolderSongCount 首")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = onAddAuthorizedFolder,
                            enabled = !isManagingAuthorizedFolders,
                        ) {
                            Text("添加目录")
                        }
                        TextButton(
                            onClick = onRefreshLibrary,
                            enabled = !isManagingAuthorizedFolders,
                        ) {
                            Text("重新扫描")
                        }
                    }

                    if (authorizedFolders.isEmpty()) {
                        Text(
                            text = "尚未添加授权目录。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        authorizedFolders.forEach { folder ->
                            AuthorizedFolderRow(
                                folder = folder,
                                enabled = !isManagingAuthorizedFolders,
                                onRemove = { onRemoveAuthorizedFolder(folder.uriString) },
                            )
                        }
                    }

                    authorizedFolderError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    libraryWarnings.forEach { warning ->
                        Text(
                            text = "注意：$warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item {
                SettingsCard(
                    title = "播放核心",
                    subtitle = "Media3 ExoPlayer + MediaSessionService",
                ) {
                    StatusItem("后台播放", "已启用")
                    StatusItem("系统媒体控件", "已启用")
                    StatusItem("耳机与蓝牙控制", "由 MediaSession 接管")
                    StatusItem("音频焦点", "自动管理")
                    StatusItem("队列与进度恢复", "已启用")
                    StatusItem("可视化播放队列", "已启用")
                    StatusItem("收藏歌曲", "$favoriteSongCount 首")
                    StatusItem("最近播放", "$recentSongCount 条")
                    StatusItem("自定义歌单", "$playlistCount 个")
                    StatusItem(
                        "播放通知权限",
                        if (notificationPermissionRequired) "未授予" else "已就绪",
                    )
                    if (notificationPermissionRequired) {
                        TextButton(onClick = onRequestNotificationPermission) {
                            Text("授予通知权限")
                        }
                    }
                }
            }

            item {
                SettingsCard(
                    title = "Compose 迁移状态",
                    subtitle = "新 UI 已脱离旧 Fragment 与 XML 页面栈。",
                ) {
                    StatusItem("本地音乐库", "Compose")
                    StatusItem("歌曲 / 专辑 / 艺术家", "已迁移")
                    StatusItem("MediaStore 文件夹浏览", "已迁移")
                    StatusItem("SAF 授权目录", "已迁移")
                    StatusItem("真实与内嵌封面", "已迁移")
                    StatusItem("媒体库自动刷新", "已迁移")
                    StatusItem("播放状态恢复", "已迁移")
                    StatusItem("同步 LRC 歌词", "已迁移")
                    StatusItem("播放队列编辑", "已迁移")
                    StatusItem("自定义歌单", "已迁移")
                    StatusItem("收藏与最近播放", "已迁移")
                    StatusItem("最近添加 / 常听 / 未播放", "已迁移")
                    StatusItem("桌面长按快捷入口", "4 个")
                    StatusItem("Live2D / 标签编辑 / 小组件", "待兼容迁移")
                }
            }

            item {
                SettingsCard(
                    title = "关于",
                    subtitle = "ACG Player X ${BuildConfig.VERSION_NAME}",
                ) {
                    Text(
                        text = "这是 2.0 Compose 重写分支。旧源码仍保留在仓库的 app/ 目录中，仅作为功能迁移参考，不参与构建。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorizedFolderRow(
    folder: AuthorizedFolder,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = folder.displayName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (folder.isAvailable) "持久读取权限有效" else "授权已失效，需要重新添加",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (folder.isAvailable) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }
            TextButton(
                onClick = onRemove,
                enabled = enabled,
            ) {
                Text("移除")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun StatusItem(
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
