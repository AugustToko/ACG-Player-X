package top.geek_studio.chenlongcould.musicplayer.ui

/** Preparation is independent of the final, explicitly confirmed write. */
enum class StatisticsImportPhase {
    NONE,
    READING,
    WAITING_FOR_METADATA,
    MATCHING,
    PREVIEW,
    METADATA_FAILED,
    FAILED,
}

internal data class ImportPreviewInput<D : Any, M : Any>(
    val requestId: Long,
    val metadataRevision: Long,
    val document: D,
    val metadata: M,
)

/**
 * Main-thread-confined rendezvous of a selected document and a library snapshot.
 * Cancellation is not the correctness boundary: generation checks also reject late results.
 * A failed scan is distinct from a successful scan of an empty library.
 */
internal class ImportPreviewPreparation<D : Any, M : Any, P : Any> {
    private var requestId = 0L
    private var metadataRevision = 0L
    private var active = false
    private var document: D? = null
    private var metadata: M? = null
    private var metadataFailed = false
    private var importFailed = false
    private var building: ImportPreviewInput<D, M>? = null

    var isLoadingMetadata: Boolean = false
        private set
    var preview: P? = null
        private set

    val hasMetadata: Boolean
        get() = metadata != null && !isLoadingMetadata && !metadataFailed

    val phase: StatisticsImportPhase
        get() = when {
            !active -> StatisticsImportPhase.NONE
            importFailed -> StatisticsImportPhase.FAILED
            document == null -> StatisticsImportPhase.READING
            metadataFailed -> StatisticsImportPhase.METADATA_FAILED
            !hasMetadata -> StatisticsImportPhase.WAITING_FOR_METADATA
            preview != null -> StatisticsImportPhase.PREVIEW
            else -> StatisticsImportPhase.MATCHING
        }

    fun beginMetadata(): Long {
        metadataRevision += 1L
        metadata = null
        metadataFailed = false
        isLoadingMetadata = true
        invalidatePreview()
        return metadataRevision
    }

    fun completeMetadata(revision: Long, value: M): Boolean {
        if (revision != metadataRevision || !isLoadingMetadata) return false
        metadata = value
        isLoadingMetadata = false
        return true
    }

    fun failMetadata(revision: Long): Boolean {
        if (revision != metadataRevision || !isLoadingMetadata) return false
        metadata = null
        metadataFailed = true
        isLoadingMetadata = false
        invalidatePreview()
        return true
    }

    fun beginImport(): Long {
        cancelImport()
        active = true
        return requestId
    }

    fun completeDocument(id: Long, value: D): Boolean {
        if (!active || id != requestId || document != null || importFailed) return false
        document = value
        return true
    }

    fun failDocument(id: Long): Boolean {
        if (!active || id != requestId || document != null) return false
        importFailed = true
        invalidatePreview()
        return true
    }

    /** Returns each ready request/revision pair once, even if both inputs notify repeatedly. */
    fun nextBuild(): ImportPreviewInput<D, M>? {
        val readyDocument = document ?: return null
        val readyMetadata = metadata ?: return null
        if (!active || importFailed || !hasMetadata || preview != null || building != null) return null
        return ImportPreviewInput(requestId, metadataRevision, readyDocument, readyMetadata)
            .also { building = it }
    }

    fun completePreview(input: ImportPreviewInput<D, M>, value: P): Boolean {
        if (!isCurrent(input)) return false
        preview = value
        building = null
        return true
    }

    fun failPreview(input: ImportPreviewInput<D, M>): Boolean {
        if (!isCurrent(input)) return false
        importFailed = true
        invalidatePreview()
        return true
    }

    fun cancelImport() {
        requestId += 1L
        active = false
        document = null
        importFailed = false
        invalidatePreview()
    }

    private fun isCurrent(input: ImportPreviewInput<D, M>): Boolean =
        active && !importFailed && hasMetadata && building === input &&
            input.requestId == requestId && input.metadataRevision == metadataRevision

    private fun invalidatePreview() {
        preview = null
        building = null
    }
}
