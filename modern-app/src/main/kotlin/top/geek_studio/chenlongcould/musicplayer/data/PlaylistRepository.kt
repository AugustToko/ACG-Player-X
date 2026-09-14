package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import top.geek_studio.chenlongcould.musicplayer.model.Song

private val Context.playlistDataStore by
    preferencesDataStore(name = "user_playlists")

data class UserPlaylist(
    val id: String,
    val name: String,
    val mediaIds: List<String> = emptyList(),
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

class PlaylistRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.playlistDataStore

    val playlists: Flow<List<UserPlaylist>> =
        dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                decodeUserPlaylists(preferences[PLAYLISTS])
            }

    suspend fun createPlaylist(
        name: String,
        nowMs: Long = System.currentTimeMillis(),
    ): UserPlaylist {
        val normalizedName = normalizePlaylistName(name)
        val createdAtMs = nowMs.coerceAtLeast(0L)
        val created =
            UserPlaylist(
                id = UUID.randomUUID().toString(),
                name = normalizedName,
                createdAtMs = createdAtMs,
                updatedAtMs = createdAtMs,
            )

        dataStore.edit { preferences ->
            val current = decodeUserPlaylists(preferences[PLAYLISTS])
            requirePlaylistNameAvailable(current, normalizedName)
            preferences[PLAYLISTS] =
                encodeUserPlaylists(sortUserPlaylists(listOf(created) + current))
        }
        return created
    }

    suspend fun renamePlaylist(
        playlistId: String,
        name: String,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val normalizedName = normalizePlaylistName(name)
        updatePlaylist(playlistId, nowMs) { playlists, target ->
            requirePlaylistNameAvailable(
                playlists = playlists,
                candidateName = normalizedName,
                ignoredPlaylistId = target.id,
            )
            target.copy(name = normalizedName)
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        if (playlistId.isBlank()) return
        dataStore.edit { preferences ->
            val current = decodeUserPlaylists(preferences[PLAYLISTS])
            val updated = current.filterNot { it.id == playlistId }
            if (updated.size != current.size) {
                preferences[PLAYLISTS] = encodeUserPlaylists(updated)
            }
        }
    }

    suspend fun addSongs(
        playlistId: String,
        mediaIds: Collection<String>,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val additions = mediaIds.filter(String::isNotBlank)
        if (additions.isEmpty()) return
        updatePlaylist(playlistId, nowMs) { _, target ->
            target.copy(mediaIds = appendPlaylistMediaIds(target.mediaIds, additions))
        }
    }

    suspend fun removeSong(
        playlistId: String,
        mediaId: String,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        if (mediaId.isBlank()) return
        updatePlaylist(playlistId, nowMs) { _, target ->
            target.copy(mediaIds = target.mediaIds.filterNot { it == mediaId })
        }
    }

    suspend fun swapSongs(
        playlistId: String,
        firstMediaId: String,
        secondMediaId: String,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        if (firstMediaId.isBlank() || secondMediaId.isBlank()) return
        updatePlaylist(playlistId, nowMs) { _, target ->
            target.copy(
                mediaIds =
                    swapPlaylistMediaIds(
                        current = target.mediaIds,
                        firstMediaId = firstMediaId,
                        secondMediaId = secondMediaId,
                    ),
            )
        }
    }

    suspend fun clearSongs(
        playlistId: String,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        updatePlaylist(playlistId, nowMs) { _, target ->
            target.copy(mediaIds = emptyList())
        }
    }

    suspend fun removeUnavailableSongs(
        playlistId: String,
        availableMediaIds: Set<String>,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        updatePlaylist(playlistId, nowMs) { _, target ->
            target.copy(mediaIds = target.mediaIds.filter(availableMediaIds::contains))
        }
    }

    private suspend fun updatePlaylist(
        playlistId: String,
        nowMs: Long,
        transform: (List<UserPlaylist>, UserPlaylist) -> UserPlaylist,
    ) {
        if (playlistId.isBlank()) return
        dataStore.edit { preferences ->
            val current = decodeUserPlaylists(preferences[PLAYLISTS])
            val target = current.firstOrNull { it.id == playlistId } ?: return@edit
            val transformed = transform(current, target)
            if (transformed == target) return@edit

            val updated =
                transformed.copy(
                    updatedAtMs = nextPlaylistTimestamp(target.updatedAtMs, nowMs),
                )
            preferences[PLAYLISTS] =
                encodeUserPlaylists(
                    sortUserPlaylists(
                        current.map { playlist ->
                            if (playlist.id == playlistId) updated else playlist
                        },
                    ),
                )
        }
    }

    private companion object {
        val PLAYLISTS = stringPreferencesKey("playlists_v1")
    }
}

internal fun normalizePlaylistName(value: String): String {
    val normalized =
        value
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_PLAYLIST_NAME_LENGTH)
    require(normalized.isNotEmpty()) { "歌单名称不能为空" }
    return normalized
}

internal fun appendPlaylistMediaIds(
    current: List<String>,
    additions: Collection<String>,
): List<String> =
    buildList {
        val seen = hashSetOf<String>()
        (current + additions)
            .asSequence()
            .filter(String::isNotBlank)
            .filter(seen::add)
            .forEach(::add)
    }

