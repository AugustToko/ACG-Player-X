package top.geek_studio.chenlongcould.musicplayer.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import org.json.JSONArray
import org.json.JSONObject
import top.geek_studio.chenlongcould.musicplayer.model.MEDIA_EXTRA_CONTENT_URI
import top.geek_studio.chenlongcould.musicplayer.model.MEDIA_EXTRA_FOLDER_PATH

data class PersistedQueueItem(
    val mediaId: String,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
    val folderPath: String?,
)

data class PlaybackQueueSnapshot(
    val items: List<PersistedQueueItem>,
    val currentIndex: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
)

data class PlaybackPositionSnapshot(
    val currentIndex: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
)

data class RestoredPlaybackState(
    val mediaItems: List<MediaItem>,
    val currentIndex: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
)

class PlaybackStateStore(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveQueue(snapshot: PlaybackQueueSnapshot) {
        if (snapshot.items.isEmpty()) {
            clear()
            return
        }

        val items =
            JSONArray().apply {
                snapshot.items.forEach { item ->
                    put(
                        JSONObject()
                            .put(KEY_MEDIA_ID, item.mediaId)
                            .put(KEY_CONTENT_URI, item.contentUri)
                            .put(KEY_TITLE, item.title)
                            .put(KEY_ARTIST, item.artist)
                            .put(KEY_ALBUM, item.album)
                            .put(KEY_ARTWORK_URI, item.artworkUri.orEmpty())
                            .put(KEY_FOLDER_PATH, item.folderPath.orEmpty()),
                    )
                }
            }

        preferences.edit()
            .putString(KEY_QUEUE, items.toString())
            .putInt(KEY_CURRENT_INDEX, snapshot.currentIndex)
            .putLong(KEY_POSITION_MS, snapshot.positionMs)
            .putInt(KEY_REPEAT_MODE, snapshot.repeatMode)
            .putBoolean(KEY_SHUFFLE_ENABLED, snapshot.shuffleEnabled)
            .apply()
    }

    fun savePosition(snapshot: PlaybackPositionSnapshot) {
        if (!preferences.contains(KEY_QUEUE)) return

        preferences.edit()
            .putInt(KEY_CURRENT_INDEX, snapshot.currentIndex)
            .putLong(KEY_POSITION_MS, snapshot.positionMs)
            .putInt(KEY_REPEAT_MODE, snapshot.repeatMode)
            .putBoolean(KEY_SHUFFLE_ENABLED, snapshot.shuffleEnabled)
            .apply()
    }

    fun restore(): RestoredPlaybackState? {
        val queueJson = preferences.getString(KEY_QUEUE, null) ?: return null
        val mediaItems =
            runCatching {
                val array = JSONArray(queueJson)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        val contentUri = item.optString(KEY_CONTENT_URI)
                        if (contentUri.isBlank()) continue

                        val folderPath = item.optString(KEY_FOLDER_PATH).takeIf(String::isNotBlank)
                        val extras =
                            Bundle().apply {
                                putString(MEDIA_EXTRA_CONTENT_URI, contentUri)
                                folderPath?.let { putString(MEDIA_EXTRA_FOLDER_PATH, it) }
                            }
                        val artworkUri =
                            item.optString(KEY_ARTWORK_URI)
                                .takeIf(String::isNotBlank)
                                ?.let(Uri::parse)
                        val metadata =
                            MediaMetadata.Builder()
                                .setTitle(item.optString(KEY_TITLE))
                                .setArtist(item.optString(KEY_ARTIST))
                                .setAlbumTitle(item.optString(KEY_ALBUM))
                                .setArtworkUri(artworkUri)
                                .setExtras(extras)
                                .build()

                        add(
                            MediaItem.Builder()
                                .setMediaId(item.optString(KEY_MEDIA_ID))
                                .setUri(Uri.parse(contentUri))
                                .setMediaMetadata(metadata)
                                .build(),
                        )
                    }
                }
            }.getOrElse {
                clear()
                return null
            }

        if (mediaItems.isEmpty()) {
            clear()
            return null
        }

        val currentIndex =
            preferences.getInt(KEY_CURRENT_INDEX, 0)
                .coerceIn(0, mediaItems.lastIndex)
        val repeatMode =
            preferences.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)
                .takeIf {
                    it == Player.REPEAT_MODE_OFF ||
                        it == Player.REPEAT_MODE_ONE ||
                        it == Player.REPEAT_MODE_ALL
                }
                ?: Player.REPEAT_MODE_OFF

        return RestoredPlaybackState(
            mediaItems = mediaItems,
            currentIndex = currentIndex,
            positionMs = preferences.getLong(KEY_POSITION_MS, 0L).coerceAtLeast(0L),
            repeatMode = repeatMode,
            shuffleEnabled = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false),
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "playback_restore_state"
        const val KEY_QUEUE = "queue"
        const val KEY_CURRENT_INDEX = "current_index"
        const val KEY_POSITION_MS = "position_ms"
        const val KEY_REPEAT_MODE = "repeat_mode"
        const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"

        const val KEY_MEDIA_ID = "media_id"
        const val KEY_CONTENT_URI = "content_uri"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_ALBUM = "album"
        const val KEY_ARTWORK_URI = "artwork_uri"
        const val KEY_FOLDER_PATH = "folder_path"
    }
}

fun Player.captureQueueSnapshot(): PlaybackQueueSnapshot {
    val items =
        buildList {
            for (index in 0 until mediaItemCount) {
                val item = getMediaItemAt(index)
                val metadata = item.mediaMetadata
                val contentUri =
                    metadata.extras
                        ?.getString(MEDIA_EXTRA_CONTENT_URI)
                        ?.takeIf(String::isNotBlank)
                        ?: continue

                add(
                    PersistedQueueItem(
                        mediaId = item.mediaId,
                        contentUri = contentUri,
                        title = metadata.title?.toString().orEmpty(),
                        artist = metadata.artist?.toString().orEmpty(),
                        album = metadata.albumTitle?.toString().orEmpty(),
                        artworkUri = metadata.artworkUri?.toString(),
                        folderPath = metadata.extras?.getString(MEDIA_EXTRA_FOLDER_PATH),
                    ),
                )
            }
        }
    val currentMediaId = currentMediaItem?.mediaId
    val persistedIndex =
        items.indexOfFirst { it.mediaId == currentMediaId }
            .takeIf { it != C.INDEX_UNSET }
            ?: 0

    return PlaybackQueueSnapshot(
        items = items,
        currentIndex = persistedIndex.coerceAtLeast(0),
        positionMs = currentPosition.coerceAtLeast(0L),
        repeatMode = repeatMode,
        shuffleEnabled = shuffleModeEnabled,
    )
}

fun Player.capturePositionSnapshot(): PlaybackPositionSnapshot =
    PlaybackPositionSnapshot(
        currentIndex = currentMediaItemIndex.coerceAtLeast(0),
        positionMs = currentPosition.coerceAtLeast(0L),
        repeatMode = repeatMode,
        shuffleEnabled = shuffleModeEnabled,
    )
