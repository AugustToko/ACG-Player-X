package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class LibraryStateRepositoryTest {
    @Test
    fun toggleFavoriteAddsAndRemovesMediaId() {
        val added = toggleFavoriteMediaIds(setOf("1"), "2")
        assertEquals(setOf("1", "2"), added)

        val removed = toggleFavoriteMediaIds(added, "1")
        assertFalse("1" in removed)
        assertTrue("2" in removed)
    }

    @Test
    fun recentPlaybackMovesExistingItemToFrontWithoutDuplicates() {
        val updated =
            recordRecentMediaId(
                current = listOf("1", "2", "3", "2"),
                mediaId = "2",
                limit = 10,
            )

        assertEquals(listOf("2", "1", "3"), updated)
    }

    @Test
    fun recentPlaybackHonorsConfiguredLimit() {
        val updated =
            recordRecentMediaId(
                current = listOf("1", "2", "3", "4"),
                mediaId = "5",
                limit = 3,
            )

        assertEquals(listOf("5", "1", "2"), updated)
    }

    @Test
    fun recentSongsFollowHistoryOrderAndIgnoreUnavailableEntries() {
        val songs = listOf(song(1), song(2), song(3))

        val resolved =
            resolveRecentSongs(
                songs = songs,
                recentMediaIds = listOf("3", "99", "1"),
            )

        assertEquals(listOf(3L, 1L), resolved.map(Song::id))
    }

    @Test
    fun playbackStatsIncrementAndKeepLatestTimestamp() {
        val first = updatePlaybackStats(emptyMap(), "7", 1_000L)
        val second = updatePlaybackStats(first, "7", 2_000L)
        val staleTimestamp = updatePlaybackStats(second, "7", 1_500L)

        assertEquals(3, staleTimestamp.getValue("7").playCount)
        assertEquals(2_000L, staleTimestamp.getValue("7").lastPlayedAtMs)
    }

    @Test
    fun playbackStatsRoundTripAndIgnoreInvalidRows() {
        val stats =
            mapOf(
                "1" to PlaybackStats(playCount = 4, lastPlayedAtMs = 8_000L),
                "-2" to PlaybackStats(playCount = 1, lastPlayedAtMs = 9_000L),
            )
        val encoded = encodePlaybackStats(stats) + "\ninvalid"

        assertEquals(stats, decodePlaybackStats(encoded))
    }

    @Test
    fun mostPlayedSongsSortByCountThenLastPlayedTime() {
        val songs = listOf(song(1), song(2), song(3))
        val stats =
            mapOf(
                "1" to PlaybackStats(2, 5_000L),
                "2" to PlaybackStats(5, 1_000L),
                "3" to PlaybackStats(5, 8_000L),
            )

        assertEquals(
            listOf(3L, 2L, 1L),
            resolveMostPlayedSongs(songs, stats).map(Song::id),
        )
    }

    @Test
    fun unplayedSongsExcludePlayedItemsAndPreferNewestSongs() {
        val songs =
            listOf(
                song(1, dateAddedMs = 1_000L),
                song(2, dateAddedMs = 3_000L),
                song(3, dateAddedMs = 2_000L),
            )
        val stats = mapOf("2" to PlaybackStats(1, 4_000L))

        assertEquals(
            listOf(3L, 1L),
            resolveUnplayedSongs(songs, stats).map(Song::id),
        )
    }

    @Test
    fun recentlyAddedSongsSortUnknownDatesLast() {
        val songs =
            listOf(
                song(1, dateAddedMs = 0L),
                song(2, dateAddedMs = 3_000L),
                song(3, dateAddedMs = 2_000L),
            )

        assertEquals(
            listOf(2L, 3L, 1L),
            resolveRecentlyAddedSongs(songs).map(Song::id),
        )
    }

    private fun song(
        id: Long,
        dateAddedMs: Long = 0L,
    ): Song =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            contentUri = "content://test/$id",
            albumArtUri = null,
            folderName = "Music",
            folderPath = "Music/",
            dateAddedMs = dateAddedMs,
        )
}
