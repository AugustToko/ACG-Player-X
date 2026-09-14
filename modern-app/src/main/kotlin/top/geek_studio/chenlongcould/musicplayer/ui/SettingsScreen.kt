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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.geek_studio.chenlongcould.musicplayer.BuildConfig
import top.geek_studio.chenlongcould.musicplayer.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
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
                    title = "播放核心",
                    subtitle = "Media3 ExoPlayer + MediaSessionService",
                ) {
                    StatusItem("后台播放", "已启用")
                    StatusItem("系统媒体控件", "已启用")
                    StatusItem("耳机与蓝牙控制", "由 MediaSession 接管")
                    StatusItem("音频焦点", "自动管理")
                }
            }

            item {
                SettingsCard(
                    title = "Compose 迁移状态",
                    subtitle = "新 UI 已脱离旧 Fragment 与 XML 页面栈。",
                ) {
                    StatusItem("本地音乐库", "Compose")
                    StatusItem("迷你播放器", "Compose")
                    StatusItem("全屏播放页", "Compose")
                    StatusItem("响应式导航", "Compose")
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
