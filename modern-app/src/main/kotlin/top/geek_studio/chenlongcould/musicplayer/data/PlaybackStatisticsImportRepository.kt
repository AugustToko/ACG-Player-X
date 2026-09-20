package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackStatisticsImportRepository(
    context: Context,
) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun read(uri: Uri): PlaybackStatisticsImportDocument =
        withContext(Dispatchers.IO) {
            val input =
                contentResolver.openInputStream(uri)
                    ?: throw IOException("无法打开所选统计文件")
            val bytes =
                input.use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_IMPORT_BYTES) {
                            throw IllegalArgumentException("统计文件不能超过 4 MB")
                        }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
            val json =
                bytes.toString(StandardCharsets.UTF_8)
                    .removePrefix("\uFEFF")
            parsePlaybackStatisticsImportJson(json)
        }
}

internal fun parsePlaybackStatisticsImportJson(json: String): PlaybackStatisticsImportDocument {
    if (json.isBlank()) throw IllegalArgumentException("统计文件为空")

    var schemaVersion = 0
    var generatedAtMs = 0L
    var rawEntryCount = 0
    var duplicateSourceEntryCount = 0
    val entriesByMediaId = linkedMapOf<String, PlaybackStatisticsEntry>()

    JsonReader(StringReader(json)).use { reader ->
        reader.isLenient = false
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "schemaVersion", "version" -> schemaVersion = reader.nextNonNegativeInt()
                "generatedAtMs", "exportedAtMs" -> generatedAtMs = reader.nextNonNegativeLong()
                "summary" -> reader.skipValue()
                "entries", "statistics" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        rawEntryCount += 1
                        if (rawEntryCount > MAX_IMPORT_ENTRIES) {
                            throw IllegalArgumentException("统计文件最多包含 20,000 项")
                        }
                        val entry = reader.readPlaybackStatisticsEntry()
                        if (entry.mediaId.isBlank()) continue
                        val existing = entriesByMediaId[entry.mediaId]
                        if (existing == null) {
                            entriesByMediaId[entry.mediaId] = entry
                        } else {
                            duplicateSourceEntryCount += 1
                            entriesByMediaId[entry.mediaId] = mergeImportedEntries(existing, entry)
                        }
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw IllegalArgumentException("统计文件包含多余内容")
        }
    }

    if (schemaVersion <= 0) throw IllegalArgumentException("统计文件缺少 schemaVersion")
    if (schemaVersion > PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION) {
        throw IllegalArgumentException(
            "统计文件版本 $schemaVersion 高于当前支持的版本 $PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION",
        )
    }

    return PlaybackStatisticsImportDocument(
        schemaVersion = schemaVersion,
        generatedAtMs = generatedAtMs,
        entries = entriesByMediaId.values.toList(),
        duplicateSourceEntryCount = duplicateSourceEntryCount,
    )
}

private fun JsonReader.readPlaybackStatisticsEntry(): PlaybackStatisticsEntry {
    var mediaId = ""
    var available = false
    var title = ""
    var artist = ""
    var album = ""
    var folderPath = ""
    var playCount = 0
    var lastPlayedAtMs = 0L
    var completedCount = 0
    var lastCompletedAtMs = 0L
    var totalListenTimeMs = 0L
    var lastPositionMs = 0L
    var durationMs = 0L

    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "mediaId" -> mediaId = nextStringValue().trim().take(MAX_TEXT_LENGTH)
            "available" -> available = nextBooleanValue()
            "title" -> title = nextStringValue().take(MAX_TEXT_LENGTH)
            "artist" -> artist = nextStringValue().take(MAX_TEXT_LENGTH)
            "album" -> album = nextStringValue().take(MAX_TEXT_LENGTH)
            "folderPath" -> folderPath = nextStringValue().take(MAX_PATH_LENGTH)
            "playCount" -> playCount = nextNonNegativeInt()
            "lastPlayedAtMs" -> lastPlayedAtMs = nextNonNegativeLong()
            "completedCount" -> completedCount = nextNonNegativeInt()
            "lastCompletedAtMs" -> lastCompletedAtMs = nextNonNegativeLong()
            "totalListenTimeMs" -> totalListenTimeMs = nextNonNegativeLong()
            "lastPositionMs" -> lastPositionMs = nextNonNegativeLong()
            "durationMs" -> durationMs = nextNonNegativeLong()
            else -> skipValue()
        }
    }
    endObject()

    return PlaybackStatisticsEntry(
        mediaId = mediaId,
        available = available,
        title = title,
        artist = artist,
        album = album,
        folderPath = folderPath,
        playCount = playCount,
        lastPlayedAtMs = lastPlayedAtMs,
        completedCount = completedCount,
        lastCompletedAtMs = lastCompletedAtMs,
        totalListenTimeMs = totalListenTimeMs,
        lastPositionMs = lastPositionMs,
        durationMs = durationMs,
    )
}

