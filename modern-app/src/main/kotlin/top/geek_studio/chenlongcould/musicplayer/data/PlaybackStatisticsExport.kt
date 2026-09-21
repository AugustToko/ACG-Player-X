package top.geek_studio.chenlongcould.musicplayer.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.geek_studio.chenlongcould.musicplayer.model.Song

enum class PlaybackStatisticsExportFormat(
    val extension: String,
    val mimeType: String,
) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
}

data class PlaybackStatisticsEntry(
    val mediaId: String,
    val available: Boolean,
    val title: String,
    val artist: String,
    val album: String,
    val folderPath: String,
    val playCount: Int,
    val lastPlayedAtMs: Long,
    val completedCount: Int,
    val lastCompletedAtMs: Long,
    val totalListenTimeMs: Long,
    val lastPositionMs: Long,
    val durationMs: Long,
)

data class PlaybackStatisticsSummary(
    val trackedMediaCount: Int,
    val availableMediaCount: Int,
    val totalPlayCount: Long,
    val totalCompletedCount: Long,
    val totalListenTimeMs: Long,
)

data class PlaybackStatisticsSnapshot(
    val schemaVersion: Int,
    val generatedAtMs: Long,
    val summary: PlaybackStatisticsSummary,
    val entries: List<PlaybackStatisticsEntry>,
)

data class ListeningGroupInsight(
    val name: String,
    val songCount: Int,
    val playCount: Long,
    val completedCount: Long,
    val listenTimeMs: Long,
)

fun buildPlaybackStatisticsSnapshot(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
    generatedAtMs: Long = System.currentTimeMillis(),
): PlaybackStatisticsSnapshot {
    val songsByMediaId = songs.associateBy { it.id.toString() }
    val entries =
        playbackStats
            .map { (mediaId, stats) ->
                val song = songsByMediaId[mediaId]
                PlaybackStatisticsEntry(
                    mediaId = mediaId,
                    available = song != null,
                    title = song?.title.orEmpty(),
                    artist = song?.artist.orEmpty(),
                    album = song?.album.orEmpty(),
                    folderPath = song?.folderPath.orEmpty(),
                    playCount = stats.playCount.coerceAtLeast(0),
                    lastPlayedAtMs = stats.lastPlayedAtMs.coerceAtLeast(0L),
                    completedCount = stats.completedCount.coerceAtLeast(0),
                    lastCompletedAtMs = stats.lastCompletedAtMs.coerceAtLeast(0L),
                    totalListenTimeMs = stats.totalListenTimeMs.coerceAtLeast(0L),
                    lastPositionMs = stats.lastPositionMs.coerceAtLeast(0L),
                    durationMs = stats.durationMs.coerceAtLeast(0L),
                )
            }
            .sortedWith(
                compareByDescending<PlaybackStatisticsEntry>(PlaybackStatisticsEntry::totalListenTimeMs)
                    .thenByDescending(PlaybackStatisticsEntry::completedCount)
                    .thenByDescending(PlaybackStatisticsEntry::playCount)
                    .thenByDescending(PlaybackStatisticsEntry::lastPlayedAtMs)
                    .thenBy(PlaybackStatisticsEntry::mediaId),
            )

    return PlaybackStatisticsSnapshot(
        schemaVersion = PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION,
        generatedAtMs = generatedAtMs.coerceAtLeast(0L),
        summary =
            PlaybackStatisticsSummary(
                trackedMediaCount = entries.size,
                availableMediaCount = entries.count(PlaybackStatisticsEntry::available),
                totalPlayCount = entries.sumOf { it.playCount.toLong() },
                totalCompletedCount = entries.sumOf { it.completedCount.toLong() },
                totalListenTimeMs =
                    entries.fold(0L) { total, entry ->
                        saturatingAdd(total, entry.totalListenTimeMs)
                    },
            ),
        entries = entries,
    )
}

fun topArtistInsights(
    snapshot: PlaybackStatisticsSnapshot,
    limit: Int = 5,
): List<ListeningGroupInsight> =
    aggregateInsights(snapshot.entries, PlaybackStatisticsEntry::artist, limit)

fun topAlbumInsights(
    snapshot: PlaybackStatisticsSnapshot,
    limit: Int = 5,
): List<ListeningGroupInsight> =
    aggregateInsights(snapshot.entries, PlaybackStatisticsEntry::album, limit)

fun encodePlaybackStatistics(
    snapshot: PlaybackStatisticsSnapshot,
    format: PlaybackStatisticsExportFormat,
): String =
    when (format) {
        PlaybackStatisticsExportFormat.JSON -> snapshot.toJson()
        PlaybackStatisticsExportFormat.CSV -> snapshot.toCsv()
    }

fun playbackStatisticsFileName(
    format: PlaybackStatisticsExportFormat,
    timestampMs: Long = System.currentTimeMillis(),
): String {
    val suffix =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
            .format(Date(timestampMs.coerceAtLeast(0L)))
    return "acg-player-listening-$suffix.${format.extension}"
}

