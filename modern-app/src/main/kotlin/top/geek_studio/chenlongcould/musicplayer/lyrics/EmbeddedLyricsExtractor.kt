package top.geek_studio.chenlongcould.musicplayer.lyrics

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal enum class EmbeddedLyricsFormat {
    ID3_USLT,
    ID3_SYLT,
    ID3_TEXT,
    FLAC_VORBIS,
}

internal data class EmbeddedLyrics(
    val parsed: ParsedLyrics,
    val format: EmbeddedLyricsFormat,
)

/**
 * Bounded, read-only extractor for synchronized lyrics embedded in local audio files.
 *
 * Supported containers/tags:
 * - ID3v2.2/v2.3/v2.4 USLT/ULT carrying LRC text.
 * - ID3v2.2/v2.3/v2.4 SYLT/SLT with millisecond timestamps.
 * - ID3 TXXX/TXX/COMM/COM lyric fields carrying LRC text.
 * - FLAC Vorbis-comment lyric fields carrying LRC text.
 *
 * The extractor intentionally does not read the complete audio payload. ID3 bodies and FLAC
 * metadata scans are bounded so a malformed or hostile document provider cannot force an
 * unbounded allocation.
 */
internal object EmbeddedLyricsExtractor {
    fun extract(input: InputStream): EmbeddedLyrics? =
        try {
            extractSafely(input)
        } catch (_: Exception) {
            null
        }

    private fun extractSafely(input: InputStream): EmbeddedLyrics? {
        val stream = PushbackInputStream(BufferedInputStream(input), SIGNATURE_BYTES)
        val signature = ByteArray(SIGNATURE_BYTES)
        val count = stream.readAtMost(signature)
        if (count <= 0) return null
        stream.unread(signature, 0, count)

        return when {
            count >= 3 && signature.matchesAscii(0, "ID3") -> extractId3(stream)
            count >= 4 && signature.matchesAscii(0, "fLaC") -> extractFlac(stream)
            else -> null
        }
    }

    private fun extractId3(input: InputStream): EmbeddedLyrics? {
        val header = input.readExactly(ID3_HEADER_BYTES) ?: return null
        if (!header.matchesAscii(0, "ID3")) return null

        val majorVersion = header[3].toInt() and 0xff
        if (majorVersion !in 2..4) return null
        val flags = header[5].toInt() and 0xff
        val declaredSize = syncSafeInt(header, 6) ?: return null
        if (declaredSize !in 1..MAX_ID3_TAG_BYTES) return null

        var body = input.readExactly(declaredSize) ?: return null
        val tagUnsynchronized = flags and ID3_FLAG_UNSYNCHRONIZATION != 0
        if (tagUnsynchronized) body = removeUnsynchronization(body)

        var offset = id3FramesOffset(body, majorVersion, flags)
        var best: RankedLyrics? = null
        while (offset < body.size) {
            val headerSize = if (majorVersion == 2) ID3_V22_FRAME_HEADER_BYTES else ID3_FRAME_HEADER_BYTES
            if (body.size - offset < headerSize || body[offset] == 0.toByte()) break

            val idLength = if (majorVersion == 2) 3 else 4
            val frameId = body.ascii(offset, idLength)
            if (!frameId.isFrameId(idLength)) break

            val frameSize =
                when (majorVersion) {
                    2 -> unsigned24(body, offset + 3)
                    3 -> unsigned32(body, offset + 4)
                    else -> syncSafeInt(body, offset + 4)
                } ?: break
            if (frameSize <= 0 || frameSize > body.size - offset - headerSize) break

            val secondFlag = if (majorVersion == 2) 0 else body[offset + 9].toInt() and 0xff
            val rawPayload = body.copyOfRange(offset + headerSize, offset + headerSize + frameSize)
            prepareFramePayload(
                payload = rawPayload,
                majorVersion = majorVersion,
                secondFlag = secondFlag,
                tagAlreadyUnsynchronized = tagUnsynchronized,
            )?.let { payload ->
                parseId3LyricsFrame(frameId, payload)?.let { candidate ->
                    val previous = best
                    if (previous == null || candidate.score > previous.score) best = candidate
                }
            }
            offset += headerSize + frameSize
        }
        return best?.lyrics
    }

