package top.geek_studio.chenlongcould.musicplayer.model

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

const val MEDIA_EXTRA_CONTENT_URI =
    "top.geek_studio.chenlongcould.musicplayer.extra.CONTENT_URI"
const val MEDIA_EXTRA_FOLDER_PATH =
    "top.geek_studio.chenlongcould.musicplayer.extra.FOLDER_PATH"

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val folderName: String,
    val folderPath: String,
    val dateAddedMs: Long = 0L,
)

fun Song.toMediaItem(): MediaItem {
    val mediaUri = Uri.parse(contentUri)
    val extras =
        Bundle().apply {
            putString(MEDIA_EXTRA_CONTENT_URI, contentUri)
            putString(MEDIA_EXTRA_FOLDER_PATH, folderPath)
        }

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(mediaUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri?.let(Uri::parse))
                .setExtras(extras)
                .build(),
        )
        .build()
}
