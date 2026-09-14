package top.geek_studio.chenlongcould.musicplayer.lyrics

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val lyricsDirectory = File(appContext.filesDir, LYRICS_DIRECTORY)
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun load(mediaId: String): ParsedLyrics? = withContext(Dispatchers.IO) {
        val file = lyricsFile(mediaId)
        if (!file.isFile) return@withContext null

        runCatching { parseLrc(file.readText(Charsets.UTF_8)) }
            .getOrElse {
                file.delete()
                null
            }
    }

    suspend fun import(
        mediaId: String,
        uri: Uri,
    ): ParsedLyrics = withContext(Dispatchers.IO) {
        val bytes =
            appContext.contentResolver.openInputStream(uri)?.use(::readLimited)
                ?: throw IllegalArgumentException("无法读取所选歌词文件")
        val content = decodeLyrics(bytes)
        val parsed = parseLrc(content)
        require(parsed.lines.isNotEmpty()) {
            "未检测到可同步的 LRC 时间标签"
        }

        check(lyricsDirectory.exists() || lyricsDirectory.mkdirs()) {
            "无法创建歌词缓存目录"
        }
        lyricsFile(mediaId).writeText(content, Charsets.UTF_8)
        parsed
    }

    suspend fun delete(mediaId: String) = withContext(Dispatchers.IO) {
        lyricsFile(mediaId).delete()
        preferences.edit().remove(offsetKey(mediaId)).apply()
    }

    fun getUserOffsetMs(mediaId: String): Long =
        preferences.getLong(offsetKey(mediaId), 0L)

    fun setUserOffsetMs(
        mediaId: String,
        offsetMs: Long,
    ) {
        preferences.edit()
            .putLong(offsetKey(mediaId), offsetMs.coerceIn(-MAX_USER_OFFSET_MS, MAX_USER_OFFSET_MS))
            .apply()
    }

    private fun lyricsFile(mediaId: String): File =
        File(lyricsDirectory, "${stableKey(mediaId)}.lrc")

    private fun offsetKey(mediaId: String): String =
        "offset_${stableKey(mediaId)}"

    private fun stableKey(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .take(16)
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_LYRICS_BYTES) {
                "歌词文件不能超过 ${MAX_LYRICS_BYTES / 1024 / 1024} MB"
            }
            output.write(buffer, 0, count)
        }

        return output.toByteArray()
    }

    private fun decodeLyrics(bytes: ByteArray): String {
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
        const val LYRICS_DIRECTORY = "lyrics"
        const val PREFERENCES_NAME = "lyrics_preferences"
        const val MAX_LYRICS_BYTES = 2 * 1024 * 1024
        const val MAX_USER_OFFSET_MS = 30_000L
    }
}
