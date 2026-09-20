package top.geek_studio.chenlongcould.musicplayer.data

import java.util.Locale
import kotlin.math.abs
import top.geek_studio.chenlongcould.musicplayer.model.Song

enum class PlaybackStatisticsImportMode {
    MERGE,
    REPLACE,
}

data class PlaybackStatisticsImportDocument(
    val schemaVersion: Int,
    val generatedAtMs: Long,
    val entries: List<PlaybackStatisticsEntry>,
    val duplicateSourceEntryCount: Int = 0,
)

data class PlaybackStatisticsImportPreview(
    val schemaVersion: Int,
    val generatedAtMs: Long,
    val sourceEntryCount: Int,
    val exactMatchCount: Int,
    val portableMatchCount: Int,
    val unavailableEntryCount: Int,
    val ambiguousEntryCount: Int,
    val skippedEntryCount: Int,
    val duplicateSourceEntryCount: Int,
    val duplicateTargetCount: Int,
    val matchedStats: Map<String, PlaybackStats>,
    val unavailableStats: Map<String, PlaybackStats>,
    val warnings: List<String> = emptyList(),
) {
    fun selectedStats(retainUnavailable: Boolean): Map<String, PlaybackStats> =
        if (retainUnavailable) {
            mergePlaybackStatisticsMaps(matchedStats, unavailableStats)
        } else {
            matchedStats
        }

    val importableEntryCount: Int
        get() = matchedStats.size + unavailableStats.size
}

fun buildPlaybackStatisticsImportPreview(
    document: PlaybackStatisticsImportDocument,
    songs: List<Song>,
): PlaybackStatisticsImportPreview {
    val songsByMediaId = songs.associateBy { it.id.toString() }
    val matchedStats = linkedMapOf<String, PlaybackStats>()
    val unavailableStats = linkedMapOf<String, PlaybackStats>()
    var exactMatchCount = 0
    var portableMatchCount = 0
    var unavailableEntryCount = 0
    var ambiguousEntryCount = 0
    var skippedEntryCount = 0
    var duplicateTargetCount = 0

    document.entries.forEach { entry ->
        val importedStats = entry.toPlaybackStats()
        val exactSong = songsByMediaId[entry.mediaId]
        val exactIdentityMatches =
            exactSong != null &&
                entry.hasPortableIdentity() &&
                exactSong.matchesPortableIdentity(entry)

        var entryWasAmbiguous = false
        val targetMediaId =
            when {
                exactIdentityMatches -> {
                    exactMatchCount += 1
                    requireNotNull(exactSong).id.toString()
                }

                entry.hasPortableIdentity() -> {
                    val candidates = songs.filter { it.matchesPortableIdentity(entry) }
                    when (candidates.size) {
                        1 -> {
                            portableMatchCount += 1
                            candidates.single().id.toString()
                        }

                        0 -> null
                        else -> {
                            ambiguousEntryCount += 1
                            entryWasAmbiguous = true
                            null
                        }
                    }
                }

                else -> null
            }

        when {
            targetMediaId != null -> {
                if (targetMediaId in matchedStats) duplicateTargetCount += 1
                matchedStats[targetMediaId] =
                    mergePlaybackStatsIdempotently(
                        matchedStats[targetMediaId] ?: PlaybackStats(),
                        importedStats,
                    )
            }

            !entry.available && exactSong == null -> {
                unavailableEntryCount += 1
                if (entry.mediaId in unavailableStats) duplicateTargetCount += 1
                unavailableStats[entry.mediaId] =
                    mergePlaybackStatsIdempotently(
                        unavailableStats[entry.mediaId] ?: PlaybackStats(),
                        importedStats,
                    )
            }

            entryWasAmbiguous -> Unit
            else -> skippedEntryCount += 1
        }
    }

    val warnings = buildList {
        if (ambiguousEntryCount > 0) {
            add("$ambiguousEntryCount 项存在多个候选，已跳过以避免错误绑定")
        }
        if (skippedEntryCount > 0) {
            add("$skippedEntryCount 项无法安全匹配到当前音乐库")
        }
        if (unavailableEntryCount > 0) {
            add("$unavailableEntryCount 项当前不可用，可选择保留原媒体 ID")
        }
        if (document.duplicateSourceEntryCount > 0 || duplicateTargetCount > 0) {
            add("重复条目已按幂等规则合并，不会重复累加次数或时长")
        }
    }

    return PlaybackStatisticsImportPreview(
        schemaVersion = document.schemaVersion,
        generatedAtMs = document.generatedAtMs,
        sourceEntryCount = document.entries.size,
        exactMatchCount = exactMatchCount,
        portableMatchCount = portableMatchCount,
        unavailableEntryCount = unavailableEntryCount,
        ambiguousEntryCount = ambiguousEntryCount,
        skippedEntryCount = skippedEntryCount,
        duplicateSourceEntryCount = document.duplicateSourceEntryCount,
        duplicateTargetCount = duplicateTargetCount,
        matchedStats = matchedStats,
        unavailableStats = unavailableStats,
        warnings = warnings,
    )
}

fun applyPlaybackStatisticsImport(
    current: Map<String, PlaybackStats>,
    imported: Map<String, PlaybackStats>,
    mode: PlaybackStatisticsImportMode,
): Map<String, PlaybackStats> =
    when (mode) {
        PlaybackStatisticsImportMode.MERGE -> mergePlaybackStatisticsMaps(current, imported)
        PlaybackStatisticsImportMode.REPLACE ->
            imported
                .filterKeys(String::isNotBlank)
                .mapValues { (_, stats) -> stats.sanitized() }
    }

