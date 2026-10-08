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
import java.util.LinkedHashMap
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LyricsSource(
    val label: String,
) {
    IMPORTED("已导入 LRC"),
    EMBEDDED_ID3_USLT("音频内嵌 · ID3 USLT"),
    EMBEDDED_ID3_SYLT("音频内嵌 · ID3 SYLT"),
    EMBEDDED_ID3_TEXT("音频内嵌 · ID3 文本标签"),
    EMBEDDED_FLAC("音频内嵌 · FLAC 标签"),
}

data class ResolvedLyrics(
    val parsed: ParsedLyrics,
    val source: LyricsSource,
)

class LyricsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val lyricsDirectory = File(appContext.filesDir, LYRICS_DIRECTORY)
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val embeddedLyricsCache =
        object : LinkedHashMap<EmbeddedCacheKey, EmbeddedLyrics>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<EmbeddedCacheKey, EmbeddedLyrics>?,
            ): Boolean = size > MAX_EMBEDDED_CACHE_ENTRIES
        }

    suspend fun load(mediaId: String): ParsedLyrics? = withContext(Dispatchers.IO) {
        loadImported(mediaId)
    }

    suspend fun resolve(
        mediaId: String,
        mediaUri: Uri?,
    ): ResolvedLyrics? = withContext(Dispatchers.IO) {
        loadImported(mediaId)?.let { imported ->
            return@withContext ResolvedLyrics(imported, LyricsSource.IMPORTED)
        }
        if (mediaUri == null) return@withContext null

        val cacheKey = EmbeddedCacheKey(mediaId = mediaId, mediaUri = mediaUri.toString())
        val cached = synchronized(embeddedLyricsCache) { embeddedLyricsCache[cacheKey] }
        val embedded =
            cached ?:
                runCatching {
                    appContext.contentResolver
                        .openInputStream(mediaUri)
                        ?.use(EmbeddedLyricsExtractor::extract)
                }.getOrNull()?.also { extracted ->
                    synchronized(embeddedLyricsCache) {
                        embeddedLyricsCache[cacheKey] = extracted
                    }
                } ?: return@withContext null
        ResolvedLyrics(
            parsed = embedded.parsed,
            source = embedded.format.toLyricsSource(),
        )
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

    private data class EmbeddedCacheKey(
        val mediaId: String,
        val mediaUri: String,
    )

    private fun loadImported(mediaId: String): ParsedLyrics? {
        val file = lyricsFile(mediaId)
        if (!file.isFile) return null

        val parsed =
            runCatching { parseLrc(file.readText(Charsets.UTF_8)) }
                .getOrElse {
                    file.delete()
                    return null
                }
        if (parsed.lines.isEmpty()) {
            file.delete()
            return null
        }
        return parsed
    }

    private fun EmbeddedLyricsFormat.toLyricsSource(): LyricsSource =
        when (this) {
            EmbeddedLyricsFormat.ID3_USLT -> LyricsSource.EMBEDDED_ID3_USLT
            EmbeddedLyricsFormat.ID3_SYLT -> LyricsSource.EMBEDDED_ID3_SYLT
            EmbeddedLyricsFormat.ID3_TEXT -> LyricsSource.EMBEDDED_ID3_TEXT
            EmbeddedLyricsFormat.FLAC_VORBIS -> LyricsSource.EMBEDDED_FLAC
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
        const val MAX_EMBEDDED_CACHE_ENTRIES = 24
    }
}
