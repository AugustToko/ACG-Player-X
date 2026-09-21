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
    val songIndex = PlaybackStatisticsSongIndex(songs)
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
        val identity = entry.portableIdentity()
        val exactSong = songIndex.findByMediaId(entry.mediaId)
        val exactIdentityMatches =
            exactSong != null &&
                identity.isUsable &&
                exactSong.matches(identity)

        var entryWasAmbiguous = false
        val targetMediaId =
            when {
                exactIdentityMatches -> {
                    exactMatchCount += 1
                    requireNotNull(exactSong).song.id.toString()
                }

                identity.isUsable -> {
                    when (val match = songIndex.findPortableMatch(identity)) {
                        is PortableMatch.Unique -> {
                            portableMatchCount += 1
                            match.song.id.toString()
                        }

                        PortableMatch.None -> null
                        PortableMatch.Ambiguous -> {
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

private data class PortablePlaybackIdentity(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
) {
    val isUsable: Boolean
        get() = title.isNotEmpty() && (artist.isNotEmpty() || album.isNotEmpty() || durationMs > 0L)
}

private data class IndexedPlaybackSong(
    val song: Song,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
) {
    fun matches(identity: PortablePlaybackIdentity): Boolean =
        title == identity.title &&
            (identity.artist.isEmpty() || artist == identity.artist) &&
            (identity.album.isEmpty() || album == identity.album) &&
            durationMatches(identity.durationMs)

    fun durationMatches(importedDurationMs: Long): Boolean =
        importedDurationMs <= 0L ||
            durationMs <= 0L ||
            abs(durationMs - importedDurationMs) <= IMPORT_DURATION_TOLERANCE_MS
}

private class PlaybackStatisticsSongIndex(
    songs: List<Song>,
) {
    private val byMediaId = linkedMapOf<String, IndexedPlaybackSong>()
    private val byTitle = mutableMapOf<String, MutableList<IndexedPlaybackSong>>()
    private val byTitleArtist = mutableMapOf<TitleArtistKey, MutableList<IndexedPlaybackSong>>()
    private val byTitleAlbum = mutableMapOf<TitleAlbumKey, MutableList<IndexedPlaybackSong>>()
    private val byTitleArtistAlbum =
        mutableMapOf<TitleArtistAlbumKey, MutableList<IndexedPlaybackSong>>()

    init {
        songs.forEach { song ->
            val indexed =
                IndexedPlaybackSong(
                    song = song,
                    title = song.title.normalizedIdentity(),
                    artist = song.artist.normalizedIdentity(),
                    album = song.album.normalizedIdentity(),
                    durationMs = song.durationMs,
                )
            // associateBy() used by the previous implementation kept the last item for duplicate IDs.
            byMediaId[song.id.toString()] = indexed
            if (indexed.title.isEmpty()) return@forEach

            byTitle.append(indexed.title, indexed)
            if (indexed.artist.isNotEmpty()) {
                byTitleArtist.append(
                    TitleArtistKey(indexed.title, indexed.artist),
                    indexed,
                )
            }
            if (indexed.album.isNotEmpty()) {
                byTitleAlbum.append(
                    TitleAlbumKey(indexed.title, indexed.album),
                    indexed,
                )
            }
            if (indexed.artist.isNotEmpty() && indexed.album.isNotEmpty()) {
                byTitleArtistAlbum.append(
                    TitleArtistAlbumKey(indexed.title, indexed.artist, indexed.album),
                    indexed,
                )
            }
        }
    }

    fun findByMediaId(mediaId: String): IndexedPlaybackSong? = byMediaId[mediaId]

    fun findPortableMatch(identity: PortablePlaybackIdentity): PortableMatch {
        if (!identity.isUsable) return PortableMatch.None

        val candidates: List<IndexedPlaybackSong> =
            when {
                identity.artist.isNotEmpty() && identity.album.isNotEmpty() ->
                    byTitleArtistAlbum[
                        TitleArtistAlbumKey(identity.title, identity.artist, identity.album),
                    ].orEmpty()

                identity.artist.isNotEmpty() ->
                    byTitleArtist[TitleArtistKey(identity.title, identity.artist)].orEmpty()

                identity.album.isNotEmpty() ->
                    byTitleAlbum[TitleAlbumKey(identity.title, identity.album)].orEmpty()

                else -> byTitle[identity.title].orEmpty()
            }

        var uniqueSong: Song? = null
        candidates.forEach { candidate ->
            if (!candidate.durationMatches(identity.durationMs)) return@forEach
            if (uniqueSong != null) return PortableMatch.Ambiguous
            uniqueSong = candidate.song
        }
        return uniqueSong?.let(PortableMatch::Unique) ?: PortableMatch.None
    }
}

private sealed class PortableMatch {
    data object None : PortableMatch()

    data object Ambiguous : PortableMatch()

    data class Unique(
        val song: Song,
    ) : PortableMatch()
}

private data class TitleArtistKey(
    val title: String,
    val artist: String,
)

private data class TitleAlbumKey(
    val title: String,
    val album: String,
)

private data class TitleArtistAlbumKey(
    val title: String,
    val artist: String,
    val album: String,
)

private fun <K> MutableMap<K, MutableList<IndexedPlaybackSong>>.append(
    key: K,
    song: IndexedPlaybackSong,
) {
    getOrPut(key) { mutableListOf() }.add(song)
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

private fun PlaybackStatisticsEntry.portableIdentity(): PortablePlaybackIdentity =
    PortablePlaybackIdentity(
        title = title.normalizedIdentity(),
        artist = artist.normalizedIdentity(),
        album = album.normalizedIdentity(),
        durationMs = durationMs,
    )

private fun String.normalizedIdentity(): String =
    trim()
        .replace(IDENTITY_WHITESPACE, " ")
        .lowercase(Locale.ROOT)

private val IDENTITY_WHITESPACE = Regex("\\s+")
private const val IMPORT_DURATION_TOLERANCE_MS = 3_000L
