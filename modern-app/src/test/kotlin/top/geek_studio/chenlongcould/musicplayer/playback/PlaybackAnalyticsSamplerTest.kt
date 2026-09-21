package top.geek_studio.chenlongcould.musicplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.data.ListeningDataGeneration

class PlaybackAnalyticsSamplerTest {
    private val original = ListeningDataGeneration("stats-1", "recent-1")

    @Test
    fun resetDiscardsUnflushedListeningWithoutCountingAnotherStart() {
        val sampler = PlaybackAnalyticsSampler()
        assertEquals("1", sampler.sample(sample(0L), original)?.playedMediaId)
        assertNull(sampler.sample(sample(10_000L), original))
        val reset = original.copy(statistics = "stats-2")
        assertNull(sampler.sample(sample(11_000L), reset))
        val update = sampler.sample(sample(26_000L), reset)!!
        assertNull(update.playedMediaId)
        assertEquals(reset, update.generation)
        assertEquals(15_000L, update.progress!!.listenedDeltaMs)
        assertEquals(15_000L, update.progress.sessionListenedMs)
    }

    @Test
    fun recentClearPreservesStatisticsSession() {
        val sampler = PlaybackAnalyticsSampler()
        sampler.sample(sample(0L), original)
        sampler.sample(sample(10_000L), original)
        val reset = original.copy(recent = "recent-2")
        val update = sampler.sample(sample(15_000L), reset)!!
        assertNull(update.playedMediaId)
        assertEquals(15_000L, update.progress!!.sessionListenedMs)
        assertEquals(reset, update.generation)
    }

    @Test
    fun replacementDiscardsOldCompletionEvidence() {
        val sampler = PlaybackAnalyticsSampler()
        sampler.sample(sample(0L, duration = 20_000L), original)
        sampler.sample(sample(10_000L, duration = 20_000L), original)
        val reset = original.copy(statistics = "replacement")
        assertNull(sampler.sample(sample(16_000L, duration = 20_000L), reset))
        val update = sampler.sample(sample(20_000L, playing = false, duration = 20_000L), reset)!!
        assertFalse(update.progress!!.completed)
        assertEquals(4_000L, update.progress.sessionListenedMs)
    }

    @Test
    fun transitionsCountRealRepeatStartsWithCapturedTime() {
        val sampler = PlaybackAnalyticsSampler()
        val first = sampler.sample(sample(0L), original)!!
        assertEquals(100_000L, first.playedAtMs)
        assertNull(sampler.sample(sample(0L), original))
        sampler.onMediaItemTransition()
        val repeat = sampler.sample(sample(0L), original)!!
        assertEquals("1", repeat.playedMediaId)
    }

    @Test
    fun immutableBatchKeepsGenerationAcrossClear() {
        val sampler = PlaybackAnalyticsSampler()
        val batch = sampler.sample(sample(0L), original)!!
        val reset = ListeningDataGeneration("stats-2", "recent-2")
        sampler.sample(sample(1_000L), reset)
        assertEquals(original, batch.generation)
        assertFalse(reset.acceptsStatistics(batch.generation))
        assertFalse(reset.acceptsRecent(batch.generation))
    }

    @Test
    fun generationGuardsAreIndependentAndNullMeansNewImmediateEvent() {
        val statsReset = original.copy(statistics = "stats-2")
        assertFalse(statsReset.acceptsStatistics(original))
        assertTrue(statsReset.acceptsRecent(original))
        val recentReset = original.copy(recent = "recent-2")
        assertTrue(recentReset.acceptsStatistics(original))
        assertFalse(recentReset.acceptsRecent(original))
        assertTrue(statsReset.acceptsStatistics(null))
        assertTrue(recentReset.acceptsRecent(null))
    }

    @Test
    fun resetWhilePausedDoesNotInventPlayback() {
        val sampler = PlaybackAnalyticsSampler()
        assertNull(sampler.sample(sample(0L, playing = false), original))
        val reset = original.copy(statistics = "stats-2")
        assertNull(sampler.sample(sample(0L, playing = false), reset))
        assertNotNull(sampler.sample(sample(0L), reset))
    }

    private fun sample(position: Long, playing: Boolean = true, duration: Long = 180_000L) =
        PlaybackProgressSample(
            mediaId = "1",
            positionMs = position,
            durationMs = duration,
            isPlaying = playing,
            elapsedRealtimeMs = position,
            wallClockMs = 100_000L + position,
        )
}