    private fun id3FramesOffset(
        body: ByteArray,
        majorVersion: Int,
        flags: Int,
    ): Int {
        if (majorVersion == 2 || flags and ID3_FLAG_EXTENDED_HEADER == 0 || body.size < 4) return 0
        val size =
            if (majorVersion == 4) {
                syncSafeInt(body, 0)
            } else {
                unsigned32(body, 0)?.let { it + 4 }
            } ?: return 0
        return size.takeIf { it in 4..body.size } ?: 0
    }

    private fun prepareFramePayload(
        payload: ByteArray,
        majorVersion: Int,
        secondFlag: Int,
        tagAlreadyUnsynchronized: Boolean,
    ): ByteArray? {
        if (majorVersion == 2) return payload

        if (majorVersion == 3) {
            if (secondFlag and (ID3_V23_COMPRESSION or ID3_V23_ENCRYPTION) != 0) return null
            return if (secondFlag and ID3_V23_GROUPING != 0) payload.dropPrefix(1) else payload
        }

        if (secondFlag and (ID3_V24_COMPRESSION or ID3_V24_ENCRYPTION) != 0) return null
        var result =
            if (!tagAlreadyUnsynchronized && secondFlag and ID3_V24_UNSYNCHRONIZATION != 0) {
                removeUnsynchronization(payload)
            } else {
                payload
            }
        if (secondFlag and ID3_V24_GROUPING != 0) result = result.dropPrefix(1) ?: return null
        if (secondFlag and ID3_V24_DATA_LENGTH != 0) result = result.dropPrefix(4) ?: return null
        return result
    }

    private fun parseId3LyricsFrame(
        frameId: String,
        payload: ByteArray,
    ): RankedLyrics? =
        when (frameId) {
            "USLT", "ULT" -> parseUslt(payload)
            "SYLT", "SLT" -> parseSylt(payload)
            "TXXX", "TXX" -> parseTxxx(payload)
            "COMM", "COM" -> parseComment(payload)
            else -> null
        }

    private fun parseUslt(payload: ByteArray): RankedLyrics? {
        if (payload.size < 5) return null
        val decoder = EncodedTextDecoder(payload[0].toInt() and 0xff) ?: return null
        val description = decoder.readTerminated(payload, 4) ?: return null
        val text = decoder.decode(payload, description.nextOffset, payload.size).cleanEmbeddedText()
        return rankedLrc(text, EmbeddedLyricsFormat.ID3_USLT, BASE_SCORE_USLT)
    }

    private fun parseTxxx(payload: ByteArray): RankedLyrics? {
        if (payload.size < 3) return null
        val decoder = EncodedTextDecoder(payload[0].toInt() and 0xff) ?: return null
        val description = decoder.readTerminated(payload, 1) ?: return null
        if (!description.text.isLyricsField()) return null
        val text = decoder.decode(payload, description.nextOffset, payload.size).cleanEmbeddedText()
        return rankedLrc(text, EmbeddedLyricsFormat.ID3_TEXT, BASE_SCORE_TEXT)
    }

    private fun parseComment(payload: ByteArray): RankedLyrics? {
        if (payload.size < 5) return null
        val decoder = EncodedTextDecoder(payload[0].toInt() and 0xff) ?: return null
        val description = decoder.readTerminated(payload, 4) ?: return null
        if (!description.text.isLyricsField()) return null
        val text = decoder.decode(payload, description.nextOffset, payload.size).cleanEmbeddedText()
        return rankedLrc(text, EmbeddedLyricsFormat.ID3_TEXT, BASE_SCORE_COMMENT)
    }

    private fun parseSylt(payload: ByteArray): RankedLyrics? {
        if (payload.size < 8) return null
        val decoder = EncodedTextDecoder(payload[0].toInt() and 0xff) ?: return null
        val timestampFormat = payload[4].toInt() and 0xff
        val contentType = payload[5].toInt() and 0xff
        if (timestampFormat != SYLT_TIMESTAMP_MILLISECONDS) return null
        val description = decoder.readTerminated(payload, 6) ?: return null

        val fragments = mutableListOf<TimedText>()
        var offset = description.nextOffset
        while (offset < payload.size) {
            val text = decoder.readTerminated(payload, offset) ?: break
            offset = text.nextOffset
            if (payload.size - offset < 4) break
            val timestamp = unsigned32Long(payload, offset)
            offset += 4
            val normalized = text.text.replace('\r', '\n')
            if (normalized.isNotEmpty()) fragments += TimedText(timestamp, normalized)
        }
        val lines = syltFragmentsToLines(fragments)
        if (lines.isEmpty()) return null
        val parsed = ParsedLyrics(lines = lines)
        val contentBonus = if (contentType == SYLT_CONTENT_LYRICS) 30 else 0
        return RankedLyrics(
            lyrics = EmbeddedLyrics(parsed, EmbeddedLyricsFormat.ID3_SYLT),
            score = BASE_SCORE_SYLT + contentBonus + lines.size.coerceAtMost(SCORE_LINE_CAP),
        )
    }

