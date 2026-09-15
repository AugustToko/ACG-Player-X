package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreDateTest {
    @Test
    fun dateResolutionPrefersDateAddedAndFallsBackToModified() {
        assertEquals(3_000L, resolveMediaStoreDateMs(3L, 5L))
        assertEquals(7_000L, resolveMediaStoreDateMs(7L, 0L))
        assertEquals(5_000L, resolveMediaStoreDateMs(0L, 5L))
    }

    @Test
    fun dateResolutionRejectsMissingAndOverflowingValues() {
        assertEquals(0L, resolveMediaStoreDateMs(0L, -1L))
        assertEquals(0L, resolveMediaStoreDateMs(Long.MAX_VALUE, 0L))
    }
}
