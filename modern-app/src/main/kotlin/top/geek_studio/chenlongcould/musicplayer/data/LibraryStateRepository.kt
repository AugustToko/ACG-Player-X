package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import top.geek_studio.chenlongcould.musicplayer.model.Song

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

internal fun toggleFavoriteMediaIds(
    current: Set<String>,
    mediaId: String,
): Set<String> =
    if (mediaId in current) {
        current - mediaId
    } else {
        current + mediaId
    }

internal fun recordRecentMediaId(
    current: List<String>,
    mediaId: String,
    limit: Int,
): List<String> {
    if (mediaId.isBlank() || limit <= 0) return emptyList()

    return buildList {
        add(mediaId)
        current
            .asSequence()
            .filter { it.isNotBlank() && it != mediaId }
            .take(limit - 1)
            .forEach(::add)
    }
}

internal fun updatePlaybackStats(
    current: Map<String, PlaybackStats>,
    mediaId: String,
    playedAtMs: Long,
): Map<String, PlaybackStats> {
    if (mediaId.isBlank()) return current

    val existing = current[mediaId] ?: PlaybackStats()
    val normalizedPlayedAtMs = playedAtMs.coerceAtLeast(0L)
    val shouldIncrement =
        existing.playCount == 0 ||
            normalizedPlayedAtMs >=
            saturatingAdd(existing.lastPlayedAtMs, PLAY_START_DEDUP_WINDOW_MS)
    val updatedCount =
        when {
            !shouldIncrement -> existing.playCount
            existing.playCount == Int.MAX_VALUE -> Int.MAX_VALUE
            else -> existing.playCount + 1
        }
    return current +
        (mediaId to
            existing.copy(
                playCount = updatedCount,
                lastPlayedAtMs = maxOf(existing.lastPlayedAtMs, normalizedPlayedAtMs),
            ))
}

internal fun resolveRecentSongs(
    songs: List<Song>,
    recentMediaIds: List<String>,
): List<Song> {
    if (songs.isEmpty() || recentMediaIds.isEmpty()) return emptyList()

    val songsByMediaId = songs.associateBy { it.id.toString() }
    return recentMediaIds.mapNotNull(songsByMediaId::get)
}

internal fun resolveRecentlyAddedSongs(songs: List<Song>): List<Song> =
    songs.sortedWith(
        compareByDescending<Song>(Song::dateAddedMs)
            .thenBy { it.title.lowercase(Locale.ROOT) }
            .thenBy { it.artist.lowercase(Locale.ROOT) }
            .thenBy(Song::id),
    )

internal fun resolveMostPlayedSongs(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
): List<Song> =
    songs
        .filter { song ->
            (playbackStats[song.id.toString()]?.playCount ?: 0) > 0
        }
        .sortedWith(
            compareByDescending<Song> { song ->
                playbackStats[song.id.toString()]?.playCount ?: 0
            }.thenByDescending { song ->
                playbackStats[song.id.toString()]?.lastPlayedAtMs ?: 0L
            }.thenBy { song ->
                song.title.lowercase(Locale.ROOT)
            }.thenBy(Song::id),
        )

internal fun resolveUnplayedSongs(
    songs: List<Song>,
    playbackStats: Map<String, PlaybackStats>,
): List<Song> =
    resolveRecentlyAddedSongs(
        songs.filter { song ->
            (playbackStats[song.id.toString()]?.playCount ?: 0) <= 0
        },
    )

internal fun encodePlaybackStats(stats: Map<String, PlaybackStats>): String =
    stats
        .asSequence()
        .filter { (mediaId, value) ->
            mediaId.isNotBlank() &&
                (value.playCount > 0 ||
                    value.completedCount > 0 ||
                    value.totalListenTimeMs > 0L ||
                    value.lastPositionMs > 0L)
        }
        .sortedBy { (mediaId, _) -> mediaId }
        .joinToString(separator = ROW_SEPARATOR) { (mediaId, value) ->
            listOf(
                mediaId,
                value.playCount.coerceAtLeast(0).toString(),
                value.lastPlayedAtMs.coerceAtLeast(0L).toString(),
                value.completedCount.coerceAtLeast(0).toString(),
                value.lastCompletedAtMs.coerceAtLeast(0L).toString(),
                value.totalListenTimeMs.coerceAtLeast(0L).toString(),
                value.lastPositionMs.coerceAtLeast(0L).toString(),
                value.durationMs.coerceAtLeast(0L).toString(),
            ).joinToString(FIELD_SEPARATOR)
        }

internal fun decodePlaybackStats(encoded: String?): Map<String, PlaybackStats> =
    encoded
        ?.lineSequence()
        ?.mapNotNull { row ->
            val fields = row.split(FIELD_SEPARATOR)
            if (fields.size < 3) return@mapNotNull null

            val mediaId = fields[0].trim()
            val playCount = fields[1].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
            val lastPlayedAtMs = fields[2].toLongOrNull()?.coerceAtLeast(0L) ?: return@mapNotNull null
            val completedCount = fields.getOrNull(3)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val lastCompletedAtMs = fields.getOrNull(4)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val totalListenTimeMs = fields.getOrNull(5)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val lastPositionMs = fields.getOrNull(6)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val durationMs = fields.getOrNull(7)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            if (
                mediaId.isBlank() ||
                (playCount == 0 &&
                    completedCount == 0 &&
                    totalListenTimeMs == 0L &&
                    lastPositionMs == 0L)
            ) {
                return@mapNotNull null
            }
            mediaId to
                PlaybackStats(
                    playCount = playCount,
                    lastPlayedAtMs = lastPlayedAtMs,
                    completedCount = completedCount,
                    lastCompletedAtMs = lastCompletedAtMs,
                    totalListenTimeMs = totalListenTimeMs,
                    lastPositionMs = lastPositionMs,
                    durationMs = durationMs,
                )
        }
        ?.toMap()
        .orEmpty()

private fun encodeRecentMediaIds(mediaIds: List<String>): String =
    mediaIds.joinToString(separator = RECENT_ID_SEPARATOR)

private fun decodeRecentMediaIds(encoded: String?): List<String> =
    encoded
        ?.split(RECENT_ID_SEPARATOR)
        ?.asSequence()
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.distinct()
        ?.take(100)
        ?.toList()
        .orEmpty()

internal fun saturatingAdd(
    left: Long,
    right: Long,
): Long {
    val safeLeft = left.coerceAtLeast(0L)
    val safeRight = right.coerceAtLeast(0L)
    return if (Long.MAX_VALUE - safeLeft < safeRight) Long.MAX_VALUE else safeLeft + safeRight
}

private const val RECENT_ID_SEPARATOR = "\n"
private const val ROW_SEPARATOR = "\n"
private const val FIELD_SEPARATOR = "\t"
private const val PLAY_START_DEDUP_WINDOW_MS = 3_000L
