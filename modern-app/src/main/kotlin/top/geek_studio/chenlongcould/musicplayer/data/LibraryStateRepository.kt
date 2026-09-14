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
import top.geek_studio.chenlongcould.musicplayer.model.Song

private val Context.libraryStateDataStore by
    preferencesDataStore(name = "library_state")

class LibraryStateRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.libraryStateDataStore

    val favoriteMediaIds: Flow<Set<String>> =
        dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                preferences[FAVORITE_MEDIA_IDS]
                    .orEmpty()
                    .filter(String::isNotBlank)
                    .toSet()
            }

    val recentMediaIds: Flow<List<String>> =
        dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                decodeRecentMediaIds(preferences[RECENT_MEDIA_IDS])
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

    suspend fun recordPlayed(mediaId: String) {
        if (mediaId.isBlank()) return

        dataStore.edit { preferences ->
            val updated =
                recordRecentMediaId(
                    current = decodeRecentMediaIds(preferences[RECENT_MEDIA_IDS]),
                    mediaId = mediaId,
                    limit = MAX_RECENT_ITEMS,
                )
            preferences[RECENT_MEDIA_IDS] = encodeRecentMediaIds(updated)
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

internal fun resolveRecentSongs(
    songs: List<Song>,
    recentMediaIds: List<String>,
): List<Song> {
    if (songs.isEmpty() || recentMediaIds.isEmpty()) return emptyList()

    val songsByMediaId = songs.associateBy { it.id.toString() }
    return recentMediaIds.mapNotNull(songsByMediaId::get)
}

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

private const val RECENT_ID_SEPARATOR = "\n"
