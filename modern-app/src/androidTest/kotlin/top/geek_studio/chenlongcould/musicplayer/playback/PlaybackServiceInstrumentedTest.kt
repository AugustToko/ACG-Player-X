package top.geek_studio.chenlongcould.musicplayer.playback

import android.content.ComponentName
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import top.geek_studio.chenlongcould.musicplayer.widget.PlaybackWidgetState
import top.geek_studio.chenlongcould.musicplayer.widget.PlaybackWidgetStateStore

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlaybackServiceInstrumentedTest {
    @Test
    fun mediaControllerConnectsToSessionAndStartsPaused() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val controller = connectController()

        instrumentation.runOnMainSync {
            assertFalse(controller.isPlaying)
            assertTrue(controller.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))
            controller.clearMediaItems()
            controller.release()
        }
    }

    @Test
    fun mediaSessionPublishesCurrentMetadataToWidgetStore() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val store = PlaybackWidgetStateStore(context)
        val controller = connectController()
        store.clear()

        try {
            instrumentation.runOnMainSync {
                controller.clearMediaItems()
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(WIDGET_MEDIA_ID)
                        .setUri(Uri.parse("content://test/audio/$WIDGET_MEDIA_ID"))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Brave Shine")
                                .setArtist("Aimer")
                                .build(),
                        )
                        .build(),
                )
            }

            val state = waitForWidgetState(store) { it.mediaId == WIDGET_MEDIA_ID }

            assertEquals(WIDGET_MEDIA_ID, state.mediaId)
            assertEquals("Brave Shine", state.title)
            assertEquals("Aimer", state.artist)
            assertFalse(state.isPlaying)
            assertTrue(state.hasMedia)
        } finally {
            instrumentation.runOnMainSync {
                controller.clearMediaItems()
                controller.release()
            }
            store.clear()
        }
    }

    private fun connectController(): MediaController {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java),
            )
        return MediaController.Builder(context, sessionToken)
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()
            .get(CONTROLLER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun waitForWidgetState(
        store: PlaybackWidgetStateStore,
        predicate: (PlaybackWidgetState) -> Boolean,
    ): PlaybackWidgetState {
        val deadline = SystemClock.elapsedRealtime() + WIDGET_STATE_TIMEOUT_MS
        var state = store.read()
        while (!predicate(state) && SystemClock.elapsedRealtime() < deadline) {
            Thread.sleep(WIDGET_STATE_POLL_MS)
            state = store.read()
        }
        assertTrue("Widget state was not published before timeout: $state", predicate(state))
        return state
    }

    private companion object {
        const val CONTROLLER_TIMEOUT_SECONDS = 15L
        const val WIDGET_STATE_TIMEOUT_MS = 8_000L
        const val WIDGET_STATE_POLL_MS = 100L
        const val WIDGET_MEDIA_ID = "widget-test"
    }
}
