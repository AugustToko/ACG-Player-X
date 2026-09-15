package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.libraryStateDataStore by
    preferencesDataStore(name = "library_state")

data class PlaybackStats(
    val playCount: Int = 0,
    val lastPlayedAtMs: Long = 0L,
    val completedCount: Int = 0,
    val lastCompletedAtMs: Long = 0L,
    val totalListenTimeMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
)

class LibraryStateRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.libraryStateDataStore
    private val preferencesFlow =
        dataStore.data.catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    val favoriteMediaIds: Flow<Set<String>> =
        preferencesFlow.map { preferences ->
            preferences[FAVORITE_MEDIA_IDS]
                .orEmpty()
                .filter(String::isNotBlank)
                .toSet()
        }

    val recentMediaIds: Flow<List<String>> =
        preferencesFlow.map { preferences ->
            decodeRecentMediaIds(preferences[RECENT_MEDIA_IDS])
        }

    val playbackStats: Flow<Map<String, PlaybackStats>> =
        preferencesFlow.map { preferences ->
            decodePlaybackStats(
                preferences[PLAYBACK_STATS_V2] ?: preferences[PLAYBACK_STATS_V1],
            )
        }

    suspend fun toggleFavorite(mediaId: String) {
        if (mediaId.isBlank()) return

        dataStore.edit { preferences ->
            preferences[FAVORITE_MEDIA_IDS] =
                toggleFavoriteMediaIds(
                    current = preferences[FAVORITE_MEDIA_IDS].orEmpty(),
                    mediaId = mediaId,
                )
        }
    }

    suspend fun recordPlayed(
        mediaId: String,
        playedAtMs: Long = System.currentTimeMillis(),
    ) {
        if (mediaId.isBlank()) return

        dataStore.edit { preferences ->
            val recent =
                recordRecentMediaId(
                    current = decodeRecentMediaIds(preferences[RECENT_MEDIA_IDS]),
                    mediaId = mediaId,
                    limit = MAX_RECENT_ITEMS,
                )
            val currentStats =
                decodePlaybackStats(
                    preferences[PLAYBACK_STATS_V2] ?: preferences[PLAYBACK_STATS_V1],
                )
            val stats =
                updatePlaybackStats(
                    current = currentStats,
                    mediaId = mediaId,
                    playedAtMs = playedAtMs,
                )
            preferences[RECENT_MEDIA_IDS] = encodeRecentMediaIds(recent)
            preferences[PLAYBACK_STATS_V2] = encodePlaybackStats(stats)
            preferences.remove(PLAYBACK_STATS_V1)
        }
    }

    suspend fun recordProgress(update: PlaybackProgressUpdate) {
        if (update.mediaId.isBlank()) return
        if (update.listenedDeltaMs <= 0L && !update.completed) return

        dataStore.edit { preferences ->
            val current =
                decodePlaybackStats(
                    preferences[PLAYBACK_STATS_V2] ?: preferences[PLAYBACK_STATS_V1],
                )
            preferences[PLAYBACK_STATS_V2] =
                encodePlaybackStats(
                    updatePlaybackProgressStats(
                        current = current,
                        update = update,
                    ),
                )
            preferences.remove(PLAYBACK_STATS_V1)
        }
    }

    suspend fun clearRecent() {
        dataStore.edit { preferences ->
            preferences.remove(RECENT_MEDIA_IDS)
        }
    }

    private companion object {
        val FAVORITE_MEDIA_IDS = stringSetPreferencesKey("favorite_media_ids")
        val RECENT_MEDIA_IDS = stringPreferencesKey("recent_media_ids")
        val PLAYBACK_STATS_V1 = stringPreferencesKey("playback_stats_v1")
        val PLAYBACK_STATS_V2 = stringPreferencesKey("playback_stats_v2")
        const val MAX_RECENT_ITEMS = 100
    }
}
