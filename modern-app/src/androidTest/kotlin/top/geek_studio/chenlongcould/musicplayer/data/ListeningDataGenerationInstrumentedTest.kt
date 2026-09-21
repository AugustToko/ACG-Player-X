package top.geek_studio.chenlongcould.musicplayer.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ListeningDataGenerationInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = LibraryStateRepository(context)

    @Before
    fun setUp(): Unit = runBlocking {
        repository.clearListeningData(clearRecent = true, clearPlaybackStats = true)
    }

    @After
    fun tearDown(): Unit = runBlocking {
        repository.clearListeningData(clearRecent = true, clearPlaybackStats = true)
    }

    @Test
    fun delayedWriterCannotRestoreClearedData(): Unit = runBlocking {
        val writerRepository = repository
        val generation = writerRepository.readListeningDataGeneration()
        val resumeWriter = CompletableDeferred<Unit>()
        val writer = launch {
            resumeWriter.await()
            writerRepository.recordPlayed("501", 1_000L, generation)
            writerRepository.recordProgress(progress("501", completed = true), generation)
        }
        repository.clearListeningData(clearRecent = true, clearPlaybackStats = true)
        resumeWriter.complete(Unit)
        writer.join()

        assertTrue(repository.recentMediaIds.first().isEmpty())
        assertTrue(repository.playbackStats.first().isEmpty())
    }

    @Test
    fun recentOnlyClearDoesNotDiscardStatistics(): Unit = runBlocking {
        val generation = repository.readListeningDataGeneration()
        repository.clearListeningData(clearRecent = true, clearPlaybackStats = false)
        repository.recordPlayed("502", 1_000L, generation)
        repository.recordProgress(progress("502"), generation)

        assertTrue(repository.recentMediaIds.first().isEmpty())
        val stats = repository.playbackStats.first().getValue("502")
        assertEquals(1, stats.playCount)
        assertEquals(15_000L, stats.totalListenTimeMs)
        val next = repository.readListeningDataGeneration()
        assertEquals(generation.statistics, next.statistics)
        assertNotEquals(generation.recent, next.recent)
    }

    @Test
    fun statisticsOnlyClearDoesNotDiscardRecentEvents(): Unit = runBlocking {
        val generation = repository.readListeningDataGeneration()
        repository.clearListeningData(clearRecent = false, clearPlaybackStats = true)
        repository.recordPlayed("503", 1_000L, generation)
        repository.recordProgress(progress("503", completed = true), generation)

        assertEquals(listOf("503"), repository.recentMediaIds.first())
        assertTrue(repository.playbackStats.first().isEmpty())
        val next = repository.readListeningDataGeneration()
        assertNotEquals(generation.statistics, next.statistics)
        assertEquals(generation.recent, next.recent)
    }

    @Test
    fun replacementRejectsPreImportProgressButPreservesRecent(): Unit = runBlocking {
        val generation = repository.readListeningDataGeneration()
        val restored = mapOf("504" to PlaybackStats(playCount = 7, totalListenTimeMs = 90_000L))
        repository.importPlaybackStatistics(restored, PlaybackStatisticsImportMode.REPLACE)
        repository.recordPlayed("505", 1_000L, generation)
        repository.recordProgress(progress("504", completed = true), generation)

        assertEquals(restored, repository.playbackStats.first())
        assertEquals(listOf("505"), repository.recentMediaIds.first())
    }

    @Test
    fun mergeDoesNotInvalidateCurrentSampling(): Unit = runBlocking {
        val generation = repository.readListeningDataGeneration()
        repository.importPlaybackStatistics(
            mapOf("506" to PlaybackStats(playCount = 3)),
            PlaybackStatisticsImportMode.MERGE,
        )
        repository.recordProgress(progress("506"), generation)

        assertEquals(generation, repository.readListeningDataGeneration())
        val stats = repository.playbackStats.first().getValue("506")
        assertEquals(3, stats.playCount)
        assertEquals(15_000L, stats.totalListenTimeMs)
    }

    @Test
    fun newRepositoryReadsGenerationAndAcceptsNewEvidence(): Unit = runBlocking {
        val old = repository.readListeningDataGeneration()
        repository.clearListeningData(clearRecent = true, clearPlaybackStats = true)
        val fresh = repository
        val generation = fresh.readListeningDataGeneration()
        assertNotEquals(old, generation)
        fresh.recordPlayed("507", 2_000L, generation)
        fresh.recordProgress(progress("507"), generation)

        assertEquals(listOf("507"), repository.recentMediaIds.first())
        assertEquals(1, repository.playbackStats.first().getValue("507").playCount)
        assertEquals(15_000L, repository.playbackStats.first().getValue("507").totalListenTimeMs)
    }

    @Test
    fun noOpClearDoesNotRotateGenerations(): Unit = runBlocking {
        val generation = repository.readListeningDataGeneration()
        repository.clearListeningData(clearRecent = false, clearPlaybackStats = false)
        assertEquals(generation, repository.readListeningDataGeneration())
    }

    @Test
    fun clearRecentEntryPointUsesSameInvalidation(): Unit = runBlocking {
        val generation = repository.readListeningDataGeneration()
        repository.clearRecent()
        repository.recordPlayed("508", 2_000L, generation)
        assertFalse("508" in repository.recentMediaIds.first())
        assertEquals(1, repository.playbackStats.first().getValue("508").playCount)
    }

    private fun progress(id: String, completed: Boolean = false) =
        PlaybackProgressUpdate(
            mediaId = id,
            positionMs = 45_000L,
            durationMs = 180_000L,
            listenedDeltaMs = 15_000L,
            sessionListenedMs = 15_000L,
            completed = completed,
            recordedAtMs = 2_000L,
        )
}
