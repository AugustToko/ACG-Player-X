package top.geek_studio.chenlongcould.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.model.Song

data class MusicLibraryResult(
    val songs: List<Song>,
    val warnings: List<String>,
    val mediaStoreSongCount: Int,
    val authorizedFolderSongCount: Int,
)

class MusicRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val documentTreeScanner = DocumentTreeMusicScanner(appContext)
    private val authorizedFolderCache = linkedMapOf<String, DocumentTreeScanResult>()
    private val cacheLock = Any()

    private val collection: Uri
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

    suspend fun loadSongs(): List<Song> =
        loadSongs(
            includeMediaStore = true,
            authorizedFolders = emptyList(),
            refreshAuthorizedFolders = false,
        ).songs

    suspend fun loadSongs(
        includeMediaStore: Boolean,
        authorizedFolders: List<AuthorizedFolder>,
        refreshAuthorizedFolders: Boolean,
    ): MusicLibraryResult = withContext(Dispatchers.IO) {
        val warnings = mutableListOf<String>()
        val mediaStoreSongs =
            if (includeMediaStore) {
                runCatching { loadMediaStoreSongs() }
                    .onFailure { throwable ->
                        warnings +=
                            "系统媒体库读取失败：" +
                                (throwable.localizedMessage ?: "未知错误")
                    }
                    .getOrDefault(emptyList())
            } else {
                emptyList()
            }

        val activeFolderUris =
            authorizedFolders
                .filter(AuthorizedFolder::isAvailable)
                .map(AuthorizedFolder::uriString)
                .toSet()
        synchronized(cacheLock) {
            authorizedFolderCache.keys.retainAll(activeFolderUris)
        }

        val authorizedSongs = mutableListOf<Song>()
        authorizedFolders.forEach { folder ->
            val result =
                if (!folder.isAvailable) {
                    documentTreeScanner.scan(folder)
                } else {
                    val cached =
                        if (refreshAuthorizedFolders) {
                            null
                        } else {
                            synchronized(cacheLock) {
                                authorizedFolderCache[folder.uriString]
                            }
                        }
                    cached ?: documentTreeScanner.scan(folder).also { scanned ->
                        synchronized(cacheLock) {
                            authorizedFolderCache[folder.uriString] = scanned
                        }
                    }
                }

            authorizedSongs += result.songs
            warnings += result.warnings
        }

        val mergedSongs = mergeMusicSources(mediaStoreSongs, authorizedSongs)
        MusicLibraryResult(
            songs = mergedSongs,
            warnings = warnings.distinct(),
            mediaStoreSongCount = mediaStoreSongs.size,
            authorizedFolderSongCount = mergedSongs.count { it.id < 0L },
        )
    }

    fun observeChanges(onChanged: () -> Unit): AutoCloseable {
        val resolver = appContext.contentResolver
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

    private fun loadMediaStoreSongs(): List<Song> {
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
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                pathColumnName,
            )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        return appContext.contentResolver.query(
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
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
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
                    val displayName =
                        if (displayNameColumn >= 0) {
                            cursor.getString(displayNameColumn).orUnknown(title)
                        } else {
                            title
                        }
                    val dateAddedSeconds =
                        if (dateAddedColumn >= 0) cursor.getLong(dateAddedColumn) else 0L
                    val dateModifiedSeconds =
                        if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
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
                            dateAddedMs =
                                resolveMediaStoreDateMs(
                                    dateAddedSeconds = dateAddedSeconds,
                                    dateModifiedSeconds = dateModifiedSeconds,
                                ),
                            displayName = displayName,
                        ),
                    )
                }
            }
        } ?: emptyList()
    }

    private fun String?.orUnknown(fallback: String): String =
        this
            ?.takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }
            ?: fallback

    private companion object {
        const val ALBUM_ART_BASE_URI = "content://media/external/audio/albumart"
    }
}

internal fun resolveMediaStoreDateMs(
    dateAddedSeconds: Long,
    dateModifiedSeconds: Long,
): Long {
    val seconds =
        dateAddedSeconds
            .takeIf { it > 0L }
            ?: dateModifiedSeconds.coerceAtLeast(0L)
    return if (seconds == 0L || seconds > Long.MAX_VALUE / 1_000L) {
        0L
    } else {
        seconds * 1_000L
    }
}

internal fun mergeMusicSources(
    mediaStoreSongs: List<Song>,
    authorizedFolderSongs: List<Song>,
): List<Song> {
    val result = mutableListOf<Song>()
    val seenUris = hashSetOf<String>()
    val mediaStoreFingerprints = hashSetOf<String>()

    mediaStoreSongs.forEach { song ->
        if (seenUris.add(song.contentUri)) {
            result += song
            mediaStoreFingerprints += song.sourceFingerprint()
        }
    }

    authorizedFolderSongs.forEach { song ->
        if (!seenUris.add(song.contentUri)) return@forEach
        if (song.sourceFingerprint() in mediaStoreFingerprints) return@forEach
        result += song
    }

    return result.sortedWith(
        compareBy<Song> { it.title.lowercase(Locale.ROOT) }
            .thenBy { it.artist.lowercase(Locale.ROOT) }
            .thenBy(Song::id),
    )
}

private fun Song.sourceFingerprint(): String =
    listOf(
        title.trim().lowercase(Locale.ROOT),
        artist.trim().lowercase(Locale.ROOT),
        album.trim().lowercase(Locale.ROOT),
        (durationMs / 1_000L).toString(),
    ).joinToString("|")
