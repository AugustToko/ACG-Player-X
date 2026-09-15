package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyMediaStorePlaylistRepositoryTest {
    @Test
    fun memberIdsKeepSourceOrderAndDropInvalidDuplicates() {
        val normalized =
            normalizeLegacyPlaylistMediaIds(
                listOf(7L, 0L, -4L, 7L, 2L, 2L, 9L),
            )

        assertEquals(listOf("7", "2", "9"), normalized)
    }

    @Test
    fun blankLegacyNameUsesStableFallback() {
        assertEquals("旧系统歌单 42", normalizeLegacyPlaylistName("   ", 42L))
        assertEquals("Anime Mix", normalizeLegacyPlaylistName("  Anime   Mix  ", 42L))
    }

    @Test
    fun legacySecondsConversionIsSafe() {
        assertEquals(0L, legacySecondsToMillis(-1L))
        assertEquals(12_000L, legacySecondsToMillis(12L))
        assertEquals(
            Long.MAX_VALUE / 1_000L * 1_000L,
            legacySecondsToMillis(Long.MAX_VALUE),
        )
    }
}
