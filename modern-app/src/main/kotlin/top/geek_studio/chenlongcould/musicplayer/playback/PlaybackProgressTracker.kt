package top.geek_studio.chenlongcould.musicplayer.playback

import top.geek_studio.chenlongcould.musicplayer.data.PlaybackProgressUpdate

internal data class PlaybackProgressSample(
    val mediaId: String?,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val elapsedRealtimeMs: Long,
    val wallClockMs: Long,
)

internal class PlaybackProgressTracker(
    private val flushIntervalMs: Long = DEFAULT_FLUSH_INTERVAL_MS,
    private val maxSampleGapMs: Long = DEFAULT_MAX_SAMPLE_GAP_MS,
    private val positionSlackMs: Long = DEFAULT_POSITION_SLACK_MS,
) {
    private var session: Session? = null

    fun update(sample: PlaybackProgressSample): PlaybackProgressUpdate? {
        val mediaId = sample.mediaId?.trim()?.takeIf(String::isNotEmpty)
        if (mediaId == null) {
            return finish(sample.wallClockMs)
        }

        val current = session
        if (current == null || current.mediaId != mediaId) {
            val previousUpdate = current?.finish(sample.wallClockMs)
            session = Session.from(sample, mediaId)
            return previousUpdate
        }

        val durationMs = sample.durationMs.takeIf { it > 0L } ?: current.durationMs
        val positionMs = sample.positionMs.coerceAtLeast(0L)
        val restarted =
            current.lastPositionMs > positionMs + RESTART_POSITION_TOLERANCE_MS &&
                positionMs <= RESTART_POSITION_MAX_MS &&
                current.lastPositionMs >= completionPositionThresholdMs(durationMs)
        if (restarted) {
            val previousUpdate =
                when {
                    !current.completionEmitted &&
                        qualifiesAsCompleted(
                            positionMs = current.lastPositionMs,
                            durationMs = durationMs,
                            listenedMs = current.sessionListenedMs,
                        ) -> current.snapshot(completed = true, recordedAtMs = sample.wallClockMs)

                    current.unflushedListenMs > 0L ->
                        current.snapshot(completed = false, recordedAtMs = sample.wallClockMs)

                    else -> null
                }
            session = Session.from(sample, mediaId)
            return previousUpdate
        }

        if (current.completionEmitted) {
            current.lastPositionMs = positionMs
            current.durationMs = durationMs
            current.lastElapsedRealtimeMs = sample.elapsedRealtimeMs.coerceAtLeast(0L)
            current.wasPlaying = sample.isPlaying
            return null
        }

        val previousWasPlaying = current.wasPlaying
        val elapsedDeltaMs =
            (sample.elapsedRealtimeMs - current.lastElapsedRealtimeMs)
                .coerceAtLeast(0L)
        val positionDeltaMs = positionMs - current.lastPositionMs
        if (
            previousWasPlaying &&
            elapsedDeltaMs in 1..maxSampleGapMs.coerceAtLeast(1L) &&
            positionDeltaMs > 0L &&
            positionDeltaMs <= elapsedDeltaMs * MAX_PLAYBACK_SPEED_MULTIPLIER + positionSlackMs
        ) {
            current.sessionListenedMs =
                saturatingAdd(current.sessionListenedMs, positionDeltaMs)
            current.unflushedListenMs =
                saturatingAdd(current.unflushedListenMs, positionDeltaMs)
        }

        current.lastPositionMs = positionMs
        current.durationMs = durationMs
        current.lastElapsedRealtimeMs = sample.elapsedRealtimeMs.coerceAtLeast(0L)
        current.wasPlaying = sample.isPlaying

        val completed =
            qualifiesAsCompleted(
                positionMs = positionMs,
                durationMs = durationMs,
                listenedMs = current.sessionListenedMs,
            )
        val paused = previousWasPlaying && !sample.isPlaying
        val shouldFlush =
            completed ||
                current.unflushedListenMs >= flushIntervalMs.coerceAtLeast(1L) ||
                (paused && current.unflushedListenMs > 0L)
        if (!shouldFlush) return null

        return current.snapshot(
            completed = completed,
            recordedAtMs = sample.wallClockMs,
        )
    }

    fun finish(recordedAtMs: Long): PlaybackProgressUpdate? {
        val current = session ?: return null
        session = null
        return current.finish(recordedAtMs)
    }

    private data class Session(
        val mediaId: String,
        var lastPositionMs: Long,
        var durationMs: Long,
        var lastElapsedRealtimeMs: Long,
        var wasPlaying: Boolean,
        var sessionListenedMs: Long = 0L,
        var unflushedListenMs: Long = 0L,
        var completionEmitted: Boolean = false,
    ) {
        fun snapshot(
            completed: Boolean,
            recordedAtMs: Long,
        ): PlaybackProgressUpdate {
            val update =
                PlaybackProgressUpdate(
                    mediaId = mediaId,
                    positionMs = lastPositionMs,
                    durationMs = durationMs,
                    listenedDeltaMs = unflushedListenMs,
                    sessionListenedMs = sessionListenedMs,
                    completed = completed,
                    recordedAtMs = recordedAtMs.coerceAtLeast(0L),
                )
            unflushedListenMs = 0L
            if (completed) completionEmitted = true
            return update
        }

        fun finish(recordedAtMs: Long): PlaybackProgressUpdate? {
            if (completionEmitted) return null
            val completed =
                qualifiesAsCompleted(
                    positionMs = lastPositionMs,
                    durationMs = durationMs,
                    listenedMs = sessionListenedMs,
                )
            if (!completed && unflushedListenMs <= 0L) return null
            return snapshot(completed = completed, recordedAtMs = recordedAtMs)
        }

        companion object {
            fun from(
                sample: PlaybackProgressSample,
                mediaId: String,
            ): Session =
                Session(
                    mediaId = mediaId,
                    lastPositionMs = sample.positionMs.coerceAtLeast(0L),
                    durationMs = sample.durationMs.coerceAtLeast(0L),
                    lastElapsedRealtimeMs = sample.elapsedRealtimeMs.coerceAtLeast(0L),
                    wasPlaying = sample.isPlaying,
                )
        }
    }
}