internal fun swapPlaylistMediaIds(
    current: List<String>,
    firstMediaId: String,
    secondMediaId: String,
): List<String> {
    val firstIndex = current.indexOf(firstMediaId)
    val secondIndex = current.indexOf(secondMediaId)
    if (firstIndex < 0 || secondIndex < 0 || firstIndex == secondIndex) return current

    return current.toMutableList().apply {
        val first = this[firstIndex]
        this[firstIndex] = this[secondIndex]
        this[secondIndex] = first
    }
}

internal fun resolvePlaylistSongs(
    songs: List<Song>,
    playlist: UserPlaylist,
): List<Song> {
    if (songs.isEmpty() || playlist.mediaIds.isEmpty()) return emptyList()
    val songsById = songs.associateBy { it.id.toString() }
    return playlist.mediaIds.mapNotNull(songsById::get)
}

internal fun countUnavailablePlaylistSongs(
    songs: List<Song>,
    playlist: UserPlaylist,
): Int {
    val availableIds = songs.asSequence().map { it.id.toString() }.toHashSet()
    return playlist.mediaIds.count { it !in availableIds }
}

internal fun encodeUserPlaylists(playlists: List<UserPlaylist>): String =
    playlists.joinToString(separator = ROW_SEPARATOR) { playlist ->
        listOf(
            escapePlaylistField(playlist.id),
            escapePlaylistField(playlist.name),
            playlist.createdAtMs.coerceAtLeast(0L).toString(),
            playlist.updatedAtMs.coerceAtLeast(0L).toString(),
            escapePlaylistField(playlist.mediaIds.joinToString(MEDIA_ID_SEPARATOR)),
        ).joinToString(FIELD_SEPARATOR)
    }

internal fun decodeUserPlaylists(encoded: String?): List<UserPlaylist> {
    if (encoded.isNullOrBlank()) return emptyList()

    val byId = linkedMapOf<String, UserPlaylist>()
    encoded.lineSequence().forEach { row ->
        val fields = row.split(FIELD_SEPARATOR)
        if (fields.size != PLAYLIST_FIELD_COUNT) return@forEach

        val id = unescapePlaylistField(fields[0]).trim()
        val name = unescapePlaylistField(fields[1]).trim()
        val createdAtMs = fields[2].toLongOrNull()?.coerceAtLeast(0L) ?: return@forEach
        val updatedAtMs = fields[3].toLongOrNull()?.coerceAtLeast(createdAtMs) ?: return@forEach
        if (id.isBlank() || name.isBlank()) return@forEach

        val mediaIds =
            unescapePlaylistField(fields[4])
                .split(MEDIA_ID_SEPARATOR)
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .toList()
        byId[id] =
            UserPlaylist(
                id = id,
                name = name.take(MAX_PLAYLIST_NAME_LENGTH),
                mediaIds = mediaIds,
                createdAtMs = createdAtMs,
                updatedAtMs = updatedAtMs,
            )
    }
    return sortUserPlaylists(byId.values.toList())
}

internal fun escapePlaylistField(value: String): String =
    buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
    }

internal fun unescapePlaylistField(value: String): String =
    buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character != '\\' || index == value.lastIndex) {
                append(character)
                index += 1
                continue
            }

            when (value[index + 1]) {
                '\\' -> append('\\')
                't' -> append('\t')
                'n' -> append('\n')
                'r' -> append('\r')
                else -> {
                    append('\\')
                    append(value[index + 1])
                }
            }
            index += 2
        }
    }

private fun requirePlaylistNameAvailable(
    playlists: List<UserPlaylist>,
    candidateName: String,
    ignoredPlaylistId: String? = null,
) {
    val normalizedCandidate = candidateName.lowercase(Locale.ROOT)
    require(
        playlists.none { playlist ->
            playlist.id != ignoredPlaylistId &&
                playlist.name.lowercase(Locale.ROOT) == normalizedCandidate
        },
    ) { "已存在同名歌单" }
}

private fun sortUserPlaylists(playlists: List<UserPlaylist>): List<UserPlaylist> =
    playlists.sortedWith(
        compareByDescending<UserPlaylist>(UserPlaylist::updatedAtMs)
            .thenByDescending(UserPlaylist::createdAtMs)
            .thenBy { it.name.lowercase(Locale.ROOT) }
            .thenBy(UserPlaylist::id),
    )

private fun nextPlaylistTimestamp(
    existingTimestampMs: Long,
    requestedTimestampMs: Long,
): Long {
    if (existingTimestampMs == Long.MAX_VALUE) return Long.MAX_VALUE
    return maxOf(requestedTimestampMs.coerceAtLeast(0L), existingTimestampMs + 1L)
}

private const val MAX_PLAYLIST_NAME_LENGTH = 80
private const val PLAYLIST_FIELD_COUNT = 5
private const val ROW_SEPARATOR = "\n"
private const val FIELD_SEPARATOR = "\t"
private const val MEDIA_ID_SEPARATOR = ","
