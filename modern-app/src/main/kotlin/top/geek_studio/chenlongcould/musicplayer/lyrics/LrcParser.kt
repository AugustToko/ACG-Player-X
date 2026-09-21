package top.geek_studio.chenlongcould.musicplayer.lyrics

data class LyricLine(
    val timestampMs: Long,
    val text: String,
)

data class ParsedLyrics(
    val lines: List<LyricLine>,
    val fileOffsetMs: Long = 0L,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
)

fun parseLrc(content: String): ParsedLyrics {
    var fileOffsetMs = 0L
    var title: String? = null
    var artist: String? = null
    var album: String? = null
    val lines = mutableListOf<LyricLine>()

    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim().removePrefix("\uFEFF")
        if (line.isEmpty()) return@forEach

        val timestamps = TIMESTAMP_REGEX.findAll(line).toList()
        if (timestamps.isEmpty()) {
            val attribute = ATTRIBUTE_REGEX.matchEntire(line) ?: return@forEach
            val key = attribute.groupValues[1].trim().lowercase()
            val value = attribute.groupValues[2].trim()
            when (key) {
                "offset" -> fileOffsetMs = value.toLongOrNull() ?: fileOffsetMs
                "ti", "title" -> title = value.takeIf(String::isNotBlank)
                "ar", "artist" -> artist = value.takeIf(String::isNotBlank)
                "al", "album" -> album = value.takeIf(String::isNotBlank)
            }
            return@forEach
        }

        val textStart = timestamps.last().range.last + 1
        val lyricText = line.substring(textStart).trim()
        timestamps.forEach { match ->
            parseTimestampMs(match)?.let { timestampMs ->
                lines += LyricLine(timestampMs = timestampMs, text = lyricText)
            }
        }
    }

    return ParsedLyrics(
        lines =
            lines
                .distinctBy { it.timestampMs to it.text }
                .sortedWith(compareBy<LyricLine> { it.timestampMs }.thenBy { it.text }),
        fileOffsetMs = fileOffsetMs,
        title = title,
        artist = artist,
        album = album,
    )
}

fun activeLyricIndex(
    lines: List<LyricLine>,
    positionMs: Long,
    totalOffsetMs: Long = 0L,
): Int {
    if (lines.isEmpty()) return -1

    val adjustedPosition = positionMs - totalOffsetMs
    var low = 0
    var high = lines.lastIndex
    var result = -1

    while (low <= high) {
        val middle = (low + high).ushr(1)
        if (lines[middle].timestampMs <= adjustedPosition) {
            result = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }

    return result
}

fun LyricLine.effectiveTimestampMs(totalOffsetMs: Long): Long =
    (timestampMs + totalOffsetMs).coerceAtLeast(0L)

private fun parseTimestampMs(match: MatchResult): Long? {
    val minutes = match.groupValues[1].toLongOrNull() ?: return null
    val seconds = match.groupValues[2].toLongOrNull() ?: return null
    if (seconds !in 0L..59L) return null

    val fraction = match.groupValues[3]
    val fractionMs =
        when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100L
            2 -> fraction.toLong() * 10L
            else -> fraction.take(3).padEnd(3, '0').toLong()
        }

    return minutes * 60_000L + seconds * 1_000L + fractionMs
}

private val TIMESTAMP_REGEX =
    Regex("""\[(\d{1,4}):(\d{1,2})(?:[.:](\d{1,6}))?]""")
private val ATTRIBUTE_REGEX =
    Regex("""\[([A-Za-z]+):\s*(.*)]""")