internal fun mergePlaybackStatisticsMaps(
    first: Map<String, PlaybackStats>,
    second: Map<String, PlaybackStats>,
): Map<String, PlaybackStats> {
    if (first.isEmpty()) {
        return second
            .filterKeys(String::isNotBlank)
            .mapValues { (_, stats) -> stats.sanitized() }
    }
    if (second.isEmpty()) {
        return first
            .filterKeys(String::isNotBlank)
            .mapValues { (_, stats) -> stats.sanitized() }
    }

    val merged = linkedMapOf<String, PlaybackStats>()
    first.forEach { (mediaId, stats) ->
        if (mediaId.isNotBlank()) merged[mediaId] = stats.sanitized()
    }
    second.forEach { (mediaId, stats) ->
        if (mediaId.isNotBlank()) {
            merged[mediaId] =
                mergePlaybackStatsIdempotently(
                    merged[mediaId] ?: PlaybackStats(),
                    stats,
                )
        }
    }
    return merged
}

internal fun mergePlaybackStatsIdempotently(
    current: PlaybackStats,
    imported: PlaybackStats,
): PlaybackStats {
    val safeCurrent = current.sanitized()
    val safeImported = imported.sanitized()
    val currentActivityAt = maxOf(safeCurrent.lastPlayedAtMs, safeCurrent.lastCompletedAtMs)
    val importedActivityAt = maxOf(safeImported.lastPlayedAtMs, safeImported.lastCompletedAtMs)
    val durationMs = maxOf(safeCurrent.durationMs, safeImported.durationMs)
    val selectedPosition =
        when {
            importedActivityAt > currentActivityAt -> safeImported.lastPositionMs
            currentActivityAt > importedActivityAt -> safeCurrent.lastPositionMs
            safeImported.lastCompletedAtMs > safeCurrent.lastCompletedAtMs -> safeImported.lastPositionMs
            safeCurrent.lastCompletedAtMs > safeImported.lastCompletedAtMs -> safeCurrent.lastPositionMs
            else -> maxOf(safeCurrent.lastPositionMs, safeImported.lastPositionMs)
        }

    return PlaybackStats(
        playCount = maxOf(safeCurrent.playCount, safeImported.playCount),
        lastPlayedAtMs = maxOf(safeCurrent.lastPlayedAtMs, safeImported.lastPlayedAtMs),
        completedCount = maxOf(safeCurrent.completedCount, safeImported.completedCount),
        lastCompletedAtMs = maxOf(safeCurrent.lastCompletedAtMs, safeImported.lastCompletedAtMs),
        totalListenTimeMs = maxOf(safeCurrent.totalListenTimeMs, safeImported.totalListenTimeMs),
        lastPositionMs =
            if (durationMs > 0L) {
                selectedPosition.coerceIn(0L, durationMs)
            } else {
                selectedPosition.coerceAtLeast(0L)
            },
        durationMs = durationMs,
    )
}

private fun PlaybackStatisticsEntry.toPlaybackStats(): PlaybackStats =
    PlaybackStats(
        playCount = playCount,
        lastPlayedAtMs = lastPlayedAtMs,
        completedCount = completedCount,
        lastCompletedAtMs = lastCompletedAtMs,
        totalListenTimeMs = totalListenTimeMs,
        lastPositionMs = lastPositionMs,
        durationMs = durationMs,
    ).sanitized()

private fun PlaybackStats.sanitized(): PlaybackStats {
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    return copy(
        playCount = playCount.coerceAtLeast(0),
        lastPlayedAtMs = lastPlayedAtMs.coerceAtLeast(0L),
        completedCount = completedCount.coerceAtLeast(0),
        lastCompletedAtMs = lastCompletedAtMs.coerceAtLeast(0L),
        totalListenTimeMs = totalListenTimeMs.coerceAtLeast(0L),
        lastPositionMs =
            if (safeDurationMs > 0L) {
                lastPositionMs.coerceIn(0L, safeDurationMs)
            } else {
                lastPositionMs.coerceAtLeast(0L)
            },
        durationMs = safeDurationMs,
    )
}

private fun PlaybackStatisticsEntry.hasPortableIdentity(): Boolean {
    val normalizedTitle = title.normalizedIdentity()
    if (normalizedTitle.isEmpty()) return false
    return artist.normalizedIdentity().isNotEmpty() ||
        album.normalizedIdentity().isNotEmpty() ||
        durationMs > 0L
}

private fun Song.matchesPortableIdentity(entry: PlaybackStatisticsEntry): Boolean {
    if (title.normalizedIdentity() != entry.title.normalizedIdentity()) return false

    val entryArtist = entry.artist.normalizedIdentity()
    val entryAlbum = entry.album.normalizedIdentity()
    val songArtist = artist.normalizedIdentity()
    val songAlbum = album.normalizedIdentity()
    if (entryArtist.isNotEmpty() && songArtist != entryArtist) return false
    if (entryArtist.isEmpty() && entryAlbum.isNotEmpty() && songAlbum != entryAlbum) return false

    if (
        entry.durationMs > 0L &&
        durationMs > 0L &&
        abs(durationMs - entry.durationMs) > IMPORT_DURATION_TOLERANCE_MS
    ) {
        return false
    }
    return true
}

private fun String.normalizedIdentity(): String =
    trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

private const val IMPORT_DURATION_TOLERANCE_MS = 3_000L
