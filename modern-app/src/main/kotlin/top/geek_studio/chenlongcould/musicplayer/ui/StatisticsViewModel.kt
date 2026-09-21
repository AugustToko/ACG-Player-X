package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.data.AuthorizedFolder
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsExportFormat
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportDocument
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportMode
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportPreview
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsSnapshot
import top.geek_studio.chenlongcould.musicplayer.data.buildPlaybackStatisticsImportPreview
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
    val hasLibraryMetadata: Boolean = false,
    val importPhase: StatisticsImportPhase = StatisticsImportPhase.NONE,
) {
    val hasPendingImport: Boolean get() = importPhase != StatisticsImportPhase.NONE
    val isPreparingImport: Boolean get() = importPhase == StatisticsImportPhase.READING ||
        importPhase == StatisticsImportPhase.WAITING_FOR_METADATA || importPhase == StatisticsImportPhase.MATCHING
}

class StatisticsViewModel internal constructor(
    application: Application,
    private val operations: StatisticsOperations,
) : AndroidViewModel(application) {
    // Keep the Application-only constructor used by AndroidViewModelFactory.
    constructor(application: Application) : this(application, RepositoryStatisticsOperations(application))

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
    private val preparation =
        ImportPreviewPreparation<PlaybackStatisticsImportDocument, List<Song>, PlaybackStatisticsImportPreview>()
    private var metadataJob: Job? = null
    private var readJob: Job? = null
    private var previewJob: Job? = null
    private var metadataRequest: MetadataRequest? = null
    private var importDefaultsApplied = false

    fun refreshLibraryMetadata(
        includeMediaStore: Boolean,
        authorizedFolders: List<AuthorizedFolder>,
        expectedSongCount: Int,
    ) {
        val request = MetadataRequest(
            includeMediaStore,
            authorizedFolders.sortedBy(AuthorizedFolder::uriString).toList(),
            expectedSongCount,
        )
        if (request == metadataRequest && (preparation.isLoadingMetadata || preparation.hasMetadata)) return
        startMetadata(request)
    }

    fun retryLibraryMetadata() {
        val request = metadataRequest ?: return
        if (preparation.isLoadingMetadata || _uiState.value.isWorking) return
        startMetadata(request)
    }

    private fun startMetadata(request: MetadataRequest) {
        metadataRequest = request
        val revision = preparation.beginMetadata()
        metadataJob?.cancel()
        previewJob?.cancel()
        _uiState.update { it.copy(errorMessage = null) }
        publishPreparation()
        metadataJob = viewModelScope.launch {
            try {
                val songs = operations.loadMetadata(request.includeMediaStore, request.folders)
                ensureActive()
                if (preparation.completeMetadata(revision, songs)) {
                    _uiState.update { it.copy(librarySongs = songs) }
                    preparePreviewIfReady()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                if (preparation.failMetadata(revision)) {
                    _uiState.update {
                        it.copy(errorMessage = exception.localizedMessage ?: "完整音乐库元数据读取失败，请重试")
                    }
                    publishPreparation()
                }
            }
        }
    }

    fun prepareImport(uri: Uri) {
        if (_uiState.value.isWorking) {
            _uiState.update { it.copy(errorMessage = "本地数据正在写入，请完成后重新选择导入文件") }
            return
        }
        // Accept the picker result even when the library is still loading. Read the granted
        // URI now; only the parsed, bounded document waits for a usable metadata snapshot.
        val request = preparation.beginImport()
        readJob?.cancel()
        previewJob?.cancel()
        importDefaultsApplied = false
        _uiState.update {
            it.copy(infoMessage = null, errorMessage = null, importMode = PlaybackStatisticsImportMode.MERGE)
        }
        publishPreparation()
        readJob = viewModelScope.launch {
            try {
                val document = operations.readImport(uri)
                ensureActive()
                if (preparation.completeDocument(request, document)) preparePreviewIfReady()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                if (preparation.failDocument(request)) {
                    _uiState.update {
                        it.copy(errorMessage = exception.localizedMessage ?: "收听统计导入文件读取失败")
                    }
                    publishPreparation()
                }
            }
        }
    }

    private fun preparePreviewIfReady() {
        if (_uiState.value.isWorking) {
            publishPreparation()
            return
        }
        val input = preparation.nextBuild()
        publishPreparation()
        if (input == null) return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.Default) {
                    buildPlaybackStatisticsImportPreview(document = input.document, songs = input.metadata)
                }
                ensureActive()
                if (preparation.completePreview(input, preview)) {
                    _uiState.update { it.copy(errorMessage = null) }
                    if (!importDefaultsApplied) {
                        _uiState.update { it.copy(retainUnavailable = preview.unavailableEntryCount > 0) }
                        importDefaultsApplied = true
                    }
                    publishPreparation()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                if (preparation.failPreview(input)) {
                    _uiState.update { it.copy(errorMessage = exception.localizedMessage ?: "统计导入预览生成失败") }
                    publishPreparation()
                }
            }
        }
    }

    fun setImportMode(mode: PlaybackStatisticsImportMode) {
        if (!_uiState.value.isWorking) _uiState.update { it.copy(importMode = mode) }
    }

    fun setRetainUnavailable(retain: Boolean) {
        if (!_uiState.value.isWorking) _uiState.update { it.copy(retainUnavailable = retain) }
    }

    fun cancelImport() {
        if (_uiState.value.isWorking) return
        preparation.cancelImport()
        readJob?.cancel()
        previewJob?.cancel()
        _uiState.update { it.copy(errorMessage = null) }
        publishPreparation()
    }

    fun confirmImport() {
        val current = _uiState.value
        if (current.isWorking) return
        val preview = preparation.preview
        if (preparation.phase != StatisticsImportPhase.PREVIEW || !preparation.hasMetadata || preview == null) {
            _uiState.update { it.copy(errorMessage = "请等待完整音乐库与导入预览准备完成后再确认") }
            return
        }
        val selectedStats = preview.selectedStats(current.retainUnavailable)
        if (selectedStats.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "没有可安全导入的统计项目") }
            return
        }
        // The explicit confirmation is the transaction boundary. Metadata changes before
        // this point invalidate the preview; an already submitted write uses this snapshot.
        runWrite("收听统计导入失败") {
            val importedCount = operations.importStatistics(selectedStats, current.importMode)
            val modeLabel = when (current.importMode) {
                PlaybackStatisticsImportMode.MERGE -> "幂等合并"
                PlaybackStatisticsImportMode.REPLACE -> "完全替换"
            }
            preparation.cancelImport()
            publishPreparation()
            _uiState.update { it.copy(infoMessage = "已通过${modeLabel}导入 $importedCount 项收听统计") }
        }
    }

    fun export(uri: Uri, format: PlaybackStatisticsExportFormat, snapshot: PlaybackStatisticsSnapshot) {
        if (!canStartWrite()) return
        runWrite("收听统计导出失败") {
            operations.export(uri, format, snapshot)
            _uiState.update { it.copy(infoMessage = "收听统计已导出为 ${format.name}") }
        }
    }

    fun clearListeningData(clearRecent: Boolean, clearPlaybackStats: Boolean) {
        if ((!clearRecent && !clearPlaybackStats) || !canStartWrite()) return
        runWrite("本地收听数据清理失败") {
            operations.clearListeningData(clearRecent, clearPlaybackStats)
            val message = when {
                clearRecent && clearPlaybackStats -> "最近播放与全部收听统计已清除"
                clearRecent -> "最近播放记录已清除"
                else -> "播放次数、完成度、收听时长与恢复位置已清除"
            }
            _uiState.update { it.copy(infoMessage = message) }
        }
    }

    private fun canStartWrite(): Boolean {
        val current = _uiState.value
        if (current.isWorking) return false
        if (current.hasPendingImport) {
            _uiState.update { it.copy(errorMessage = "请先确认或取消当前导入") }
            return false
        }
        return true
    }

    private fun runWrite(fallback: String, block: suspend () -> Unit) {
        // Set the guard before launch so two callbacks cannot submit duplicate writes.
        _uiState.update { it.copy(isWorking = true, infoMessage = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.localizedMessage ?: fallback) }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
                preparePreviewIfReady()
            }
        }
    }

    private fun publishPreparation() {
        _uiState.update {
            it.copy(
                isRefreshingLibraryMetadata = preparation.isLoadingMetadata,
                hasLibraryMetadata = preparation.hasMetadata,
                importPhase = preparation.phase,
                importPreview = preparation.preview,
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    override fun onCleared() {
        preparation.cancelImport()
        metadataJob?.cancel()
        readJob?.cancel()
        previewJob?.cancel()
        super.onCleared()
    }

    private data class MetadataRequest(
        val includeMediaStore: Boolean,
        val folders: List<AuthorizedFolder>,
        // A change invalidates the cached scan; equality of counts is not proof of identity.
        val expectedSongCount: Int,
    )
}
