package top.geek_studio.chenlongcould.musicplayer.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class LibraryStatePrivacyInstrumentedTest {
    @Test
    fun clearingListeningDataPreservesFavorites() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = LibraryStateRepository(context)
        val favoriteId = "privacy-favorite-${System.nanoTime()}"
        val playedId = "privacy-played-${System.nanoTime()}"

        repository.clearListeningData(clearRecent = true, clearPlaybackStats = true)
        repository.toggleFavorite(favoriteId)
        repository.recordPlayed(playedId, playedAtMs = 1_000L)
        repository.recordProgress(
            PlaybackProgressUpdate(
                mediaId = playedId,
                positionMs = 30_000L,
                durationMs = 180_000L,
                listenedDeltaMs = 15_000L,
                sessionListenedMs = 15_000L,
                completed = false,
                recordedAtMs = 2_000L,
            ),
        )

        repository.clearListeningData(clearRecent = true, clearPlaybackStats = true)

        assertTrue(repository.recentMediaIds.first().isEmpty())
        assertTrue(repository.playbackStats.first().isEmpty())
        assertTrue(favoriteId in repository.favoriteMediaIds.first())

        repository.toggleFavorite(favoriteId)
    }
}