private fun aggregateInsights(
    entries: List<PlaybackStatisticsEntry>,
    nameSelector: (PlaybackStatisticsEntry) -> String,
    limit: Int,
): List<ListeningGroupInsight> =
    entries
        .asSequence()
        .filter(PlaybackStatisticsEntry::available)
        .map { it to nameSelector(it).trim() }
        .filter { (_, name) -> name.isNotEmpty() }
        .groupBy(
            keySelector = { (_, name) -> name },
            valueTransform = { (entry, _) -> entry },
        )
        .map { (name, groupEntries) ->
            ListeningGroupInsight(
                name = name,
                songCount = groupEntries.map(PlaybackStatisticsEntry::mediaId).distinct().size,
                playCount = groupEntries.sumOf { it.playCount.toLong() },
                completedCount = groupEntries.sumOf { it.completedCount.toLong() },
                listenTimeMs =
                    groupEntries.fold(0L) { total, entry ->
                        saturatingAdd(total, entry.totalListenTimeMs)
                    },
            )
        }
        .sortedWith(
            compareByDescending<ListeningGroupInsight>(ListeningGroupInsight::listenTimeMs)
                .thenByDescending(ListeningGroupInsight::completedCount)
                .thenByDescending(ListeningGroupInsight::playCount)
                .thenBy { it.name.lowercase(Locale.ROOT) },
        )
        .take(limit.coerceAtLeast(0))

private fun PlaybackStatisticsSnapshot.toJson(): String =
    buildString {
        append("{\n")
        append("  \"schemaVersion\": ").append(schemaVersion).append(",\n")
        append("  \"generatedAtMs\": ").append(generatedAtMs).append(",\n")
        append("  \"summary\": {\n")
        append("    \"trackedMediaCount\": ").append(summary.trackedMediaCount).append(",\n")
        append("    \"availableMediaCount\": ").append(summary.availableMediaCount).append(",\n")
        append("    \"totalPlayCount\": ").append(summary.totalPlayCount).append(",\n")
        append("    \"totalCompletedCount\": ").append(summary.totalCompletedCount).append(",\n")
        append("    \"totalListenTimeMs\": ").append(summary.totalListenTimeMs).append('\n')
        append("  },\n")
        append("  \"entries\": [")
        if (entries.isNotEmpty()) append('\n')
        entries.forEachIndexed { index, entry ->
            append("    {\n")
            append("      \"mediaId\": ").append(entry.mediaId.toJsonString()).append(",\n")
            append("      \"available\": ").append(entry.available).append(",\n")
            append("      \"title\": ").append(entry.title.toJsonString()).append(",\n")
            append("      \"artist\": ").append(entry.artist.toJsonString()).append(",\n")
            append("      \"album\": ").append(entry.album.toJsonString()).append(",\n")
            append("      \"folderPath\": ").append(entry.folderPath.toJsonString()).append(",\n")
            append("      \"playCount\": ").append(entry.playCount).append(",\n")
            append("      \"lastPlayedAtMs\": ").append(entry.lastPlayedAtMs).append(",\n")
            append("      \"completedCount\": ").append(entry.completedCount).append(",\n")
            append("      \"lastCompletedAtMs\": ").append(entry.lastCompletedAtMs).append(",\n")
            append("      \"totalListenTimeMs\": ").append(entry.totalListenTimeMs).append(",\n")
            append("      \"lastPositionMs\": ").append(entry.lastPositionMs).append(",\n")
            append("      \"durationMs\": ").append(entry.durationMs).append('\n')
            append("    }")
            if (index != entries.lastIndex) append(',')
            append('\n')
        }
        append("  ]\n")
        append('}')
    }

private fun PlaybackStatisticsSnapshot.toCsv(): String =
    buildString {
        append(
            listOf(
                "schemaVersion",
                "generatedAtMs",
                "mediaId",
                "available",
                "title",
                "artist",
                "album",
                "folderPath",
                "playCount",
                "lastPlayedAtMs",
                "completedCount",
                "lastCompletedAtMs",
                "totalListenTimeMs",
                "lastPositionMs",
                "durationMs",
            ).joinToString(","),
        )
        append('\n')
        entries.forEach { entry ->
            append(
                listOf(
                    schemaVersion.toString(),
                    generatedAtMs.toString(),
                    entry.mediaId,
                    entry.available.toString(),
                    entry.title,
                    entry.artist,
                    entry.album,
                    entry.folderPath,
                    entry.playCount.toString(),
                    entry.lastPlayedAtMs.toString(),
                    entry.completedCount.toString(),
                    entry.lastCompletedAtMs.toString(),
                    entry.totalListenTimeMs.toString(),
                    entry.lastPositionMs.toString(),
                    entry.durationMs.toString(),
                ).joinToString(",", transform = String::toCsvField),
            )
            append('\n')
        }
    }

private fun String.toJsonString(): String =
    buildString {
        append('"')
        this@toJsonString.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (character.code < 0x20) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
                }
            }
        }
        append('"')
    }

private fun String.toCsvField(): String {
    val requiresQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!requiresQuotes) return this
    return "\"${replace("\"", "\"\"")}\""
}

const val PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION = 1
