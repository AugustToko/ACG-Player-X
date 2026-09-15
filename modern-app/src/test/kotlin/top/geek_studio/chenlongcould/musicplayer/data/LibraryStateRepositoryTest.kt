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
    fun playbackStatsDeduplicateParallelStartSignalsAndKeepLatestTimestamp() {
        val first = updatePlaybackStats(emptyMap(), "7", 1_000L)
        val duplicate = updatePlaybackStats(first, "7", 2_000L)
        val secondSession = updatePlaybackStats(duplicate, "7", 5_000L)
        val staleTimestamp = updatePlaybackStats(secondSession, "7", 1_500L)

        assertEquals(2, staleTimestamp.getValue("7").playCount)
        assertEquals(5_000L, staleTimestamp.getValue("7").lastPlayedAtMs)
    }

    @Test
    fun playbackStatsRoundTripAllCompletionFields() {
        val stats =
            mapOf(
                "1" to
                    PlaybackStats(
                        playCount = 4,
                        lastPlayedAtMs = 8_000L,
                        completedCount = 2,
                        lastCompletedAtMs = 7_500L,
                        totalListenTimeMs = 600_000L,
                        lastPositionMs = 45_000L,
                        durationMs = 180_000L,
                    ),
                "-2" to PlaybackStats(playCount = 1, lastPlayedAtMs = 9_000L),
            )
        val encoded = encodePlaybackStats(stats) + "\ninvalid"

        assertEquals(stats, decodePlaybackStats(encoded))
    }

    @Test
    fun playbackStatsDecodeLegacyThreeFieldRows() {
        val decoded = decodePlaybackStats("7\t3\t9000")

        assertEquals(
            PlaybackStats(playCount = 3, lastPlayedAtMs = 9_000L),
            decoded.getValue("7"),
        )
    }

    @Test
    fun progressUpdateStoresMeaningfulPositionAndCompletionClearsIt() {
        val inProgress =
            updatePlaybackProgressStats(
                current = emptyMap(),
                update =
                    PlaybackProgressUpdate(
                        mediaId = "9",
                        positionMs = 45_000L,
                        durationMs = 180_000L,
                        listenedDeltaMs = 15_000L,
                        sessionListenedMs = 15_000L,
                        completed = false,
                        recordedAtMs = 10_000L,
                    ),
            )
        val first = inProgress.getValue("9")
        assertEquals(45_000L, first.lastPositionMs)
        assertEquals(15_000L, first.totalListenTimeMs)
        assertEquals(25, first.progressPercent())
        assertTrue(first.isInProgress())

        val completed =
            updatePlaybackProgressStats(
                current = inProgress,
                update =
                    PlaybackProgressUpdate(
                        mediaId = "9",
                        positionMs = 170_000L,
                        durationMs = 180_000L,
                        listenedDeltaMs = 90_000L,
                        sessionListenedMs = 105_000L,
                        completed = true,
                        recordedAtMs = 20_000L,
                    ),
            ).getValue("9")

        assertEquals(1, completed.completedCount)
        assertEquals(20_000L, completed.lastCompletedAtMs)
        assertEquals(0L, completed.lastPositionMs)
        assertEquals(105_000L, completed.totalListenTimeMs)
        assertFalse(completed.isInProgress())
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
    fun inProgressAndCompletedSongsUseIndependentSessionState() {
        val songs = listOf(song(1), song(2), song(3))
        val stats =
            mapOf(
                "1" to PlaybackStats(lastPlayedAtMs = 8_000L, lastPositionMs = 90_000L, durationMs = 180_000L),
                "2" to PlaybackStats(completedCount = 2, lastCompletedAtMs = 9_000L),
                "3" to PlaybackStats(lastPlayedAtMs = 7_000L, lastPositionMs = 1_000L, durationMs = 180_000L),
            )

        assertEquals(listOf(1L), resolveInProgressSongs(songs, stats).map(Song::id))
        assertEquals(listOf(2L), resolveCompletedSongs(songs, stats).map(Song::id))
    }

    @Test
    fun recentlyPlayedWindowExcludesOldAndFutureEntries() {
        val now = 10L * 24L * 60L * 60L * 1_000L
        val songs = listOf(song(1), song(2), song(3))
        val stats =
            mapOf(
                "1" to PlaybackStats(lastPlayedAtMs = now - 1_000L),
                "2" to PlaybackStats(lastPlayedAtMs = now - RECENT_PLAY_WINDOW_MS - 1L),
                "3" to PlaybackStats(lastPlayedAtMs = now + 1L),
            )

        assertEquals(
            listOf(1L),
            resolveRecentlyPlayedWithin(songs, stats, now).map(Song::id),
        )
    }

    @Test
    fun longFormSongsSortByDuration() {
        val songs =
            listOf(
                song(1, durationMs = 1_200_000L),
                song(2, durationMs = 3_600_000L),
                song(3, durationMs = 600_000L),
            )

        assertEquals(
            listOf(2L, 1L),
            resolveLongFormSongs(songs).map(Song::id),
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
        durationMs: Long = 180_000L,
    ): Song =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            durationMs = durationMs,
            contentUri = "content://test/$id",
            albumArtUri = null,
            folderName = "Music",
            folderPath = "Music/",
            dateAddedMs = dateAddedMs,
        )
}
