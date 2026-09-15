package top.geek_studio.chenlongcould.musicplayer.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlaybackWidgetStateStoreInstrumentedTest {
    private lateinit var store: PlaybackWidgetStateStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        store = PlaybackWidgetStateStore(context)
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun stateRoundTripsAcrossStoreInstances() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        store.write(
            PlaybackWidgetState(
                mediaId = "42",
                title = "Brave Shine",
                artist = "Aimer",
                isPlaying = true,
                hasPrevious = true,
                hasNext = false,
            ),
        )

        val restored = PlaybackWidgetStateStore(context).read()

        assertEquals("42", restored.mediaId)
        assertEquals("Brave Shine", restored.title)
        assertEquals("Aimer", restored.artist)
        assertTrue(restored.isPlaying)
        assertTrue(restored.hasPrevious)
        assertFalse(restored.hasNext)
        assertTrue(restored.hasMedia)
    }

    @Test
    fun emptyStoreReturnsSafeZeroState() {
        val restored = store.read()

        assertFalse(restored.hasMedia)
        assertFalse(restored.isPlaying)
        assertEquals(PlaybackWidgetState.DEFAULT_TITLE, restored.title)
        assertEquals(PlaybackWidgetState.DEFAULT_ARTIST, restored.artist)
    }
}
