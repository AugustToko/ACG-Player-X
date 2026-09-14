package top.geek_studio.chenlongcould.musicplayer.data

import top.geek_studio.chenlongcould.musicplayer.model.Song

data class PlaylistImportPreviewEntry(
    val index: Int,
    val location: String,
    val title: String? = null,
    val artist: String? = null,
    val durationSeconds: Long? = null,
    val fileNameHint: String? = null,
    val folderHint: String? = null,
    val status: PlaylistImportEntryStatus,
    val automaticMediaId: String? = null,
    val selectedMediaId: String? = null,
    val candidateMediaIds: List<String> = emptyList(),
    val candidateCount: Int = candidateMediaIds.size,
)

data class PlaylistImportPreview(
    val preferredName: String,
    val entries: List<PlaylistImportPreviewEntry>,
    val truncated: Boolean = false,
) {
    val selectedCount: Int
        get() = entries.count { it.selectedMediaId != null }

    val attentionCount: Int
        get() =
            entries.count { entry ->
                entry.selectedMediaId == null ||
                    entry.status == PlaylistImportEntryStatus.AMBIGUOUS ||
                    entry.status == PlaylistImportEntryStatus.UNMATCHED ||
                    entry.status == PlaylistImportEntryStatus.DUPLICATE
            }
}

data class PlaylistImportResult(
    val preferredName: String,
    val mediaIds: List<String>,
    val totalEntryCount: Int,
    val matchedCount: Int,
    val manuallyMappedCount: Int,
    val preservedUnavailableCount: Int,
    val unmatchedCount: Int,
    val ambiguousCount: Int,
    val duplicateCount: Int,
    val skippedCount: Int,
    val truncated: Boolean,
)

internal fun buildPlaylistImportPreview(
    parsed: ParsedM3uPlaylist,
    fallbackName: String,
    librarySongs: List<Song>,
): PlaylistImportPreview {
    val detailed = resolveM3uPlaylistDetailed(parsed, librarySongs)
    return PlaylistImportPreview(
        preferredName =
            parsed.name
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: fallbackName.trim().ifBlank { DEFAULT_IMPORTED_PLAYLIST_NAME },
        entries =
            detailed.entries.map { resolution ->
                val entry = resolution.entry
                val selectedMediaId =
                    resolution.automaticMediaId.takeIf {
                        resolution.status == PlaylistImportEntryStatus.MATCHED ||
                            resolution.status == PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE
                    }
                PlaylistImportPreviewEntry(
                    index = resolution.index,
                    location = entry.location,
                    title = entry.title,
                    artist = entry.artist,
                    durationSeconds = entry.durationSeconds,
                    fileNameHint = entry.fileNameHint,
                    folderHint = entry.folderHint,
                    status = resolution.status,
                    automaticMediaId = resolution.automaticMediaId,
                    selectedMediaId = selectedMediaId,
                    candidateMediaIds = resolution.candidateMediaIds,
                    candidateCount = resolution.candidateCount,
                )
            },
        truncated = parsed.truncated,
    )
}

internal fun updatePlaylistImportName(
    preview: PlaylistImportPreview,
    name: String,
): PlaylistImportPreview =
    preview.copy(preferredName = name.take(MAX_PLAYLIST_IMPORT_NAME_LENGTH))

internal fun updatePlaylistImportSelection(
    preview: PlaylistImportPreview,
    entryIndex: Int,
    mediaId: String?,
): PlaylistImportPreview {
    val normalizedMediaId = normalizeImportMediaId(mediaId)
    if (preview.entries.none { it.index == entryIndex }) return preview

    return preview.copy(
        entries =
            preview.entries.map { entry ->
                if (entry.index == entryIndex) {
                    entry.copy(selectedMediaId = normalizedMediaId)
                } else {
                    entry
                }
            },
    )
}

internal fun finalizePlaylistImport(preview: PlaylistImportPreview): PlaylistImportResult {
    val mediaIds = mutableListOf<String>()
    val seenMediaIds = hashSetOf<String>()
    var matchedCount = 0
    var manuallyMappedCount = 0
    var preservedUnavailableCount = 0
    var unmatchedCount = 0
    var ambiguousCount = 0
    var duplicateCount = 0
    var skippedCount = 0

    preview.entries.sortedBy(PlaylistImportPreviewEntry::index).forEach { entry ->
        val selectedMediaId = normalizeImportMediaId(entry.selectedMediaId)
        if (selectedMediaId == null) {
            when (entry.status) {
                PlaylistImportEntryStatus.AMBIGUOUS -> ambiguousCount += 1
                PlaylistImportEntryStatus.UNMATCHED -> unmatchedCount += 1
                PlaylistImportEntryStatus.DUPLICATE -> duplicateCount += 1
                PlaylistImportEntryStatus.MATCHED,
                PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE,
                -> skippedCount += 1
            }
            return@forEach
        }

        if (!seenMediaIds.add(selectedMediaId)) {
            duplicateCount += 1
            return@forEach
        }

        mediaIds += selectedMediaId
        when {
            entry.status == PlaylistImportEntryStatus.MATCHED &&
                selectedMediaId == entry.automaticMediaId -> matchedCount += 1

            entry.status == PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE &&
                selectedMediaId == entry.automaticMediaId -> preservedUnavailableCount += 1

            else -> manuallyMappedCount += 1
        }
    }

    return PlaylistImportResult(
        preferredName = preview.preferredName.trim().ifBlank { DEFAULT_IMPORTED_PLAYLIST_NAME },
        mediaIds = mediaIds,
        totalEntryCount = preview.entries.size,
        matchedCount = matchedCount,
        manuallyMappedCount = manuallyMappedCount,
        preservedUnavailableCount = preservedUnavailableCount,
        unmatchedCount = unmatchedCount,
        ambiguousCount = ambiguousCount,
        duplicateCount = duplicateCount,
        skippedCount = skippedCount,
        truncated = preview.truncated,
    )
}

private fun normalizeImportMediaId(value: String?): String? =
    value
        ?.trim()
        ?.takeIf { it.toLongOrNull() != null }

private const val MAX_PLAYLIST_IMPORT_NAME_LENGTH = 80
private const val DEFAULT_IMPORTED_PLAYLIST_NAME = "导入歌单"
