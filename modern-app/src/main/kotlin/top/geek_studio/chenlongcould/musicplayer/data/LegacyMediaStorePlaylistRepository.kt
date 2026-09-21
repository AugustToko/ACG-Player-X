@file:Suppress("DEPRECATION")

package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.provider.BaseColumns
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LegacyMediaStorePlaylist(
    val id: Long,
    val name: String,
    val mediaIds: List<String>,
    val dateModifiedMs: Long = 0L,
)

class LegacyMediaStorePlaylistRepository(
    context: Context,
) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun loadPlaylists(): List<LegacyMediaStorePlaylist> = withContext(Dispatchers.IO) {
        runCatching(::queryPlaylists)
            .getOrElse(::throwReadableLegacyPlaylistError)
    }

    private fun queryPlaylists(): List<LegacyMediaStorePlaylist> {
        val metadata = mutableListOf<LegacyPlaylistMetadata>()
        resolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            PLAYLIST_PROJECTION,
            null,
            null,
            "${MediaStore.Audio.Playlists.NAME} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(BaseColumns._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME)
            val modifiedIndex = cursor.getColumnIndex(MediaStore.Audio.Playlists.DATE_MODIFIED)
            if (idIndex < 0) return@use

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (id <= 0L) continue
                metadata +=
                    LegacyPlaylistMetadata(
                        id = id,
                        name =
                            normalizeLegacyPlaylistName(
                                value = cursor.stringOrNull(nameIndex),
                                playlistId = id,
                            ),
                        dateModifiedMs =
                            legacySecondsToMillis(
                                cursor.longOrZero(modifiedIndex),
                            ),
                    )
            }
        }

        return metadata.map { item ->
            LegacyMediaStorePlaylist(
                id = item.id,
                name = item.name,
                mediaIds = queryPlaylistMembers(item.id),
                dateModifiedMs = item.dateModifiedMs,
            )
        }
    }

    private fun queryPlaylistMembers(playlistId: Long): List<String> {
        val audioIds = mutableListOf<Long>()
        val membersUri =
            MediaStore.Audio.Playlists.Members.getContentUri(
                LEGACY_EXTERNAL_VOLUME,
                playlistId,
            )
        resolver.query(
            membersUri,
            MEMBER_PROJECTION,
            null,
            null,
            "${MediaStore.Audio.Playlists.Members.PLAY_ORDER} ASC",
        )?.use { cursor ->
            val audioIdIndex =
                cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)
            if (audioIdIndex < 0) return@use
            while (cursor.moveToNext()) {
                audioIds += cursor.getLong(audioIdIndex)
            }
        }
        return normalizeLegacyPlaylistMediaIds(audioIds)
    }

    private fun throwReadableLegacyPlaylistError(throwable: Throwable): Nothing =
        when (throwable) {
            is SecurityException -> {
                throw IllegalStateException(
                    "读取旧系统歌单需要音乐媒体权限；授权后可再次尝试只读导入",
                    throwable,
                )
            }

            is IllegalArgumentException, is SQLiteException -> {
                throw IllegalStateException(
                    "当前系统或媒体提供程序没有公开旧 MediaStore 歌单；可继续使用 M3U/M3U8 导入",
                    throwable,
                )
            }

            else -> throw throwable
        }

    private data class LegacyPlaylistMetadata(
        val id: Long,
        val name: String,
        val dateModifiedMs: Long,
    )

    private companion object {
        const val LEGACY_EXTERNAL_VOLUME = "external"
        val PLAYLIST_PROJECTION =
            arrayOf(
                BaseColumns._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED,
            )
        val MEMBER_PROJECTION =
            arrayOf(
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER,
            )
    }
}

internal fun normalizeLegacyPlaylistMediaIds(audioIds: Iterable<Long>): List<String> =
    audioIds
        .asSequence()
        .filter { it > 0L }
        .map(Long::toString)
        .distinct()
        .toList()

internal fun normalizeLegacyPlaylistName(
    value: String?,
    playlistId: Long,
): String =
    value
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf(String::isNotEmpty)
        ?: "旧系统歌单 $playlistId"

internal fun legacySecondsToMillis(seconds: Long): Long {
    if (seconds <= 0L) return 0L
    return seconds
        .coerceAtMost(Long.MAX_VALUE / 1_000L)
        .times(1_000L)
}

private fun Cursor.stringOrNull(index: Int): String? =
    if (index >= 0 && !isNull(index)) getString(index) else null

private fun Cursor.longOrZero(index: Int): Long =
    if (index >= 0 && !isNull(index)) getLong(index) else 0L
