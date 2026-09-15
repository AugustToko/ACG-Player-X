package top.geek_studio.chenlongcould.musicplayer.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "top.geek_studio.chenlongcould.musicplayer"
internal const val LARGE_LIBRARY_SONG_COUNT = 10_000
private const val UI_TIMEOUT_MS = 10_000L
private const val LARGE_LIBRARY_TIMEOUT_MS = 30_000L
private const val READ_MEDIA_AUDIO_PERMISSION = "android.permission.READ_MEDIA_AUDIO"

internal fun MacrobenchmarkScope.launchFromHome() {
    pressHome()
    startActivityAndWait()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.grantAudioPermission() {
    device.executeShellCommand("pm grant $TARGET_PACKAGE $READ_MEDIA_AUDIO_PERMISSION")
}

internal fun MacrobenchmarkScope.revokeAudioPermission() {
    device.executeShellCommand("pm revoke $TARGET_PACKAGE $READ_MEDIA_AUDIO_PERMISSION")
}

internal fun MacrobenchmarkScope.waitForLargeLibrary() {
    val expectedSummary = "$LARGE_LIBRARY_SONG_COUNT 首"
    checkNotNull(
        device.wait(
            Until.findObject(By.textContains(expectedSummary)),
            LARGE_LIBRARY_TIMEOUT_MS,
        ),
    ) { "Large benchmark library did not become visible: $expectedSummary" }
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.launchLargeLibrary() {
    grantAudioPermission()
    launchFromHome()
    waitForLargeLibrary()
}

internal fun MacrobenchmarkScope.openDestination(label: String) {
    val destination =
        device.wait(Until.findObject(By.text(label)), UI_TIMEOUT_MS)
            ?: error("Unable to find navigation destination: $label")
    destination.click()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.swipeLibraryUp(repetitions: Int = 8) {
    swipeVertically(
        startFraction = 0.82f,
        endFraction = 0.24f,
        repetitions = repetitions,
    )
}

internal fun MacrobenchmarkScope.swipeLibraryDown(repetitions: Int = 8) {
    swipeVertically(
        startFraction = 0.24f,
        endFraction = 0.82f,
        repetitions = repetitions,
    )
}

internal fun MacrobenchmarkScope.swipeSettingsUp(repetitions: Int = 3) {
    swipeVertically(
        startFraction = 0.78f,
        endFraction = 0.28f,
        repetitions = repetitions,
    )
}

internal fun MacrobenchmarkScope.swipeSettingsDown(repetitions: Int = 3) {
    swipeVertically(
        startFraction = 0.28f,
        endFraction = 0.78f,
        repetitions = repetitions,
    )
}

private fun MacrobenchmarkScope.swipeVertically(
    startFraction: Float,
    endFraction: Float,
    repetitions: Int,
) {
    val x = device.displayWidth / 2
    val startY = (device.displayHeight * startFraction).toInt()
    val endY = (device.displayHeight * endFraction).toInt()
    repeat(repetitions) {
        device.swipe(x, startY, x, endY, 24)
        device.waitForIdle()
    }
}
