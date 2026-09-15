package top.geek_studio.chenlongcould.musicplayer.lyrics

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class LyricsRepositoryInstrumentedTest {
    @Test
    fun utf8ImportLoadOffsetAndDeleteRoundTrip() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mediaId = "lyrics-${System.nanoTime()}"
        val source = File(context.cacheDir, "$mediaId.lrc")
        val repository = LyricsRepository(context)
        source.writeText(
            """
            [ti:Instrumented]
            [ar:ACG Player X]
            [00:01.00]First line
            [00:02.50]Second line
            """.trimIndent(),
            Charsets.UTF_8,
        )

        try {
            val imported = repository.import(mediaId, Uri.fromFile(source))
            assertEquals("Instrumented", imported.title)
            assertEquals("ACG Player X", imported.artist)
            assertEquals(listOf(1_000L, 2_500L), imported.lines.map(LyricLine::timestampMs))

            repository.setUserOffsetMs(mediaId, 4_500L)
            val reloadedRepository = LyricsRepository(context)
            val reloaded = reloadedRepository.load(mediaId)
            assertNotNull(reloaded)
            assertEquals(imported, reloaded)
            assertEquals(4_500L, reloadedRepository.getUserOffsetMs(mediaId))

            reloadedRepository.delete(mediaId)
            assertNull(reloadedRepository.load(mediaId))
            assertEquals(0L, reloadedRepository.getUserOffsetMs(mediaId))
        } finally {
            repository.delete(mediaId)
            source.delete()
        }
    }

    @Test
    fun gb18030ImportFallsBackFromStrictUtf8() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mediaId = "lyrics-gb-${System.nanoTime()}"
        val source = File(context.cacheDir, "$mediaId.lrc")
        val repository = LyricsRepository(context)
        source.writeBytes(
            "[00:00.50]你好，世界".toByteArray(Charset.forName("GB18030")),
        )

        try {
            val imported = repository.import(mediaId, Uri.fromFile(source))
            assertEquals(500L, imported.lines.single().timestampMs)
            assertEquals("你好，世界", imported.lines.single().text)
        } finally {
            repository.delete(mediaId)
            source.delete()
        }
    }
}
