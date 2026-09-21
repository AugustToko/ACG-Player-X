package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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

    /** Read the real DataStore snapshot; an I/O failure must not produce a fake default generation. */
    suspend fun readListeningDataGeneration(): ListeningDataGeneration =
        dataStore.data.first().listeningDataGeneration()

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

    // Direct callers create a new event now. Delayed service writes must pass their sampled generation.
    suspend fun recordPlayed(
        mediaId: String,
        playedAtMs: Long = System.currentTimeMillis(),
        expectedGeneration: ListeningDataGeneration? = null,
    ) {
        if (mediaId.isBlank()) return

        dataStore.edit { preferences ->
            val generation = preferences.listeningDataGeneration()
            if (generation.acceptsRecent(expectedGeneration)) {
                val recent =
                    recordRecentMediaId(
                        current = decodeRecentMediaIds(preferences[RECENT_MEDIA_IDS]),
                        mediaId = mediaId,
                        limit = MAX_RECENT_ITEMS,
                    )
                preferences[RECENT_MEDIA_IDS] = encodeRecentMediaIds(recent)
            }
            if (generation.acceptsStatistics(expectedGeneration)) {
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
                preferences[PLAYBACK_STATS_V2] = encodePlaybackStats(stats)
                preferences.remove(PLAYBACK_STATS_V1)
                preferences[PLAYBACK_STATS_SCHEMA_VERSION] = CURRENT_PLAYBACK_STATS_SCHEMA_VERSION
            }
        }
    }

    suspend fun recordProgress(
        update: PlaybackProgressUpdate,
        expectedGeneration: ListeningDataGeneration? = null,
    ) {
        if (update.mediaId.isBlank()) return
        if (update.listenedDeltaMs <= 0L && !update.completed) return

        dataStore.edit { preferences ->
            if (!preferences.listeningDataGeneration().acceptsStatistics(expectedGeneration)) return@edit
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
            if (mode == PlaybackStatisticsImportMode.REPLACE) {
                preferences[STATISTICS_GENERATION] = UUID.randomUUID().toString()
            }
        }
        return importedCount
    }

    suspend fun clearRecent() {
        clearListeningData(clearRecent = true, clearPlaybackStats = false)
    }

    suspend fun clearListeningData(
        clearRecent: Boolean,
        clearPlaybackStats: Boolean,
    ) {
        if (!clearRecent && !clearPlaybackStats) return

        dataStore.edit { preferences ->
            // Reset and invalidation are one transaction; a check outside edit would race with writes.
            if (clearRecent) {
                preferences.remove(RECENT_MEDIA_IDS)
                preferences[RECENT_GENERATION] = UUID.randomUUID().toString()
            }
            if (clearPlaybackStats) {
                preferences.remove(PLAYBACK_STATS_V1)
                preferences.remove(PLAYBACK_STATS_V2)
                preferences[PLAYBACK_STATS_SCHEMA_VERSION] = CURRENT_PLAYBACK_STATS_SCHEMA_VERSION
                preferences[STATISTICS_GENERATION] = UUID.randomUUID().toString()
            }
        }
    }

    private fun Preferences.listeningDataGeneration(): ListeningDataGeneration =
        ListeningDataGeneration(
            statistics = this[STATISTICS_GENERATION].orEmpty(),
            recent = this[RECENT_GENERATION].orEmpty(),
        )

    private companion object {
        val FAVORITE_MEDIA_IDS = stringSetPreferencesKey("favorite_media_ids")
        val RECENT_MEDIA_IDS = stringPreferencesKey("recent_media_ids")
        val PLAYBACK_STATS_V1 = stringPreferencesKey("playback_stats_v1")
        val PLAYBACK_STATS_V2 = stringPreferencesKey("playback_stats_v2")
        val PLAYBACK_STATS_SCHEMA_VERSION = intPreferencesKey("playback_stats_schema_version")
        val STATISTICS_GENERATION = stringPreferencesKey("playback_stats_generation")
        val RECENT_GENERATION = stringPreferencesKey("recent_media_generation")
        const val CURRENT_PLAYBACK_STATS_SCHEMA_VERSION = 2
        const val MAX_RECENT_ITEMS = 100
    }
}
