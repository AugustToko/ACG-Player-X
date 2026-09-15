package top.geek_studio.chenlongcould.musicplayer.data

import java.util.Locale
import kotlin.math.roundToInt
import top.geek_studio.chenlongcould.musicplayer.model.Song

data class PlaybackProgressUpdate(
    val mediaId: String,
    val positionMs: Long,
    val durationMs: Long,
    val listenedDeltaMs: Long,
    val sessionListenedMs: Long,
    val completed: Boolean,
    val recordedAtMs: Long,
)

internal fun updatePlaybackProgressStats(
    current: Map<String, PlaybackStats>,
    update: PlaybackProgressUpdate,
): Map<String, PlaybackStats> {
    if (update.mediaId.isBlank()) return current

    val existing = current[update.mediaId] ?: PlaybackStats()
    val durationMs =
        update.durationMs
            .takeIf { it > 0L }
            ?: existing.durationMs
    val positionMs =
        if (durationMs > 0L) {
            update.positionMs.coerceIn(0L, durationMs)
        } else {
            update.positionMs.coerceAtLeast(0L)
        }
    val shouldStorePosition =
        !update.completed &&
            durationMs > 0L &&
            positionMs > 0L &&
            update.sessionListenedMs >= MIN_MEANINGFUL_SESSION_MS
    val completedCount =
        when {
            !update.completed -> existing.completedCount
            existing.completedCount == Int.MAX_VALUE -> Int.MAX_VALUE
            else -> existing.completedCount + 1
        }
    val recordedAtMs = update.recordedAtMs.coerceAtLeast(0L)
    val updated =
        existing.copy(
            lastPlayedAtMs =
                if (update.sessionListenedMs > 0L) {
                    maxOf(existing.lastPlayedAtMs, recordedAtMs)
                } else {
                    existing.lastPlayedAtMs
                },
            completedCount = completedCount,
            lastCompletedAtMs =
                if (update.completed) {
                    maxOf(existing.lastCompletedAtMs, recordedAtMs)
                } else {
                    existing.lastCompletedAtMs
                },
            totalListenTimeMs =
                saturatingAdd(
                    existing.totalListenTimeMs,
                    update.listenedDeltaMs,
                ),
            lastPositionMs =
                when {
                    update.completed -> 0L
                    shouldStorePosition -> positionMs
                    else -> existing.lastPositionMs
                },
            durationMs = durationMs.coerceAtLeast(0L),
        )
    return current + (update.mediaId to updated)
}

internal fun PlaybackStats.progressPercent(): Int {
    if (durationMs <= 0L || lastPositionMs <= 0L) return 0
    return ((lastPositionMs.toDouble() / durationMs.toDouble()) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
}

internal fun PlaybackStats.isInProgress(): Boolean =
    durationMs > 0L &&
        lastPositionMs >= MIN_IN_PROGRESS_POSITION_MS &&
        progressPercent() in MIN_IN_PROGRESS_PERCENT..MAX_IN_PROGRESS_PERCENT

internal fun resolveInProgressSongs(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
): List<Song> =
    songs
        .filter { song ->
            playbackStats[song.id.toString()]?.isInProgress() == true
        }
        .sortedWith(
            compareByDescending<Song> { song ->
                playbackStats[song.id.toString()]?.lastPlayedAtMs ?: 0L
            }.thenByDescending { song ->
                playbackStats[song.id.toString()]?.progressPercent() ?: 0
            }.thenBy { song ->
                song.title.lowercase(Locale.ROOT)
            }.thenBy(Song::id),
        )

internal fun resolveCompletedSongs(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
): List<Song> =
    songs
        .filter { song ->
            (playbackStats[song.id.toString()]?.completedCount ?: 0) > 0
        }
        .sortedWith(
            compareByDescending<Song> { song ->
                playbackStats[song.id.toString()]?.lastCompletedAtMs ?: 0L
            }.thenByDescending { song ->
                playbackStats[song.id.toString()]?.completedCount ?: 0
            }.thenBy { song ->
                song.title.lowercase(Locale.ROOT)
            }.thenBy(Song::id),
        )

internal fun resolveRecentlyPlayedWithin(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
    nowMs: Long,
    windowMs: Long = RECENT_PLAY_WINDOW_MS,
): List<Song> {
    val safeNowMs = nowMs.coerceAtLeast(0L)
    val cutoffMs = (safeNowMs - windowMs.coerceAtLeast(0L)).coerceAtLeast(0L)
    return songs
        .filter { song ->
            val lastPlayedAtMs = playbackStats[song.id.toString()]?.lastPlayedAtMs ?: 0L
            lastPlayedAtMs in cutoffMs..safeNowMs
        }
        .sortedWith(
            compareByDescending<Song> { song ->
                playbackStats[song.id.toString()]?.lastPlayedAtMs ?: 0L
            }.thenBy { song ->
                song.title.lowercase(Locale.ROOT)
            }.thenBy(Song::id),
        )
}

internal fun resolveLongFormSongs(
    songs: List<Song>,
    minimumDurationMs: Long = LONG_FORM_MIN_DURATION_MS,
): List<Song> =
    songs
        .filter { it.durationMs >= minimumDurationMs.coerceAtLeast(0L) }
        .sortedWith(
            compareByDescending<Song>(Song::durationMs)
                .thenBy { it.title.lowercase(Locale.ROOT) }
                .thenBy(Song::id),
        )

internal const val RECENT_PLAY_WINDOW_MS = 7L * 24L * 60L * 60L * 1_000L
internal const val LONG_FORM_MIN_DURATION_MS = 20L * 60L * 1_000L
private const val MIN_MEANINGFUL_SESSION_MS = 10_000L
private const val MIN_IN_PROGRESS_POSITION_MS = 15_000L
private const val MIN_IN_PROGRESS_PERCENT = 10
private const val MAX_IN_PROGRESS_PERCENT = 89