    private fun syltFragmentsToLines(fragments: List<TimedText>): List<LyricLine> {
        if (fragments.isEmpty()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        var lineTimestamp = -1L
        val current = StringBuilder()

        fun flush() {
            val text = current.toString().trim()
            if (lineTimestamp >= 0L && text.isNotEmpty()) {
                lines += LyricLine(lineTimestamp, text)
            }
            current.clear()
            lineTimestamp = -1L
        }

        fragments.forEachIndexed { index, fragment ->
            if (lineTimestamp < 0L) lineTimestamp = fragment.timestampMs
            val parts = fragment.text.split('\n')
            parts.forEachIndexed { partIndex, part ->
                current.append(part)
                if (partIndex < parts.lastIndex) flush()
            }

            val nextTimestamp = fragments.getOrNull(index + 1)?.timestampMs
            val largeGap = nextTimestamp != null && nextTimestamp - fragment.timestampMs >= SYLT_LINE_GAP_MS
            val punctuationBoundary =
                current.length >= SYLT_MIN_PUNCTUATION_LINE_LENGTH &&
                    current.lastOrNull() in SYLT_LINE_ENDINGS
            val lengthBoundary = current.length >= SYLT_MAX_LINE_LENGTH
            if (largeGap || punctuationBoundary || lengthBoundary) flush()
        }
        flush()

        return lines
            .distinctBy { it.timestampMs to it.text }
            .sortedBy(LyricLine::timestampMs)
    }

    private fun rankedLrc(
        content: String,
        format: EmbeddedLyricsFormat,
        baseScore: Int,
    ): RankedLyrics? {
        if (content.isBlank() || content.length > MAX_LYRICS_TEXT_CHARS) return null
        val parsed = parseLrc(content)
        if (parsed.lines.isEmpty()) return null
        return RankedLyrics(
            lyrics = EmbeddedLyrics(parsed, format),
            score = baseScore + parsed.lines.size.coerceAtMost(SCORE_LINE_CAP),
        )
    }

    private fun extractFlac(input: InputStream): EmbeddedLyrics? {
        val signature = input.readExactly(4) ?: return null
        if (!signature.matchesAscii(0, "fLaC")) return null

        var scannedBytes = 4L
        while (scannedBytes <= MAX_FLAC_METADATA_SCAN_BYTES) {
            val header = input.readExactly(4) ?: return null
            scannedBytes += 4
            val isLast = header[0].toInt() and 0x80 != 0
            val type = header[0].toInt() and 0x7f
            val length = unsigned24(header, 1) ?: return null
            scannedBytes += length
            if (scannedBytes > MAX_FLAC_METADATA_SCAN_BYTES) return null

            if (type == FLAC_VORBIS_COMMENT_BLOCK) {
                val block = input.readExactly(length) ?: return null
                parseVorbisComments(block)?.let { return it }
            } else {
                input.skipExactly(length)
            }
            if (isLast) break
        }
        return null
    }

    private fun parseVorbisComments(block: ByteArray): EmbeddedLyrics? {
        var offset = 0
        val vendorLength = littleEndian32(block, offset) ?: return null
        offset += 4
        if (vendorLength < 0 || vendorLength > block.size - offset) return null
        offset += vendorLength

        val count = littleEndian32(block, offset) ?: return null
        offset += 4
        if (count !in 0..MAX_VORBIS_COMMENTS) return null

        var best: RankedLyrics? = null
        repeat(count) {
            val length = littleEndian32(block, offset) ?: return null
            offset += 4
            if (length < 0 || length > block.size - offset) return null
            if (length > MAX_VORBIS_COMMENT_BYTES) {
                offset += length
                return@repeat
            }

            val comment = String(block, offset, length, StandardCharsets.UTF_8)
            offset += length
            val separator = comment.indexOf('=')
            if (separator <= 0) return@repeat
            val key = comment.substring(0, separator)
            if (!key.isLyricsField()) return@repeat
            val value = comment.substring(separator + 1).cleanEmbeddedText()
            val candidate = rankedLrc(value, EmbeddedLyricsFormat.FLAC_VORBIS, BASE_SCORE_FLAC)
            val previous = best
            if (candidate != null && (previous == null || candidate.score > previous.score)) {
                best = candidate
            }
        }
        return best?.lyrics
    }

    private data class RankedLyrics(
        val lyrics: EmbeddedLyrics,
        val score: Int,
    )

    private data class TimedText(
        val timestampMs: Long,
        val text: String,
    )

    private class EncodedTextDecoder private constructor(
        private val encoding: Int,
    ) {
        private var utf16Charset: Charset? = null

        fun readTerminated(
            data: ByteArray,
            start: Int,
        ): DecodedString? {
            if (start !in 0..data.size) return null
            val terminatorWidth = if (encoding == 1 || encoding == 2) 2 else 1
            var cursor = start
            while (cursor + terminatorWidth <= data.size) {
                val terminated =
                    if (terminatorWidth == 1) {
                        data[cursor] == 0.toByte()
                    } else {
                        (cursor - start) % 2 == 0 &&
                            data[cursor] == 0.toByte() &&
                            data[cursor + 1] == 0.toByte()
                    }
                if (terminated) {
                    return DecodedString(
                        text = decode(data, start, cursor),
                        nextOffset = cursor + terminatorWidth,
                    )
                }
                cursor += if (terminatorWidth == 2) 2 else 1
            }
            return null
        }

        fun decode(
            data: ByteArray,
            start: Int,
            end: Int,
        ): String {
            if (start !in 0..data.size || end !in start..data.size) return ""
            var contentStart = start
            val charset =
                when (encoding) {
                    0 -> StandardCharsets.ISO_8859_1
                    2 -> StandardCharsets.UTF_16BE
                    3 -> StandardCharsets.UTF_8
                    else -> {
                        if (end - start >= 2) {
                            when {
                                data[start] == 0xff.toByte() && data[start + 1] == 0xfe.toByte() -> {
                                    utf16Charset = StandardCharsets.UTF_16LE
                                    contentStart += 2
                                }
                                data[start] == 0xfe.toByte() && data[start + 1] == 0xff.toByte() -> {
                                    utf16Charset = StandardCharsets.UTF_16BE
                                    contentStart += 2
                                }
                            }
                        }
                        utf16Charset ?: StandardCharsets.UTF_16BE
                    }
                }
            if (contentStart >= end) return ""
            return String(data, contentStart, end - contentStart, charset)
                .trimEnd('\u0000')
        }

        companion object {
            operator fun invoke(encoding: Int): EncodedTextDecoder? =
                encoding.takeIf { it in 0..3 }?.let(::EncodedTextDecoder)
        }
    }

    private data class DecodedString(
        val text: String,
        val nextOffset: Int,
    )

    private fun String.cleanEmbeddedText(): String =
        replace("\u0000", "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()

    private fun String.isLyricsField(): Boolean {
        val normalized =
            uppercase()
                .filter(Char::isLetterOrDigit)
        return normalized in LYRICS_FIELD_NAMES
    }

    private fun String.isFrameId(expectedLength: Int): Boolean =
        length == expectedLength && all { it in 'A'..'Z' || it in '0'..'9' }

    private fun ByteArray.dropPrefix(count: Int): ByteArray? =
        if (size >= count) copyOfRange(count, size) else null

    private fun ByteArray.matchesAscii(
        offset: Int,
        value: String,
    ): Boolean =
        offset >= 0 && value.length <= size - offset &&
            value.indices.all { index -> this[offset + index].toInt() and 0xff == value[index].code }

    private fun ByteArray.ascii(
        offset: Int,
        length: Int,
    ): String = String(this, offset, length, StandardCharsets.ISO_8859_1)

    private fun InputStream.readAtMost(destination: ByteArray): Int {
        var total = 0
        while (total < destination.size) {
            val count = read(destination, total, destination.size - total)
            if (count < 0) break
            total += count
        }
        return total
    }

    private fun InputStream.readExactly(count: Int): ByteArray? {
        if (count < 0) return null
        val result = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = read(result, offset, count - offset)
            if (read < 0) return null
            offset += read
        }
        return result
    }

    private fun InputStream.skipExactly(count: Int) {
        var remaining = count.toLong()
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else if (read() >= 0) {
                remaining -= 1
            } else {
                throw EOFException("Unexpected end of embedded metadata")
            }
        }
    }

