package top.geek_studio.chenlongcould.musicplayer.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PlaybackProcessRecoveryTest {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        clearTargetPackage()
        shell("pm grant $TARGET_PACKAGE $READ_MEDIA_AUDIO_PERMISSION")
        launchTargetAndWaitForLibrary()
    }

    @After
    fun tearDown() {
        shell("am force-stop $TARGET_PACKAGE")
        clearTargetPackage()
    }

    @Test
    fun forceStopRestoresQueueAndPlaybackModes() {
        val searchField =
            requireUiObject(
                selector = By.clazz(EDIT_TEXT_CLASS),
                message = "Music library search field was not visible",
            )
        searchField.click()
        searchField.text = SONG_QUERY

        requireUiObject(
            selector = By.text(SONG_TITLE),
            message = "Benchmark song was not visible after filtering",
        ).click()
        device.waitForIdle()

        openDestination("播放")
        requireUiObject(By.text(SONG_TITLE), "Now-playing metadata did not appear")

        clickText("随机")
        requireUiObject(By.text("随机✓"), "Shuffle state was not enabled")
        clickText("顺序")
        requireUiObject(By.text("循环"), "Repeat-all state was not enabled")

        Thread.sleep(PERSISTENCE_SETTLE_MS)
        shell("am force-stop $TARGET_PACKAGE")
        Thread.sleep(FORCE_STOP_SETTLE_MS)

        launchTargetAndWaitForLibrary()
        openDestination("播放")

        requireUiObject(By.text(SONG_TITLE), "Queue item was not restored after force-stop")
        requireUiObject(By.text("随机✓"), "Shuffle state was not restored after force-stop")
        requireUiObject(By.text("循环"), "Repeat mode was not restored after force-stop")
    }

    private fun launchTargetAndWaitForLibrary() {
        shell("am start -W -n $TARGET_PACKAGE/.MainActivity")
        requireUiObject(
            selector = By.textContains("$LARGE_LIBRARY_SONG_COUNT 首"),
            message = "Benchmark library did not become ready",
            timeoutMs = LARGE_LIBRARY_TIMEOUT_MS,
        )
        device.waitForIdle()
    }

    private fun openDestination(label: String) {
        requireUiObject(
            selector = By.text(label),
            message = "Navigation destination was not visible: $label",
        ).click()
        device.waitForIdle()
    }

    private fun clickText(text: String) {
        requireUiObject(By.text(text), "Control was not visible: $text").click()
        device.waitForIdle()
    }

    private fun requireUiObject(
        selector: androidx.test.uiautomator.BySelector,
        message: String,
        timeoutMs: Long = UI_TIMEOUT_MS,
    ): UiObject2 =
        device.wait(Until.findObject(selector), timeoutMs)
            ?: error(message)

    private fun clearTargetPackage() {
        shell("pm clear $TARGET_PACKAGE")
    }

    private fun shell(command: String): String = device.executeShellCommand(command)

    private companion object {
        const val READ_MEDIA_AUDIO_PERMISSION = "android.permission.READ_MEDIA_AUDIO"
        const val EDIT_TEXT_CLASS = "android.widget.EditText"
        const val SONG_QUERY = "00001"
        const val SONG_TITLE = "Benchmark Song 00001"
        const val UI_TIMEOUT_MS = 15_000L
        const val LARGE_LIBRARY_TIMEOUT_MS = 35_000L
        const val PERSISTENCE_SETTLE_MS = 2_000L
        const val FORCE_STOP_SETTLE_MS = 500L
    }
}
