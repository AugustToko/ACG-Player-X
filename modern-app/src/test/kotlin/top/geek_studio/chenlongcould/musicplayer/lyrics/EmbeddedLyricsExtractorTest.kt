package top.geek_studio.chenlongcould.musicplayer.lyrics

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedLyricsExtractorTest {
    @Test
    fun `reads UTF-16 ID3v23 USLT carrying synchronized LRC`() {
        val descriptor = byteArrayOf(0xff.toByte(), 0xfe.toByte(), 0, 0)
        val lyrics =
            "[00:01.25]第一行\n[00:03.50]第二行"
                .toByteArray(StandardCharsets.UTF_16LE)
        val payload = byteArrayOf(1) + "zho".toByteArray() + descriptor + lyrics
        val result = extract(id3v23(id3Frame("USLT", payload)))

        assertEquals(EmbeddedLyricsFormat.ID3_USLT, result?.format)
        assertEquals(listOf(1_250L, 3_500L), result?.parsed?.lines?.map(LyricLine::timestampMs))
        assertEquals(listOf("第一行", "第二行"), result?.parsed?.lines?.map(LyricLine::text))
    }

    @Test
    fun `reads lyric-labelled ID3 TXXX but ignores unrelated text fields`() {
        val unrelated =
            byteArrayOf(3) + "SOURCE".toByteArray() + byteArrayOf(0) +
                "[00:00.10]wrong".toByteArray()
        val lyrics =
            byteArrayOf(3) + "SYNCEDLYRICS".toByteArray() + byteArrayOf(0) +
                "[00:02.00]right".toByteArray()
        val result = extract(id3v23(id3Frame("TXXX", unrelated), id3Frame("TXXX", lyrics)))

        assertEquals(EmbeddedLyricsFormat.ID3_TEXT, result?.format)
        assertEquals("right", result?.parsed?.lines?.single()?.text)
    }

    @Test
    fun `converts millisecond ID3 SYLT fragments into stable lyric lines`() {
        val payload =
            ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.writeByte(3)
                    output.writeBytes("eng")
                    output.writeByte(2)
                    output.writeByte(1)
                    output.writeByte(0)
                    writeSyltEntry(output, "Hello world\n", 1_000)
                    writeSyltEntry(output, "Second line", 2_500)
                }
                bytes.toByteArray()
            }
        val result = extract(id3v24(id3v24Frame("SYLT", payload)))

        assertEquals(EmbeddedLyricsFormat.ID3_SYLT, result?.format)
        assertEquals(listOf(1_000L, 2_500L), result?.parsed?.lines?.map(LyricLine::timestampMs))
        assertEquals(listOf("Hello world", "Second line"), result?.parsed?.lines?.map(LyricLine::text))
    }

    @Test
    fun `reads synchronized FLAC Vorbis comment`() {
        val result =
            extract(
                flacWithComments(
                    "ARTIST=ACG QA",
                    "SYNCEDLYRICS=[00:00.50]FLAC line\n[00:02.00]Next",
                ),
            )

        assertEquals(EmbeddedLyricsFormat.FLAC_VORBIS, result?.format)
        assertEquals(listOf(500L, 2_000L), result?.parsed?.lines?.map(LyricLine::timestampMs))
    }

    @Test
    fun `rejects plain embedded lyrics without synchronization timestamps`() {
        val payload =
            byteArrayOf(3) + "eng".toByteArray() + byteArrayOf(0) +
                "plain unsynchronized lyrics".toByteArray()

        assertNull(extract(id3v23(id3Frame("USLT", payload))))
    }

    @Test
    fun `rejects malformed or unsupported media without throwing`() {
        assertNull(extract(byteArrayOf()))
        assertNull(extract("not audio".toByteArray()))
        assertNull(extract("fLaC".toByteArray() + byteArrayOf(0x84.toByte(), 0, 0, 8, 1, 2)))
        assertNull(
            extract(
                byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 9, 0, 0, 0, 0, 0, 1, 0),
            ),
        )
    }

    @Test
    fun `prefers real SYLT over lower-priority text candidates`() {
        val textPayload =
            byteArrayOf(3) + "LYRICS".toByteArray() + byteArrayOf(0) +
                "[00:01.00]text candidate".toByteArray()
        val syltPayload =
            ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.writeByte(3)
                    output.writeBytes("eng")
                    output.writeByte(2)
                    output.writeByte(1)
                    output.writeByte(0)
                    writeSyltEntry(output, "SYLT candidate", 1_500)
                }
                bytes.toByteArray()
            }
        val result =
            extract(
                id3v24(
                    id3v24Frame("TXXX", textPayload),
                    id3v24Frame("SYLT", syltPayload),
                ),
            )

        assertEquals(EmbeddedLyricsFormat.ID3_SYLT, result?.format)
        assertTrue(result?.parsed?.lines?.single()?.text == "SYLT candidate")
    }

    private fun extract(bytes: ByteArray): EmbeddedLyrics? =
        EmbeddedLyricsExtractor.extract(ByteArrayInputStream(bytes))
}

