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

    private fun song(id: Long): Song =
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
        )
}
