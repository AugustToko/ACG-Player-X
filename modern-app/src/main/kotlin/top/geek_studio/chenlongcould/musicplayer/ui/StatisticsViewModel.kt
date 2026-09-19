package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.data.LibraryStateRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsExportFormat
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsSnapshot
import top.geek_studio.chenlongcould.musicplayer.data.encodePlaybackStatistics

data class StatisticsUiState(
    val isWorking: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

class StatisticsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val libraryStateRepository = LibraryStateRepository(application)
    private val contentResolver = application.contentResolver

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    fun export(
        uri: Uri,
        format: PlaybackStatisticsExportFormat,
        snapshot: PlaybackStatisticsSnapshot,
    ) {
        if (_uiState.value.isWorking) return

        viewModelScope.launch {
            _uiState.value = StatisticsUiState(isWorking = true)
            try {
                withContext(Dispatchers.IO) {
                    val output =
                        contentResolver.openOutputStream(uri, "wt")
                            ?: throw IOException("无法打开导出文件")
                    output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                        writer.write(encodePlaybackStatistics(snapshot, format))
                    }
                }
                _uiState.value =
                    StatisticsUiState(
                        infoMessage = "收听统计已导出为 ${format.name}",
                    )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.value =
                    StatisticsUiState(
                        errorMessage = throwable.localizedMessage ?: "收听统计导出失败",
                    )
            }
        }
    }

    fun clearListeningData(
        clearRecent: Boolean,
        clearPlaybackStats: Boolean,
    ) {
        if (_uiState.value.isWorking || (!clearRecent && !clearPlaybackStats)) return

        viewModelScope.launch {
            _uiState.value = StatisticsUiState(isWorking = true)
            try {
                libraryStateRepository.clearListeningData(
                    clearRecent = clearRecent,
                    clearPlaybackStats = clearPlaybackStats,
                )
                val message =
                    when {
                        clearRecent && clearPlaybackStats -> "最近播放与全部收听统计已清除"
                        clearRecent -> "最近播放记录已清除"
                        else -> "播放次数、完成度、收听时长与恢复位置已清除"
                    }
                _uiState.value = StatisticsUiState(infoMessage = message)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.value =
                    StatisticsUiState(
                        errorMessage = throwable.localizedMessage ?: "本地收听数据清理失败",
                    )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }
}