private fun writeSyltEntry(
    output: DataOutputStream,
    text: String,
    timestampMs: Int,
) {
    output.write(text.toByteArray(StandardCharsets.UTF_8))
    output.writeByte(0)
    output.writeInt(timestampMs)
}

private fun id3Frame(
    id: String,
    payload: ByteArray,
): ByteArray =
    ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeBytes(id)
            output.writeInt(payload.size)
            output.writeShort(0)
            output.write(payload)
        }
        bytes.toByteArray()
    }

private fun id3v24Frame(
    id: String,
    payload: ByteArray,
): ByteArray =
    ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeBytes(id)
            output.write(syncSafe(payload.size))
            output.writeShort(0)
            output.write(payload)
        }
        bytes.toByteArray()
    }

private fun id3v23(vararg frames: ByteArray): ByteArray = id3Tag(version = 3, frames = frames)

private fun id3v24(vararg frames: ByteArray): ByteArray = id3Tag(version = 4, frames = frames)

private fun id3Tag(
    version: Int,
    frames: Array<out ByteArray>,
): ByteArray {
    val body = frames.fold(ByteArray(0), ByteArray::plus)
    return "ID3".toByteArray() + byteArrayOf(version.toByte(), 0, 0) + syncSafe(body.size) + body
}

private fun syncSafe(value: Int): ByteArray =
    byteArrayOf(
        ((value ushr 21) and 0x7f).toByte(),
        ((value ushr 14) and 0x7f).toByte(),
        ((value ushr 7) and 0x7f).toByte(),
        (value and 0x7f).toByte(),
    )

private fun flacWithComments(vararg comments: String): ByteArray {
    val block =
        ByteArrayOutputStream().use { bytes ->
            val vendor = "ACG Player X".toByteArray(StandardCharsets.UTF_8)
            bytes.writeLittleEndianInt(vendor.size)
            bytes.write(vendor)
            bytes.writeLittleEndianInt(comments.size)
            comments.forEach { comment ->
                val encoded = comment.toByteArray(StandardCharsets.UTF_8)
                bytes.writeLittleEndianInt(encoded.size)
                bytes.write(encoded)
            }
            bytes.toByteArray()
        }
    val header =
        byteArrayOf(
            0x84.toByte(),
            ((block.size ushr 16) and 0xff).toByte(),
            ((block.size ushr 8) and 0xff).toByte(),
            (block.size and 0xff).toByte(),
        )
    return "fLaC".toByteArray() + header + block
}

private fun ByteArrayOutputStream.writeLittleEndianInt(value: Int) {
    write(value and 0xff)
    write((value ushr 8) and 0xff)
    write((value ushr 16) and 0xff)
    write((value ushr 24) and 0xff)
}