private fun mergeImportedEntries(
    first: PlaybackStatisticsEntry,
    second: PlaybackStatisticsEntry,
): PlaybackStatisticsEntry {
    val mergedStats =
        mergePlaybackStatsIdempotently(
            current = first.toPlaybackStatsForImport(),
            imported = second.toPlaybackStatsForImport(),
        )
    return first.copy(
        available = first.available || second.available,
        title = first.title.ifBlank { second.title },
        artist = first.artist.ifBlank { second.artist },
        album = first.album.ifBlank { second.album },
        folderPath = first.folderPath.ifBlank { second.folderPath },
        playCount = mergedStats.playCount,
        lastPlayedAtMs = mergedStats.lastPlayedAtMs,
        completedCount = mergedStats.completedCount,
        lastCompletedAtMs = mergedStats.lastCompletedAtMs,
        totalListenTimeMs = mergedStats.totalListenTimeMs,
        lastPositionMs = mergedStats.lastPositionMs,
        durationMs = mergedStats.durationMs,
    )
}

private fun PlaybackStatisticsEntry.toPlaybackStatsForImport(): PlaybackStats =
    PlaybackStats(
        playCount = playCount,
        lastPlayedAtMs = lastPlayedAtMs,
        completedCount = completedCount,
        lastCompletedAtMs = lastCompletedAtMs,
        totalListenTimeMs = totalListenTimeMs,
        lastPositionMs = lastPositionMs,
        durationMs = durationMs,
    )

private fun JsonReader.nextStringValue(): String =
    when (peek()) {
        JsonToken.STRING -> nextString()
        JsonToken.NULL -> {
            nextNull()
            ""
        }
        else -> throw IllegalArgumentException("统计文件字段类型不正确")
    }

private fun JsonReader.nextBooleanValue(): Boolean =
    when (peek()) {
        JsonToken.BOOLEAN -> nextBoolean()
        JsonToken.NULL -> {
            nextNull()
            false
        }
        else -> throw IllegalArgumentException("统计文件字段类型不正确")
    }

private fun JsonReader.nextNonNegativeInt(): Int {
    val value = nextNonNegativeLong()
    if (value > Int.MAX_VALUE) return Int.MAX_VALUE
    return value.toInt()
}

private fun JsonReader.nextNonNegativeLong(): Long =
    when (peek()) {
        JsonToken.NUMBER, JsonToken.STRING ->
            runCatching { nextString().toLong() }
                .getOrElse { throw IllegalArgumentException("统计文件包含无效数字") }
                .coerceAtLeast(0L)
        JsonToken.NULL -> {
            nextNull()
            0L
        }
        else -> throw IllegalArgumentException("统计文件字段类型不正确")
    }

private const val MAX_IMPORT_BYTES = 4 * 1024 * 1024
private const val MAX_IMPORT_ENTRIES = 20_000
private const val MAX_TEXT_LENGTH = 1_024
private const val MAX_PATH_LENGTH = 4_096
