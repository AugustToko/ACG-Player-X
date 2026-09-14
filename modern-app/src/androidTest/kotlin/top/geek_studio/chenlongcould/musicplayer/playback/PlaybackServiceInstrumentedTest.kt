package top.geek_studio.chenlongcould.musicplayer.playback

import android.content.ComponentName
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlaybackServiceInstrumentedTest {
    @Test
    fun mediaControllerConnectsToSessionAndStartsPaused() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java),
            )
        val controllerFuture =
            MediaController.Builder(context, sessionToken)
                .buildAsync()
        val controller = controllerFuture.get(CONTROLLER_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        try {
            assertFalse(controller.isPlaying)
            assertTrue(controller.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))
        } finally {
            controller.release()
        }
    }

    private companion object {
        const val CONTROLLER_TIMEOUT_SECONDS = 15L
    }
}
