package top.geek_studio.chenlongcould.musicplayer.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "top.geek_studio.chenlongcould.musicplayer"
private const val UI_TIMEOUT_MS = 10_000L

internal fun MacrobenchmarkScope.launchFromHome() {
    pressHome()
    startActivityAndWait()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.openDestination(label: String) {
    val destination =
        device.wait(Until.findObject(By.text(label)), UI_TIMEOUT_MS)
            ?: error("Unable to find navigation destination: $label")
    destination.click()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.swipeSettingsUp(repetitions: Int = 3) {
    val x = device.displayWidth / 2
    val startY = (device.displayHeight * 0.78f).toInt()
    val endY = (device.displayHeight * 0.28f).toInt()
    repeat(repetitions) {
        device.swipe(x, startY, x, endY, 24)
        device.waitForIdle()
    }
}

internal fun MacrobenchmarkScope.swipeSettingsDown(repetitions: Int = 3) {
    val x = device.displayWidth / 2
    val startY = (device.displayHeight * 0.28f).toInt()
    val endY = (device.displayHeight * 0.78f).toInt()
    repeat(repetitions) {
        device.swipe(x, startY, x, endY, 24)
        device.waitForIdle()
    }
}
