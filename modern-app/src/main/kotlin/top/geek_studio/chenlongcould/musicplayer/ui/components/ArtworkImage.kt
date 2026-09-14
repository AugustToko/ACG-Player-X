package top.geek_studio.chenlongcould.musicplayer.ui.components

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ArtworkImage(
    artworkUri: Uri?,
    fallbackUri: Uri?,
    seed: Long,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glyphSize: Int = 48,
    requestSize: Dp = 256.dp,
    contentDescription: String? = null,
) {
    val resolver = LocalContext.current.contentResolver
    val requestSizePx =
        with(LocalDensity.current) {
            requestSize.roundToPx().coerceAtLeast(MIN_THUMBNAIL_SIZE_PX)
        }
    val candidates = listOfNotNull(artworkUri, fallbackUri).distinct()
    val bitmap by
        produceState<Bitmap?>(
            initialValue = null,
            candidates,
            requestSizePx,
        ) {
            value = null
            value =
                withContext(Dispatchers.IO) {
                    candidates.firstNotNullOfOrNull { uri ->
                        ArtworkBitmapLoader.load(
                            resolver = resolver,
                            uri = uri,
                            requestSizePx = requestSizePx,
                        )
                    }
                }
        }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)),
            contentScale = ContentScale.Crop,
        )
    } else {
        ArtworkPlaceholder(
            seed = seed,
            modifier = modifier,
            cornerRadius = cornerRadius,
            glyphSize = glyphSize,
        )
    }
}

private object ArtworkBitmapLoader {
    private val cache =
        object : LruCache<String, Bitmap>(CACHE_SIZE_KIB) {
            override fun sizeOf(
                key: String,
                value: Bitmap,
            ): Int = (value.allocationByteCount / 1024).coerceAtLeast(1)
        }

    fun load(
        resolver: ContentResolver,
        uri: Uri,
        requestSizePx: Int,
    ): Bitmap? {
        val cacheKey = "$uri@$requestSizePx"
        cache.get(cacheKey)?.let { return it }

        val bitmap =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.loadThumbnail(
                        uri,
                        Size(requestSizePx, requestSizePx),
                        null,
                    )
                } else {
                    decodeSampledBitmap(resolver, uri, requestSizePx)
                }
            }.getOrNull()

        if (bitmap != null) {
            cache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    private fun decodeSampledBitmap(
        resolver: ContentResolver,
        uri: Uri,
        requestSizePx: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options =
            BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds, requestSizePx)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        return resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateSampleSize(
        options: BitmapFactory.Options,
        requestSizePx: Int,
    ): Int {
        var sampleSize = 1
        var width = options.outWidth
        var height = options.outHeight
        while (width / 2 >= requestSizePx && height / 2 >= requestSizePx) {
            width /= 2
            height /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private const val CACHE_SIZE_KIB = 24 * 1024
}

private const val MIN_THUMBNAIL_SIZE_PX = 96
