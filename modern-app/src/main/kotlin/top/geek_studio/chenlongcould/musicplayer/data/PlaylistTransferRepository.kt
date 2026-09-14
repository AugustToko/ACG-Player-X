package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.model.Song

data class PlaylistImportResult(
    val preferredName: String,
    val mediaIds: List<String>,
    val totalEntryCount: Int,
    val matchedCount: Int,
    val preservedUnavailableCount: Int,
    val unmatchedCount: Int,
    val ambiguousCount: Int,
    val duplicateCount: Int,
    val truncated: Boolean,
)

data class PlaylistExportResult(
    val exportedCount: Int,
    val unavailableCount: Int,
)

class PlaylistTransferRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    suspend fun importPlaylist(
        uri: Uri,
        librarySongs: List<Song>,
    ): PlaylistImportResult = withContext(Dispatchers.IO) {
        val bytes =
            resolver.openInputStream(uri)?.use(::readLimited)
                ?: throw IllegalArgumentException("无法读取所选歌单文件")
        val fallbackName =
            queryDisplayName(uri)
                ?.substringBeforeLast('.', missingDelimiterValue = "")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: "导入歌单"
        val parsed = parseM3uPlaylist(decodeText(bytes), fallbackName)
        require(parsed.entries.isNotEmpty()) { "M3U/M3U8 文件中没有可导入的歌曲条目" }
        val resolved = resolveM3uPlaylist(parsed, librarySongs)

        PlaylistImportResult(
            preferredName = parsed.name?.takeIf(String::isNotBlank) ?: fallbackName,
            mediaIds = resolved.mediaIds,
            totalEntryCount = parsed.entries.size,
            matchedCount = resolved.matchedCount,
            preservedUnavailableCount = resolved.preservedUnavailableCount,
            unmatchedCount = resolved.unmatchedCount,
            ambiguousCount = resolved.ambiguousCount,
            duplicateCount = resolved.duplicateCount,
            truncated = parsed.truncated,
        )
    }

    suspend fun exportPlaylist(
        uri: Uri,
        playlist: UserPlaylist,
        librarySongs: List<Song>,
    ): PlaylistExportResult = withContext(Dispatchers.IO) {
        val content = encodeM3uPlaylist(playlist, librarySongs)
        val output =
            resolver.openOutputStream(uri, "w")
                ?: throw IllegalArgumentException("无法写入所选目标文件")
        output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(content)
        }
        PlaylistExportResult(
            exportedCount = playlist.mediaIds.size,
            unavailableCount = countUnavailablePlaylistSongs(librarySongs, playlist),
        )
    }

    private fun queryDisplayName(uri: Uri): String? =
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_PLAYLIST_BYTES) {
                "歌单文件不能超过 ${MAX_PLAYLIST_BYTES / 1024 / 1024} MB"
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun decodeText(bytes: ByteArray): String {
        val contentBytes =
            if (
                bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte()
            ) {
                bytes.copyOfRange(3, bytes.size)
            } else {
                bytes
            }

        return try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(contentBytes))
                .toString()
        } catch (_: CharacterCodingException) {
            String(contentBytes, charset("GB18030"))
        }
    }

    private companion object {
        const val MAX_PLAYLIST_BYTES = 4 * 1024 * 1024
    }
}
