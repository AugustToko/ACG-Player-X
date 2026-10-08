package top.geek_studio.chenlongcould.musicplayer.lyrics

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    @Test
    fun embeddedId3LoadsAndImportedOverrideTakesPriority() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mediaId = "lyrics-embedded-${System.nanoTime()}"
        val audio = File(context.cacheDir, "$mediaId.mp3")
        val override = File(context.cacheDir, "$mediaId-override.lrc")
        val repository = LyricsRepository(context)
        audio.writeBytes(
            instrumentedId3Tag(
                "[00:01.00]Embedded first\n[00:02.50]Embedded second",
            ),
        )
        override.writeText("[00:03.00]Imported override", Charsets.UTF_8)

        try {
            val embedded = repository.resolve(mediaId, Uri.fromFile(audio))
            assertNotNull(embedded)
            assertEquals(LyricsSource.EMBEDDED_ID3_USLT, embedded?.source)
            assertEquals("Embedded first", embedded?.parsed?.lines?.first()?.text)
            assertTrue(audio.delete())

            repository.import(mediaId, Uri.fromFile(override))
            val imported = repository.resolve(mediaId, Uri.fromFile(audio))
            assertEquals(LyricsSource.IMPORTED, imported?.source)
            assertEquals("Imported override", imported?.parsed?.lines?.single()?.text)

            repository.delete(mediaId)
            val revealed = repository.resolve(mediaId, Uri.fromFile(audio))
            assertEquals(LyricsSource.EMBEDDED_ID3_USLT, revealed?.source)
            assertTrue(revealed?.parsed?.lines?.size == 2)
        } finally {
            repository.delete(mediaId)
            audio.delete()
            override.delete()
        }
    }
}

private fun instrumentedId3Tag(lyrics: String): ByteArray {
    val payload =
        byteArrayOf(3) +
            "eng".toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(0) +
            lyrics.toByteArray(Charsets.UTF_8)
    val frame =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeBytes("USLT")
                output.writeInt(payload.size)
                output.writeShort(0)
                output.write(payload)
            }
            bytes.toByteArray()
        }
    return "ID3".toByteArray(Charsets.ISO_8859_1) +
        byteArrayOf(3, 0, 0) +
        instrumentedSyncSafe(frame.size) +
        frame
}

private fun instrumentedSyncSafe(value: Int): ByteArray =
    byteArrayOf(
        ((value ushr 21) and 0x7f).toByte(),
        ((value ushr 14) and 0x7f).toByte(),
        ((value ushr 7) and 0x7f).toByte(),
        (value and 0x7f).toByte(),
    )
