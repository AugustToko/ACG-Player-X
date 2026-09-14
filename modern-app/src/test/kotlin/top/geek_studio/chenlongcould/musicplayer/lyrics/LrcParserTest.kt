package top.geek_studio.chenlongcould.musicplayer.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test
    fun `parses metadata offset and multiple timestamps`() {
        val parsed =
            parseLrc(
                """
                [ti:Example Song]
                [ar:Example Artist]
                [al:Example Album]
                [offset:+250]
                [00:01.20][00:03.456]First line
                [01:02]Second line
                """.trimIndent(),
            )

        assertEquals("Example Song", parsed.title)
        assertEquals("Example Artist", parsed.artist)
        assertEquals("Example Album", parsed.album)
        assertEquals(250L, parsed.fileOffsetMs)
        assertEquals(
            listOf(1_200L, 3_456L, 62_000L),
            parsed.lines.map(LyricLine::timestampMs),
        )
        assertEquals("First line", parsed.lines[1].text)
    }

    @Test
    fun `supports one two and three digit fractions`() {
        val parsed =
            parseLrc(
                """
                [00:01.2]one
                [00:02.34]two
                [00:03.567]three
                """.trimIndent(),
            )

        assertEquals(listOf(1_200L, 2_340L, 3_567L), parsed.lines.map(LyricLine::timestampMs))
    }

    @Test
    fun `ignores malformed timestamps and keeps deterministic order`() {
        val parsed =
            parseLrc(
                """
                [00:61.00]invalid
                [00:02.00]later
                plain text
                [00:01.00]earlier
                [00:01.00]earlier
                """.trimIndent(),
            )

        assertEquals(2, parsed.lines.size)
        assertEquals(listOf("earlier", "later"), parsed.lines.map(LyricLine::text))
    }

    @Test
    fun `finds active line with positive and negative offsets`() {
        val lines =
            listOf(
                LyricLine(1_000L, "one"),
                LyricLine(2_000L, "two"),
                LyricLine(3_000L, "three"),
            )

        assertEquals(-1, activeLyricIndex(lines, positionMs = 1_400L, totalOffsetMs = 500L))
        assertEquals(0, activeLyricIndex(lines, positionMs = 1_500L, totalOffsetMs = 500L))
        assertEquals(1, activeLyricIndex(lines, positionMs = 1_750L, totalOffsetMs = -300L))
        assertTrue(lines[1].effectiveTimestampMs(-3_000L) == 0L)
    }
}
