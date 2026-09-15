package top.geek_studio.chenlongcould.musicplayer.data

import top.geek_studio.chenlongcould.musicplayer.model.Song

internal const val BENCHMARK_LIBRARY_SONG_COUNT = 10_000
internal const val BENCHMARK_LIBRARY_MEDIASTORE_COUNT = 7_000
internal const val BENCHMARK_LIBRARY_SAF_COUNT =
    BENCHMARK_LIBRARY_SONG_COUNT - BENCHMARK_LIBRARY_MEDIASTORE_COUNT

internal fun createBenchmarkMusicLibrary(
    songCount: Int = BENCHMARK_LIBRARY_SONG_COUNT,
): MusicLibraryResult {
    require(songCount in 1..MAX_BENCHMARK_LIBRARY_SONG_COUNT) {
        "Benchmark library size must be between 1 and $MAX_BENCHMARK_LIBRARY_SONG_COUNT"
    }

    val mediaStoreSongCount = (songCount * MEDIASTORE_PERCENT) / 100
    val songs = ArrayList<Song>(songCount)

    repeat(songCount) { index ->
        val ordinal = index + 1
        val sequence = ordinal.toString().padStart(5, '0')
        val artistSequence = (index % ARTIST_COUNT).toString().padStart(3, '0')
        val albumSequence = (index % ALBUM_COUNT).toString().padStart(4, '0')
        val folderSequence = (index % FOLDER_COUNT).toString().padStart(2, '0')
        val isAuthorizedFolderSong = index >= mediaStoreSongCount
        val id = if (isAuthorizedFolderSong) -ordinal.toLong() else ordinal.toLong()
        val title = "Benchmark Song $sequence"
        val folderName = "Folder $folderSequence"
        val folderPath =
            if (isAuthorizedFolderSong) {
                "saf:benchmark:Benchmark SAF/$folderName"
            } else {
                "Music/Benchmark/$folderName"
            }

        songs +=
            Song(
                id = id,
                title = title,
                artist = "Benchmark Artist $artistSequence",
                album = "Benchmark Album $albumSequence",
                durationMs = BASE_DURATION_MS + (index % DURATION_VARIANTS) * 1_000L,
                contentUri =
                    "content://top.geek_studio.chenlongcould.musicplayer.benchmark/" +
                        (if (isAuthorizedFolderSong) "saf" else "mediastore") +
                        "/$ordinal",
                albumArtUri = null,
                folderName = folderName,
                folderPath = folderPath,
                dateAddedMs = BENCHMARK_DATE_EPOCH_MS - index * 60_000L,
                displayName = "$title.mp3",
            )
    }

    return MusicLibraryResult(
        songs = songs,
        warnings = emptyList(),
        mediaStoreSongCount = mediaStoreSongCount,
        authorizedFolderSongCount = songCount - mediaStoreSongCount,
    )
}

private const val MAX_BENCHMARK_LIBRARY_SONG_COUNT = 50_000
private const val MEDIASTORE_PERCENT = 70
private const val ARTIST_COUNT = 250
private const val ALBUM_COUNT = 1_000
private const val FOLDER_COUNT = 80
private const val DURATION_VARIANTS = 360
private const val BASE_DURATION_MS = 120_000L
private const val BENCHMARK_DATE_EPOCH_MS = 1_800_000_000_000L
