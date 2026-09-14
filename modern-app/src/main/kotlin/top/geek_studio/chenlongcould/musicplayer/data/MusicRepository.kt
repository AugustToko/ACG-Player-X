package top.geek_studio.chenlongcould.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.model.Song

class MusicRepository(
    private val context: Context,
) {
    private val collection: Uri
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val pathColumnName =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.RELATIVE_PATH
            } else {
                MediaStore.Audio.Media.DATA
            }
        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                pathColumnName,
            )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val pathColumn = cursor.getColumnIndex(pathColumnName)
            val usesRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn).orUnknown("未知曲目")
                    val artist = cursor.getString(artistColumn).orUnknown("未知艺术家")
                    val album = cursor.getString(albumColumn).orUnknown("未知专辑")
                    val duration = cursor.getLong(durationColumn).coerceAtLeast(0L)
                    val albumId = cursor.getLong(albumIdColumn)
                    val folder =
                        resolveMusicFolder(
                            rawPath =
                                if (pathColumn >= 0) {
                                    cursor.getString(pathColumn)
                                } else {
                                    null
                                },
                            isRelativePath = usesRelativePath,
                        )

                    add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            contentUri = ContentUris.withAppendedId(collection, id).toString(),
                            albumArtUri =
                                albumId
                                    .takeIf { it > 0L }
                                    ?.let {
                                        ContentUris.withAppendedId(
                                            Uri.parse(ALBUM_ART_BASE_URI),
                                            it,
                                        )
                                    }
                                    ?.toString(),
                            folderName = folder.name,
                            folderPath = folder.path,
                        ),
                    )
                }
            }
        } ?: emptyList()
    }

    fun observeChanges(onChanged: () -> Unit): AutoCloseable {
        val resolver = context.contentResolver
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    onChanged()
                }

                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?,
                ) {
                    onChanged()
                }
            }

        resolver.registerContentObserver(collection, true, observer)
        return AutoCloseable {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    private fun String?.orUnknown(fallback: String): String =
        this
            ?.takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }
            ?: fallback

    private companion object {
        const val ALBUM_ART_BASE_URI = "content://media/external/audio/albumart"
    }
}