internal fun completionPositionThresholdMs(durationMs: Long): Long {
    if (durationMs <= 0L) return Long.MAX_VALUE
    if (durationMs <= SHORT_MEDIA_DURATION_MS) {
        return (durationMs * SHORT_MEDIA_COMPLETION_PERCENT / 100L)
            .coerceIn(1L, durationMs)
    }
    val ninetyPercent = durationMs * STANDARD_COMPLETION_PERCENT / 100L
    val finalWindow = (durationMs - LONG_MEDIA_FINAL_WINDOW_MS).coerceAtLeast(0L)
    return maxOf(ninetyPercent, finalWindow).coerceIn(1L, durationMs)
}

internal fun requiredCompletionListenMs(durationMs: Long): Long {
    if (durationMs <= 0L) return Long.MAX_VALUE
    val minimum = minOf(MINIMUM_COMPLETION_LISTEN_MS, durationMs)
    return minOf(durationMs / 2L, MAXIMUM_COMPLETION_LISTEN_MS)
        .coerceAtLeast(minimum)
}

internal fun qualifiesAsCompleted(
    positionMs: Long,
    durationMs: Long,
    listenedMs: Long,
): Boolean =
    durationMs > 0L &&
        positionMs >= completionPositionThresholdMs(durationMs) &&
        listenedMs >= requiredCompletionListenMs(durationMs)

private fun saturatingAdd(
    left: Long,
    right: Long,
): Long {
    val safeLeft = left.coerceAtLeast(0L)
    val safeRight = right.coerceAtLeast(0L)
    return if (Long.MAX_VALUE - safeLeft < safeRight) Long.MAX_VALUE else safeLeft + safeRight
}

private const val DEFAULT_FLUSH_INTERVAL_MS = 15_000L
private const val DEFAULT_MAX_SAMPLE_GAP_MS = 30_000L
private const val DEFAULT_POSITION_SLACK_MS = 2_500L
private const val MAX_PLAYBACK_SPEED_MULTIPLIER = 3L
private const val RESTART_POSITION_TOLERANCE_MS = 3_000L
private const val RESTART_POSITION_MAX_MS = 3_000L
private const val SHORT_MEDIA_DURATION_MS = 30_000L
private const val SHORT_MEDIA_COMPLETION_PERCENT = 80L
private const val STANDARD_COMPLETION_PERCENT = 90L
private const val LONG_MEDIA_FINAL_WINDOW_MS = 5L * 60L * 1_000L
private const val MINIMUM_COMPLETION_LISTEN_MS = 5_000L
private const val MAXIMUM_COMPLETION_LISTEN_MS = 10L * 60L * 1_000L
