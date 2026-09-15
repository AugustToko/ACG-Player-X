package top.geek_studio.chenlongcould.musicplayer.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackWidgetStateTest {
    @Test
    fun widgetTextCollapsesWhitespaceAndTrims() {
        assertEquals(
            "Brave Shine Live",
            normalizeWidgetText("  Brave\nShine\tLive  ", "fallback"),
        )
    }

    @Test
    fun widgetTextUsesFallbackForBlankMetadata() {
        assertEquals("未知曲目", normalizeWidgetText("   ", "未知曲目"))
        assertEquals("未知艺术家", normalizeWidgetText(null, "未知艺术家"))
    }

    @Test
    fun defaultStateHasNoPlayableMedia() {
        val state = PlaybackWidgetState()

        assertEquals(false, state.hasMedia)
        assertEquals("尚未播放", state.title)
    }
}
