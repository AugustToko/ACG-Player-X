package top.geek_studio.chenlongcould.musicplayer.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun settingsScrollFrameTiming() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode =
                CompilationMode.Partial(
                    baselineProfileMode = BaselineProfileMode.Require,
                ),
            iterations = 5,
            setupBlock = {
                launchFromHome()
                openDestination("设置")
            },
            measureBlock = {
                swipeSettingsUp()
                swipeSettingsDown()
            },
        )
    }
}