    private fun syncSafeInt(
        data: ByteArray,
        offset: Int,
    ): Int? {
        if (offset < 0 || data.size - offset < 4) return null
        var value = 0
        repeat(4) { index ->
            val byte = data[offset + index].toInt() and 0xff
            if (byte and 0x80 != 0) return null
            value = (value shl 7) or byte
        }
        return value
    }

    private fun unsigned24(
        data: ByteArray,
        offset: Int,
    ): Int? {
        if (offset < 0 || data.size - offset < 3) return null
        return ((data[offset].toInt() and 0xff) shl 16) or
            ((data[offset + 1].toInt() and 0xff) shl 8) or
            (data[offset + 2].toInt() and 0xff)
    }

    private fun unsigned32(
        data: ByteArray,
        offset: Int,
    ): Int? {
        val value = unsigned32Long(data, offset)
        return value.takeIf { it <= Int.MAX_VALUE }?.toInt()
    }

    private fun unsigned32Long(
        data: ByteArray,
        offset: Int,
    ): Long {
        if (offset < 0 || data.size - offset < 4) return Long.MAX_VALUE
        return ((data[offset].toLong() and 0xff) shl 24) or
            ((data[offset + 1].toLong() and 0xff) shl 16) or
            ((data[offset + 2].toLong() and 0xff) shl 8) or
            (data[offset + 3].toLong() and 0xff)
    }

