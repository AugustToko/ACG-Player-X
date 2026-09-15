package top.geek_studio.chenlongcould.musicplayer.widget

import android.content.Context
import androidx.media3.common.Player

data class PlaybackWidgetState(
    val mediaId: String? = null,
    val title: String = DEFAULT_TITLE,
    val artist: String = DEFAULT_ARTIST,
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
) {
    val hasMedia: Boolean
        get() = !mediaId.isNullOrBlank()

    companion object {
        const val DEFAULT_TITLE = "尚未播放"
        const val DEFAULT_ARTIST = "打开 ACG Player X 选择歌曲"
    }
}

class PlaybackWidgetStateStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun read(): PlaybackWidgetState =
        PlaybackWidgetState(
            mediaId = preferences.getString(KEY_MEDIA_ID, null)?.takeIf(String::isNotBlank),
            title =
                preferences.getString(KEY_TITLE, null)
                    ?.takeIf(String::isNotBlank)
                    ?: PlaybackWidgetState.DEFAULT_TITLE,
            artist =
                preferences.getString(KEY_ARTIST, null)
                    ?.takeIf(String::isNotBlank)
                    ?: PlaybackWidgetState.DEFAULT_ARTIST,
            isPlaying = preferences.getBoolean(KEY_IS_PLAYING, false),
            hasPrevious = preferences.getBoolean(KEY_HAS_PREVIOUS, false),
            hasNext = preferences.getBoolean(KEY_HAS_NEXT, false),
        )

    fun write(state: PlaybackWidgetState) {
        preferences.edit()
            .putString(KEY_MEDIA_ID, state.mediaId)
            .putString(KEY_TITLE, state.title)
            .putString(KEY_ARTIST, state.artist)
            .putBoolean(KEY_IS_PLAYING, state.isPlaying)
            .putBoolean(KEY_HAS_PREVIOUS, state.hasPrevious)
            .putBoolean(KEY_HAS_NEXT, state.hasNext)
            .apply()
    }

    internal fun clear() {
        preferences.edit().clear().commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "playback_widget_state"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_IS_PLAYING = "is_playing"
        const val KEY_HAS_PREVIOUS = "has_previous"
        const val KEY_HAS_NEXT = "has_next"
    }
}

internal fun Player.toPlaybackWidgetState(): PlaybackWidgetState {
    val mediaId = currentMediaItem?.mediaId?.takeIf(String::isNotBlank)
    if (mediaId == null) return PlaybackWidgetState()

    return PlaybackWidgetState(
        mediaId = mediaId,
        title = normalizeWidgetText(mediaMetadata.title, "未知曲目"),
        artist = normalizeWidgetText(mediaMetadata.artist, "未知艺术家"),
        isPlaying = isPlaying,
        hasPrevious = hasPreviousMediaItem(),
        hasNext = hasNextMediaItem(),
    )
}

internal fun normalizeWidgetText(
    value: CharSequence?,
    fallback: String,
): String =
    value
        ?.toString()
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.take(MAX_WIDGET_TEXT_LENGTH)
        ?.takeIf(String::isNotEmpty)
        ?: fallback

private const val MAX_WIDGET_TEXT_LENGTH = 96
