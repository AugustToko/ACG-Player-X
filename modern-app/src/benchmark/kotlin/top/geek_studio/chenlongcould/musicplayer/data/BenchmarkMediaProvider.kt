package top.geek_studio.chenlongcould.musicplayer.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Supplies deterministic media for the release-like benchmark build.
 *
 * The 10k-song fixture intentionally uses synthetic content URIs. Registering a real provider for
 * those URIs keeps artwork decoding and Media3 playback representative without repeatedly asking
 * ActivityThread to resolve a missing authority on physical devices.
 */
class BenchmarkMediaProvider : ContentProvider() {
    private lateinit var audioFile: File
    private lateinit var artworkFile: File
    private val fixtureLock = Any()

    override fun onCreate(): Boolean {
        val providerContext = context ?: return false
        val fixtureDirectory =
            File(providerContext.cacheDir, FIXTURE_DIRECTORY).apply {
                check(mkdirs() || isDirectory) { "Unable to create benchmark media directory" }
            }
        audioFile = File(fixtureDirectory, SILENCE_FILE_NAME)
        artworkFile = File(fixtureDirectory, ARTWORK_FILE_NAME)
        return true
    }

    override fun getType(uri: Uri): String? =
        when (resolveAsset(uri)) {
            Asset.Audio -> MIME_AUDIO
            Asset.Artwork -> MIME_ARTWORK
            null -> null
        }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Benchmark media is read-only")
        val asset = resolveAsset(uri) ?: throw FileNotFoundException("Unknown benchmark media URI: $uri")
        val file = fileForAsset(asset)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val asset = resolveAsset(uri) ?: throw FileNotFoundException("Unknown benchmark media URI: $uri")
        val file = fileForAsset(asset)
        val columns = projection ?: DEFAULT_COLUMNS
        return MatrixCursor(columns, 1).apply {
            val row = newRow()
            columns.forEach { column ->
                row.add(
                    when (column) {
                        OpenableColumns.DISPLAY_NAME -> file.name
                        OpenableColumns.SIZE -> file.length()
                        else -> null
                    },
                )
            }
        }
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = throw UnsupportedOperationException("Benchmark media is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Benchmark media is read-only")

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Benchmark media is read-only")

    private fun fileForAsset(asset: Asset): File =
        synchronized(fixtureLock) {
            when (asset) {
                Asset.Audio -> audioFile.also(::ensureSilenceWav)
                Asset.Artwork -> artworkFile.also(::ensureArtworkPng)
            }
        }

    private fun resolveAsset(uri: Uri): Asset? {
        if (uri.authority != BENCHMARK_MEDIA_AUTHORITY) return null
        val segments = uri.pathSegments
        if (segments == listOf(ARTWORK_PATH)) return Asset.Artwork
        if (
            segments.size == 2 &&
            segments.first() in AUDIO_PATHS &&
            segments.last().toLongOrNull()?.let { it > 0L } == true
        ) {
            return Asset.Audio
        }
        return null
    }

    private enum class Asset {
        Audio,
        Artwork,
    }
}

private val DEFAULT_COLUMNS = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
private val AUDIO_PATHS = setOf("mediastore", "saf")

private const val ARTWORK_PATH = "artwork"
private const val FIXTURE_DIRECTORY = "benchmark-media-v1"
private const val SILENCE_FILE_NAME = "benchmark-silence.wav"
private const val ARTWORK_FILE_NAME = "benchmark-artwork.png"
private const val MIME_AUDIO = "audio/wav"
private const val MIME_ARTWORK = "image/png"

private const val SAMPLE_RATE_HZ = 8_000
private const val CHANNEL_COUNT = 1
private const val BITS_PER_SAMPLE = 16
private const val SILENCE_DURATION_SECONDS = 120
private const val WAV_HEADER_SIZE = 44
private const val PCM_FORMAT = 1
private const val ARTWORK_SIZE_PX = 64

private fun ensureSilenceWav(file: File) {
    val bytesPerSample = BITS_PER_SAMPLE / 8
    val dataSize = SAMPLE_RATE_HZ * CHANNEL_COUNT * bytesPerSample * SILENCE_DURATION_SECONDS
    val expectedSize = WAV_HEADER_SIZE.toLong() + dataSize
    if (file.isFile && file.length() == expectedSize) return

    writeAtomically(file) { output ->
        val byteRate = SAMPLE_RATE_HZ * CHANNEL_COUNT * bytesPerSample
        val blockAlign = CHANNEL_COUNT * bytesPerSample
        val header =
            ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
                putAscii("RIFF")
                putInt(WAV_HEADER_SIZE - 8 + dataSize)
                putAscii("WAVE")
                putAscii("fmt ")
                putInt(16)
                putShort(PCM_FORMAT.toShort())
                putShort(CHANNEL_COUNT.toShort())
                putInt(SAMPLE_RATE_HZ)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(BITS_PER_SAMPLE.toShort())
                putAscii("data")
                putInt(dataSize)
            }.array()
        output.write(header)

        val silence = ByteArray(16 * 1024)
        var remaining = dataSize
        while (remaining > 0) {
            val count = minOf(remaining, silence.size)
            output.write(silence, 0, count)
            remaining -= count
        }
    }
}

private fun ensureArtworkPng(file: File) {
    if (file.isFile && file.length() > 0L) return

    val bitmap = Bitmap.createBitmap(ARTWORK_SIZE_PX, ARTWORK_SIZE_PX, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(37, 46, 63))
        canvas.drawCircle(
            ARTWORK_SIZE_PX * 0.5f,
            ARTWORK_SIZE_PX * 0.5f,
            ARTWORK_SIZE_PX * 0.28f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(116, 181, 255) },
        )
        val bytes =
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Unable to encode benchmark artwork"
                }
                output.toByteArray()
            }
        writeAtomically(file) { it.write(bytes) }
    } finally {
        bitmap.recycle()
    }
}

private inline fun writeAtomically(
    destination: File,
    write: (FileOutputStream) -> Unit,
) {
    val temporary = File(destination.parentFile, ".${destination.name}.tmp")
    FileOutputStream(temporary).use(write)
    if (!temporary.renameTo(destination)) {
        temporary.copyTo(destination, overwrite = true)
        check(temporary.delete()) { "Unable to remove temporary benchmark media file" }
    }
}

private fun ByteBuffer.putAscii(value: String) {
    put(value.toByteArray(Charsets.US_ASCII))
}
