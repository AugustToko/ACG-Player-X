package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

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
        preferencesFlow
            .onStart { ensurePlaybackStatsSchema() }
            .map { preferences ->
                decodePlaybackStats(
                    preferences[PLAYBACK_STATS_V2] ?: preferences[PLAYBACK_STATS_V1],
                )
            }

    suspend fun ensurePlaybackStatsSchema() {
        dataStore.edit { preferences ->
            val storedVersion = preferences[PLAYBACK_STATS_SCHEMA_VERSION] ?: 0
            if (storedVersion >= CURRENT_PLAYBACK_STATS_SCHEMA_VERSION) return@edit

            val stats =
                decodePlaybackStats(
                    preferences[PLAYBACK_STATS_V2] ?: preferences[PLAYBACK_STATS_V1],
                )
            if (stats.isEmpty()) {
                preferences.remove(PLAYBACK_STATS_V2)
            } else {
                preferences[PLAYBACK_STATS_V2] = encodePlaybackStats(stats)
            }
            preferences.remove(PLAYBACK_STATS_V1)
            preferences[PLAYBACK_STATS_SCHEMA_VERSION] = CURRENT_PLAYBACK_STATS_SCHEMA_VERSION
        }
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
            preferences[PLAYBACK_STATS_SCHEMA_VERSION] = CURRENT_PLAYBACK_STATS_SCHEMA_VERSION
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
            preferences[PLAYBACK_STATS_SCHEMA_VERSION] = CURRENT_PLAYBACK_STATS_SCHEMA_VERSION
        }
    }

    suspend fun importPlaybackStatistics(
        imported: Map<String, PlaybackStats>,
        mode: PlaybackStatisticsImportMode,
    ): Int {
        if (imported.isEmpty()) return 0

        var importedCount = 0
        dataStore.edit { preferences ->
            val current =
                decodePlaybackStats(
                    preferences[PLAYBACK_STATS_V2] ?: preferences[PLAYBACK_STATS_V1],
                )
            val updated =
                applyPlaybackStatisticsImport(
                    current = current,
                    imported = imported,
                    mode = mode,
                )
            importedCount = imported.size
            if (updated.isEmpty()) {
                preferences.remove(PLAYBACK_STATS_V2)
            } else {
                preferences[PLAYBACK_STATS_V2] = encodePlaybackStats(updated)
            }
            preferences.remove(PLAYBACK_STATS_V1)
            preferences[PLAYBACK_STATS_SCHEMA_VERSION] = CURRENT_PLAYBACK_STATS_SCHEMA_VERSION
        }
        return importedCount
    }

    suspend fun clearRecent() {
        dataStore.edit { preferences ->
            preferences.remove(RECENT_MEDIA_IDS)
        }
    }

    suspend fun clearListeningData(
        clearRecent: Boolean,
        clearPlaybackStats: Boolean,
    ) {
        if (!clearRecent && !clearPlaybackStats) return

        dataStore.edit { preferences ->
            if (clearRecent) {
                preferences.remove(RECENT_MEDIA_IDS)
            }
            if (clearPlaybackStats) {
                preferences.remove(PLAYBACK_STATS_V1)
                preferences.remove(PLAYBACK_STATS_V2)
                preferences[PLAYBACK_STATS_SCHEMA_VERSION] = CURRENT_PLAYBACK_STATS_SCHEMA_VERSION
            }
        }
    }

    private companion object {
        val FAVORITE_MEDIA_IDS = stringSetPreferencesKey("favorite_media_ids")
        val RECENT_MEDIA_IDS = stringPreferencesKey("recent_media_ids")
        val PLAYBACK_STATS_V1 = stringPreferencesKey("playback_stats_v1")
        val PLAYBACK_STATS_V2 = stringPreferencesKey("playback_stats_v2")
        val PLAYBACK_STATS_SCHEMA_VERSION = intPreferencesKey("playback_stats_schema_version")
        const val CURRENT_PLAYBACK_STATS_SCHEMA_VERSION = 2
        const val MAX_RECENT_ITEMS = 100
    }
}
