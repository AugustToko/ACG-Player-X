package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.data.AuthorizedFolder
import top.geek_studio.chenlongcould.musicplayer.data.LibraryStateRepository
import top.geek_studio.chenlongcould.musicplayer.data.MusicRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsExportFormat
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportMode
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportPreview
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsSnapshot
import top.geek_studio.chenlongcould.musicplayer.data.buildPlaybackStatisticsImportPreview
import top.geek_studio.chenlongcould.musicplayer.data.encodePlaybackStatistics
import top.geek_studio.chenlongcould.musicplayer.model.Song

data class StatisticsUiState(
    val isWorking: Boolean = false,
    val isRefreshingLibraryMetadata: Boolean = false,
    val librarySongs: List<Song> = emptyList(),
    val importPreview: PlaybackStatisticsImportPreview? = null,
    val importMode: PlaybackStatisticsImportMode = PlaybackStatisticsImportMode.MERGE,
    val retainUnavailable: Boolean = true,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

class StatisticsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val libraryStateRepository = LibraryStateRepository(application)
    private val musicRepository = MusicRepository(application)
    private val importRepository = PlaybackStatisticsImportRepository(application)
    private val contentResolver = application.contentResolver

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var metadataJob: Job? = null
    private var metadataSignature: String? = null

    fun refreshLibraryMetadata(
        includeMediaStore: Boolean,
        authorizedFolders: List<AuthorizedFolder>,
        expectedSongCount: Int,
    ) {
        val signature =
            buildString {
                append(includeMediaStore)
                append('|')
                append(expectedSongCount)
                authorizedFolders
                    .sortedBy(AuthorizedFolder::uriString)
                    .forEach { folder ->
                        append('|')
                        append(folder.uriString)
                        append(':')
                        append(folder.isAvailable)
                    }
            }
        val current = _uiState.value
        if (
            signature == metadataSignature &&
            !current.isRefreshingLibraryMetadata &&
            (expectedSongCount == 0 || current.librarySongs.size == expectedSongCount)
        ) {
            return
        }

        metadataSignature = signature
        metadataJob?.cancel()
        metadataJob =
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshingLibraryMetadata = true) }
                try {
                    val songs =
                        if (!includeMediaStore && authorizedFolders.none(AuthorizedFolder::isAvailable)) {
                            emptyList()
                        } else {
                            musicRepository.loadSongs(
                                includeMediaStore = includeMediaStore,
                                authorizedFolders = authorizedFolders,
                                refreshAuthorizedFolders = false,
                            ).songs
                        }
                    _uiState.update {
                        it.copy(
                            isRefreshingLibraryMetadata = false,
                            librarySongs = songs,
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _uiState.update {
                        it.copy(
                            isRefreshingLibraryMetadata = false,
                            errorMessage = throwable.localizedMessage ?: "完整音乐库元数据读取失败",
                        )
                    }
                }
            }
    }

    fun export(
        uri: Uri,
        format: PlaybackStatisticsExportFormat,
        snapshot: PlaybackStatisticsSnapshot,
    ) {
        if (_uiState.value.isWorking) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isWorking = true,
                    infoMessage = null,
                    errorMessage = null,
                )
            }
            try {
                withContext(Dispatchers.IO) {
                    val output =
                        contentResolver.openOutputStream(uri, "wt")
                            ?: throw IOException("无法打开导出文件")
                    output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                        writer.write(encodePlaybackStatistics(snapshot, format))
                    }
                }
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        infoMessage = "收听统计已导出为 ${format.name}",
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        errorMessage = throwable.localizedMessage ?: "收听统计导出失败",
                    )
                }
            }
        }
    }

    fun prepareImport(uri: Uri) {
        if (_uiState.value.isWorking || _uiState.value.isRefreshingLibraryMetadata) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isWorking = true,
                    importPreview = null,
                    infoMessage = null,
                    errorMessage = null,
                )
            }
            try {
                val document = importRepository.read(uri)
                val preview =
                    buildPlaybackStatisticsImportPreview(
                        document = document,
                        songs = _uiState.value.librarySongs,
                    )
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        importPreview = preview,
                        importMode = PlaybackStatisticsImportMode.MERGE,
                        retainUnavailable = preview.unavailableEntryCount > 0,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        errorMessage = throwable.localizedMessage ?: "收听统计导入文件读取失败",
                    )
                }
            }
        }
    }

    fun setImportMode(mode: PlaybackStatisticsImportMode) {
        _uiState.update { it.copy(importMode = mode) }
    }

    fun setRetainUnavailable(retain: Boolean) {
        _uiState.update { it.copy(retainUnavailable = retain) }
    }

    fun cancelImport() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(importPreview = null) }
    }

    fun confirmImport() {
        val current = _uiState.value
        val preview = current.importPreview ?: return
        if (current.isWorking) return
        val selectedStats = preview.selectedStats(current.retainUnavailable)
        if (selectedStats.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "没有可安全导入的统计项目") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isWorking = true,
                    infoMessage = null,
                    errorMessage = null,
                )
            }
            try {
                val importedCount =
                    libraryStateRepository.importPlaybackStatistics(
                        imported = selectedStats,
                        mode = current.importMode,
                    )
                val modeLabel =
                    when (current.importMode) {
                        PlaybackStatisticsImportMode.MERGE -> "幂等合并"
                        PlaybackStatisticsImportMode.REPLACE -> "完全替换"
                    }
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        importPreview = null,
                        infoMessage = "已通过${modeLabel}导入 $importedCount 项收听统计",
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        errorMessage = throwable.localizedMessage ?: "收听统计导入失败",
                    )
                }
            }
        }
    }

    fun clearListeningData(
        clearRecent: Boolean,
        clearPlaybackStats: Boolean,
    ) {
        if (_uiState.value.isWorking || (!clearRecent && !clearPlaybackStats)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isWorking = true,
                    infoMessage = null,
                    errorMessage = null,
                )
            }
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
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        infoMessage = message,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        errorMessage = throwable.localizedMessage ?: "本地收听数据清理失败",
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    override fun onCleared() {
        metadataJob?.cancel()
        super.onCleared()
    }
}
