package top.geek_studio.chenlongcould.musicplayer.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlaybackStateStoreInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetBeforeTest() {
        PlaybackStateStore(context).clear()
    }

    @After
    fun resetAfterTest() {
        PlaybackStateStore(context).clear()
    }

    @Test
    fun queuePositionAndModesSurviveNewStoreInstance() {
        val originalStore = PlaybackStateStore(context)
        assertTrue(
            originalStore.saveQueue(
                PlaybackQueueSnapshot(
                    items =
                        listOf(
                            queueItem("101", "First"),
                            queueItem("202", "Second"),
                        ),
                    currentIndex = 0,
                    positionMs = 1_000L,
                    repeatMode = Player.REPEAT_MODE_OFF,
                    shuffleEnabled = false,
                ),
            ),
        )
        assertTrue(
            originalStore.savePosition(
                PlaybackPositionSnapshot(
                    currentIndex = 1,
                    positionMs = 42_500L,
                    repeatMode = Player.REPEAT_MODE_ALL,
                    shuffleEnabled = true,
                ),
            ),
        )

        val restored = requireNotNull(PlaybackStateStore(context).restore())
        assertEquals(listOf("101", "202"), restored.mediaItems.map { it.mediaId })
        assertEquals("Second", restored.mediaItems[1].mediaMetadata.title?.toString())
        assertEquals(1, restored.currentIndex)
        assertEquals(42_500L, restored.positionMs)
        assertEquals(Player.REPEAT_MODE_ALL, restored.repeatMode)
        assertTrue(restored.shuffleEnabled)
    }

    @Test
    fun corruptQueueIsDiscardedInsteadOfCrashingNextLaunch() {
        val preferences =
            context.getSharedPreferences(PLAYBACK_PREFERENCES_NAME, Context.MODE_PRIVATE)
        assertTrue(
            preferences.edit()
                .putString(QUEUE_KEY, "{not-valid-json")
                .commit(),
        )

        assertNull(PlaybackStateStore(context).restore())
        assertFalse(preferences.contains(QUEUE_KEY))
    }

    private fun queueItem(
        mediaId: String,
        title: String,
    ): PersistedQueueItem =
        PersistedQueueItem(
            mediaId = mediaId,
            contentUri = "content://test/audio/$mediaId",
            title = title,
            artist = "Artist $mediaId",
            album = "Album",
            artworkUri = null,
            folderPath = "Music/Test",
        )

    private companion object {
        const val PLAYBACK_PREFERENCES_NAME = "playback_restore_state"
        const val QUEUE_KEY = "queue"
    }
}