    private fun littleEndian32(
        data: ByteArray,
        offset: Int,
    ): Int? {
        if (offset < 0 || data.size - offset < 4) return null
        val value =
            (data[offset].toLong() and 0xff) or
                ((data[offset + 1].toLong() and 0xff) shl 8) or
                ((data[offset + 2].toLong() and 0xff) shl 16) or
                ((data[offset + 3].toLong() and 0xff) shl 24)
        return value.takeIf { it <= Int.MAX_VALUE }?.toInt()
    }

    private fun removeUnsynchronization(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        var source = 0
        var target = 0
        while (source < data.size) {
            val current = data[source]
            result[target++] = current
            if (
                current == 0xff.toByte() &&
                source + 1 < data.size &&
                data[source + 1] == 0.toByte()
            ) {
                source += 1
            }
            source += 1
        }
        return result.copyOf(target)
    }

    private const val SIGNATURE_BYTES = 10
    private const val ID3_HEADER_BYTES = 10
    private const val ID3_FRAME_HEADER_BYTES = 10
    private const val ID3_V22_FRAME_HEADER_BYTES = 6
    private const val MAX_ID3_TAG_BYTES = 16 * 1024 * 1024
    private const val MAX_FLAC_METADATA_SCAN_BYTES = 32L * 1024L * 1024L
    private const val MAX_LYRICS_TEXT_CHARS = 2 * 1024 * 1024
    private const val MAX_VORBIS_COMMENTS = 10_000
    private const val MAX_VORBIS_COMMENT_BYTES = MAX_LYRICS_TEXT_CHARS + 256

    private const val ID3_FLAG_UNSYNCHRONIZATION = 0x80
    private const val ID3_FLAG_EXTENDED_HEADER = 0x40
    private const val ID3_V23_COMPRESSION = 0x80
    private const val ID3_V23_ENCRYPTION = 0x40
    private const val ID3_V23_GROUPING = 0x20
    private const val ID3_V24_GROUPING = 0x40
    private const val ID3_V24_COMPRESSION = 0x08
    private const val ID3_V24_ENCRYPTION = 0x04
    private const val ID3_V24_UNSYNCHRONIZATION = 0x02
    private const val ID3_V24_DATA_LENGTH = 0x01

    private const val SYLT_TIMESTAMP_MILLISECONDS = 2
    private const val SYLT_CONTENT_LYRICS = 1
    private const val SYLT_LINE_GAP_MS = 4_000L
    private const val SYLT_MIN_PUNCTUATION_LINE_LENGTH = 12
    private const val SYLT_MAX_LINE_LENGTH = 64
    private val SYLT_LINE_ENDINGS = setOf('.', '!', '?', '。', '！', '？')

    private const val FLAC_VORBIS_COMMENT_BLOCK = 4
    private const val BASE_SCORE_SYLT = 500
    private const val BASE_SCORE_USLT = 400
    private const val BASE_SCORE_FLAC = 350
    private const val BASE_SCORE_TEXT = 300
    private const val BASE_SCORE_COMMENT = 250
    private const val SCORE_LINE_CAP = 100
    private val LYRICS_FIELD_NAMES =
        setOf("LYRIC", "LYRICS", "LRC", "SYNCEDLYRICS", "UNSYNCEDLYRICS")
}
