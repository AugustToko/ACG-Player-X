package top.geek_studio.chenlongcould.musicplayer.data

import java.util.Locale
import top.geek_studio.chenlongcould.musicplayer.model.Song

internal fun toggleFavoriteMediaIds(
    current: Set<String>,
    mediaId: String,
): Set<String> =
    if (mediaId in current) {
        current - mediaId
    } else {
        current + mediaId
    }

internal fun recordRecentMediaId(
    current: List<String>,
    mediaId: String,
    limit: Int,
): List<String> {
    if (mediaId.isBlank() || limit <= 0) return emptyList()

    return buildList {
        add(mediaId)
        current
            .asSequence()
            .filter { it.isNotBlank() && it != mediaId }
            .take(limit - 1)
            .forEach(::add)
    }
}

internal fun updatePlaybackStats(
    current: Map<String, PlaybackStats>,
    mediaId: String,
    playedAtMs: Long,
): Map<String, PlaybackStats> {
    if (mediaId.isBlank()) return current

    val existing = current[mediaId] ?: PlaybackStats()
    val updatedCount =
        if (existing.playCount == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            existing.playCount + 1
        }
    return current +
        (mediaId to
            existing.copy(
                playCount = updatedCount,
                lastPlayedAtMs = maxOf(existing.lastPlayedAtMs, playedAtMs.coerceAtLeast(0L)),
            ))
}

internal fun resolveRecentSongs(
    songs: List<Song>,
    recentMediaIds: List<String>,
): List<Song> {
    if (songs.isEmpty() || recentMediaIds.isEmpty()) return emptyList()

    val songsByMediaId = songs.associateBy { it.id.toString() }
    return recentMediaIds.mapNotNull(songsByMediaId::get)
}

internal fun resolveRecentlyAddedSongs(songs: List<Song>): List<Song> =
    songs.sortedWith(
        compareByDescending<Song>(Song::dateAddedMs)
            .thenBy { it.title.lowercase(Locale.ROOT) }
            .thenBy { it.artist.lowercase(Locale.ROOT) }
            .thenBy(Song::id),
    )

internal fun resolveMostPlayedSongs(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
): List<Song> =
    songs
        .filter { song ->
            (playbackStats[song.id.toString()]?.playCount ?: 0) > 0
        }
        .sortedWith(
            compareByDescending<Song> { song ->
                playbackStats[song.id.toString()]?.playCount ?: 0
            }.thenByDescending { song ->
                playbackStats[song.id.toString()]?.lastPlayedAtMs ?: 0L
            }.thenBy { song ->
                song.title.lowercase(Locale.ROOT)
            }.thenBy(Song::id),
        )

internal fun resolveUnplayedSongs(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
): List<Song> =
    resolveRecentlyAddedSongs(
        songs.filter { song ->
            (playbackStats[song.id.toString()]?.playCount ?: 0) <= 0
        },
    )

internal fun encodePlaybackStats(stats: Map<String, PlaybackStats>): String =
    stats
        .asSequence()
        .filter { (mediaId, value) ->
            mediaId.isNotBlank() &&
                (value.playCount > 0 ||
                    value.completedCount > 0 ||
                    value.totalListenTimeMs > 0L ||
                    value.lastPositionMs > 0L)
        }
        .sortedBy { (mediaId, _) -> mediaId }
        .joinToString(separator = ROW_SEPARATOR) { (mediaId, value) ->
            listOf(
                mediaId,
                value.playCount.coerceAtLeast(0).toString(),
                value.lastPlayedAtMs.coerceAtLeast(0L).toString(),
                value.completedCount.coerceAtLeast(0).toString(),
                value.lastCompletedAtMs.coerceAtLeast(0L).toString(),
                value.totalListenTimeMs.coerceAtLeast(0L).toString(),
                value.lastPositionMs.coerceAtLeast(0L).toString(),
                value.durationMs.coerceAtLeast(0L).toString(),
            ).joinToString(FIELD_SEPARATOR)
        }

internal fun decodePlaybackStats(encoded: String?): Map<String, PlaybackStats> =
    encoded
        ?.lineSequence()
        ?.mapNotNull { row ->
            val fields = row.split(FIELD_SEPARATOR)
            if (fields.size < 3) return@mapNotNull null

            val mediaId = fields[0].trim()
            val playCount = fields[1].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
            val lastPlayedAtMs = fields[2].toLongOrNull()?.coerceAtLeast(0L) ?: return@mapNotNull null
            val completedCount = fields.getOrNull(3)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val lastCompletedAtMs = fields.getOrNull(4)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val totalListenTimeMs = fields.getOrNull(5)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val lastPositionMs = fields.getOrNull(6)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val durationMs = fields.getOrNull(7)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            if (
                mediaId.isBlank() ||
                (playCount == 0 &&
                    completedCount == 0 &&
                    totalListenTimeMs == 0L &&
                    lastPositionMs == 0L)
            ) {
                return@mapNotNull null
            }
            mediaId to
                PlaybackStats(
                    playCount = playCount,
                    lastPlayedAtMs = lastPlayedAtMs,
                    completedCount = completedCount,
                    lastCompletedAtMs = lastCompletedAtMs,
                    totalListenTimeMs = totalListenTimeMs,
                    lastPositionMs = lastPositionMs,
                    durationMs = durationMs,
                )
        }
        ?.toMap()
        .orEmpty()

internal fun encodeRecentMediaIds(mediaIds: List<String>): String =
    mediaIds.joinToString(separator = RECENT_ID_SEPARATOR)

internal fun decodeRecentMediaIds(encoded: String?): List<String> =
    encoded
        ?.split(RECENT_ID_SEPARATOR)
        ?.asSequence()
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.distinct()
        ?.take(100)
        ?.toList()
        .orEmpty()

internal fun saturatingAdd(
    left: Long,
    right: Long,
): Long {
    val safeLeft = left.coerceAtLeast(0L)
    val safeRight = right.coerceAtLeast(0L)
    return if (Long.MAX_VALUE - safeLeft < safeRight) Long.MAX_VALUE else safeLeft + safeRight
}

private const val RECENT_ID_SEPARATOR = "\n"
private const val ROW_SEPARATOR = "\n"
private const val FIELD_SEPARATOR = "\t"
