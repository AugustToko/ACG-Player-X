package top.geek_studio.chenlongcould.musicplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressTrackerTest {
    @Test
    fun contiguousPlaybackFlushesAtConfiguredInterval() {
        val tracker = PlaybackProgressTracker(flushIntervalMs = 5_000L)
        tracker.update(sample(positionMs = 0L, elapsedMs = 0L, playing = true))

        var update = tracker.update(sample(positionMs = 1_000L, elapsedMs = 1_000L, playing = true))
        assertNull(update)
        update = tracker.update(sample(positionMs = 2_000L, elapsedMs = 2_000L, playing = true))
        assertNull(update)
        update = tracker.update(sample(positionMs = 3_000L, elapsedMs = 3_000L, playing = true))
        assertNull(update)
        update = tracker.update(sample(positionMs = 4_000L, elapsedMs = 4_000L, playing = true))
        assertNull(update)
        update = tracker.update(sample(positionMs = 5_000L, elapsedMs = 5_000L, playing = true))

        assertNotNull(update)
        assertEquals(5_000L, update?.listenedDeltaMs)
        assertEquals(5_000L, update?.sessionListenedMs)
        assertFalse(update?.completed == true)
    }

    @Test
    fun forwardSeekIsNotCountedAsListeningTime() {
        val tracker = PlaybackProgressTracker(flushIntervalMs = 60_000L)
        tracker.update(sample(positionMs = 0L, elapsedMs = 0L, playing = true))
        tracker.update(sample(positionMs = 1_000L, elapsedMs = 1_000L, playing = true))
        tracker.update(sample(positionMs = 120_000L, elapsedMs = 2_000L, playing = true))
        tracker.update(sample(positionMs = 121_000L, elapsedMs = 3_000L, playing = true))

        val update = tracker.finish(recordedAtMs = 4_000L)

        assertNotNull(update)
        assertEquals(2_000L, update?.listenedDeltaMs)
        assertEquals(2_000L, update?.sessionListenedMs)
        assertFalse(update?.completed == true)
    }

    @Test
    fun pausingFlushesUnpersistedProgress() {
        val tracker = PlaybackProgressTracker(flushIntervalMs = 60_000L)
        tracker.update(sample(positionMs = 0L, elapsedMs = 0L, playing = true))
        tracker.update(sample(positionMs = 1_000L, elapsedMs = 1_000L, playing = true))
        tracker.update(sample(positionMs = 2_000L, elapsedMs = 2_000L, playing = true))

        val update = tracker.update(sample(positionMs = 3_000L, elapsedMs = 3_000L, playing = false))

        assertNotNull(update)
        assertEquals(3_000L, update?.listenedDeltaMs)
        assertFalse(update?.completed == true)
    }

    @Test
    fun mediaChangeFlushesPreviousSession() {
        val tracker = PlaybackProgressTracker(flushIntervalMs = 60_000L)
        tracker.update(sample(mediaId = "1", positionMs = 0L, elapsedMs = 0L, playing = true))
        tracker.update(sample(mediaId = "1", positionMs = 4_000L, elapsedMs = 4_000L, playing = true))

        val update =
            tracker.update(
                sample(
                    mediaId = "2",
                    positionMs = 0L,
                    elapsedMs = 5_000L,
                    playing = true,
                ),
            )

        assertNotNull(update)
        assertEquals("1", update?.mediaId)
        assertEquals(4_000L, update?.listenedDeltaMs)
    }

    @Test
    fun completionIsEmittedOncePerPlaybackCycle() {
        val tracker = PlaybackProgressTracker(flushIntervalMs = 60_000L)
        tracker.update(
            sample(
                durationMs = 20_000L,
                positionMs = 0L,
                elapsedMs = 0L,
                playing = true,
            ),
        )

        var completion =
            (1..16).firstNotNullOfOrNull { second ->
                tracker.update(
                    sample(
                        durationMs = 20_000L,
                        positionMs = second * 1_000L,
                        elapsedMs = second * 1_000L,
                        playing = true,
                    ),
                )
            }

        assertNotNull(completion)
        assertTrue(completion?.completed == true)
        assertEquals(16_000L, completion?.sessionListenedMs)

        completion =
            tracker.update(
                sample(
                    durationMs = 20_000L,
                    positionMs = 17_000L,
                    elapsedMs = 17_000L,
                    playing = true,
                ),
            )
        assertNull(completion)
    }

    @Test
    fun repeatRestartBeginsNewCycleWithoutDuplicateCompletion() {
        val tracker = PlaybackProgressTracker(flushIntervalMs = 60_000L)
        tracker.update(sample(durationMs = 20_000L, positionMs = 0L, elapsedMs = 0L, playing = true))
        var completion = null as top.geek_studio.chenlongcould.musicplayer.data.PlaybackProgressUpdate?
        for (second in 1..16) {
            completion =
                tracker.update(
                    sample(
                        durationMs = 20_000L,
                        positionMs = second * 1_000L,
                        elapsedMs = second * 1_000L,
                        playing = true,
                    ),
                ) ?: completion
        }
        assertTrue(completion?.completed == true)

        val restart =
            tracker.update(
                sample(
                    durationMs = 20_000L,
                    positionMs = 0L,
                    elapsedMs = 17_000L,
                    playing = true,
                ),
            )
        assertNull(restart)

        tracker.update(sample(durationMs = 20_000L, positionMs = 1_000L, elapsedMs = 18_000L, playing = true))
        val next = tracker.finish(recordedAtMs = 19_000L)
        assertEquals(1_000L, next?.sessionListenedMs)
        assertFalse(next?.completed == true)
    }

    @Test
    fun completionThresholdRequiresBothPositionAndMeaningfulListening() {
        assertEquals(16_000L, completionPositionThresholdMs(20_000L))
        assertEquals(10_000L, requiredCompletionListenMs(20_000L))
        assertFalse(qualifiesAsCompleted(16_000L, 20_000L, 9_999L))
        assertTrue(qualifiesAsCompleted(16_000L, 20_000L, 10_000L))
    }

    private fun sample(
        mediaId: String = "1",
        positionMs: Long,
        elapsedMs: Long,
        playing: Boolean,
        durationMs: Long = 180_000L,
    ): PlaybackProgressSample =
        PlaybackProgressSample(
            mediaId = mediaId,
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = playing,
            elapsedRealtimeMs = elapsedMs,
            wallClockMs = elapsedMs,
        )
}
