package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.ui.filterSongs

class BenchmarkLibraryFixtureTest {
    @Test
    fun fixtureProvidesDeterministicMixedTenThousandSongLibrary() {
        val result = createBenchmarkMusicLibrary()

        assertEquals(BENCHMARK_LIBRARY_SONG_COUNT, result.songs.size)
        assertEquals(BENCHMARK_LIBRARY_MEDIASTORE_COUNT, result.mediaStoreSongCount)
        assertEquals(BENCHMARK_LIBRARY_SAF_COUNT, result.authorizedFolderSongCount)
        assertEquals(result.songs.size, result.songs.map { it.id }.distinct().size)
        assertEquals(result.mediaStoreSongCount, result.songs.count { it.id > 0L })
        assertEquals(result.authorizedFolderSongCount, result.songs.count { it.id < 0L })
        assertTrue(result.songs.zipWithNext().all { (first, second) -> first.title < second.title })
    }

    @Test(timeout = 10_000L)
    fun tenThousandSongPipelinesAndFiveThousandSongPlaylistCompleteAsOneGate() {
        val result = createBenchmarkMusicLibrary()
        val songs = result.songs
        val stats =
            songs
                .filterIndexed { index, _ -> index % 4 == 0 }
                .associate { song ->
                    song.id.toString() to
                        PlaybackStats(
                            playCount = ((song.id and Long.MAX_VALUE) % 20L + 1L).toInt(),
                            lastPlayedAtMs = song.dateAddedMs,
                        )
                }

        val artistMatches = filterSongs(songs, "Benchmark Artist 042")
        val mostPlayed = resolveMostPlayedSongs(songs, stats)
        val unplayed = resolveUnplayedSongs(songs, stats)
        val merged =
            mergeMusicSources(
                mediaStoreSongs = songs.filter { it.id > 0L },
                authorizedFolderSongs = songs.filter { it.id < 0L },
            )
        val playlist =
            UserPlaylist(
                id = "benchmark-playlist",
                name = "Benchmark 5K",
                mediaIds = songs.take(5_000).map { it.id.toString() },
                createdAtMs = 1L,
                updatedAtMs = 1L,
            )
        val resolvedPlaylist = resolvePlaylistSongs(songs, playlist)
        val reordered =
            movePlaylistMediaIdToIndex(
                current = playlist.mediaIds,
                mediaId = playlist.mediaIds.first(),
                targetIndex = playlist.mediaIds.lastIndex,
            )
        val decoded = decodeUserPlaylists(encodeUserPlaylists(listOf(playlist)))

        assertEquals(40, artistMatches.size)
        assertEquals(2_500, mostPlayed.size)
        assertEquals(7_500, unplayed.size)
        assertEquals(BENCHMARK_LIBRARY_SONG_COUNT, merged.size)
        assertEquals(5_000, resolvedPlaylist.size)
        assertEquals(playlist.mediaIds.first(), reordered.last())
        assertEquals(playlist.mediaIds, decoded.single().mediaIds)
    }
}
