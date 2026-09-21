package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun StatisticsImportPreparationStatus(
    state: StatisticsUiState,
    onCancel: () -> Unit,
    onRetryMetadata: () -> Unit,
) {
    val message = when (state.importPhase) {
        StatisticsImportPhase.READING -> "正在读取统计备份…"
        StatisticsImportPhase.WAITING_FOR_METADATA -> "文件已读取，正在等待完整音乐库元数据…"
        StatisticsImportPhase.MATCHING -> "正在匹配统计备份与当前音乐库…"
        StatisticsImportPhase.METADATA_FAILED -> "备份已保留，音乐库读取失败。重试后会重新生成预览，不会自动写入。"
        StatisticsImportPhase.FAILED -> "导入准备失败，请取消后重新选择文件。"
        else -> null
    }
    if (message != null) {
        Column(
            modifier = Modifier.testTag("statistics_import_preparation"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.isPreparingImport) CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Row {
                TextButton(
                    onClick = onCancel,
                    enabled = !state.isWorking,
                    modifier = Modifier.testTag("statistics_import_cancel"),
                ) { Text("取消导入") }
                if (state.importPhase == StatisticsImportPhase.METADATA_FAILED) {
                    TextButton(
                        onClick = onRetryMetadata,
                        enabled = !state.isWorking && !state.isRefreshingLibraryMetadata,
                        modifier = Modifier.testTag("statistics_import_retry"),
                    ) { Text("重试音乐库") }
                }
            }
        }
    } else if (!state.hasLibraryMetadata && !state.isRefreshingLibraryMetadata && state.errorMessage != null) {
        TextButton(onClick = onRetryMetadata, enabled = !state.isWorking) { Text("重试音乐库") }
    }
}
