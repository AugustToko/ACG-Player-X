package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class PlaylistRepositoryTest {
    @Test
    fun codecRoundTripsEscapedNamesAndMediaOrder() {
        val source =
            listOf(
                UserPlaylist(
                    id = "playlist-1",
                    name = "Anime\\Mix\t精选\n2026",
                    mediaIds = listOf("3", "-7", "99"),
                    createdAtMs = 100L,
                    updatedAtMs = 200L,
                ),
            )

        val decoded = decodeUserPlaylists(encodeUserPlaylists(source))

        assertEquals(source, decoded)
    }

    @Test
    fun appendSongsDeduplicatesWithoutChangingExistingOrder() {
        val updated =
            appendPlaylistMediaIds(
                current = listOf("1", "2"),
                additions = listOf("2", "3", "", "1", "4"),
            )

        assertEquals(listOf("1", "2", "3", "4"), updated)
    }

    @Test
    fun swappingSongsUsesStoredOrder() {
        val updated =
            swapPlaylistMediaIds(
                current = listOf("1", "2", "3"),
                firstMediaId = "1",
                secondMediaId = "3",
            )

        assertEquals(listOf("3", "2", "1"), updated)
    }

    @Test
    fun resolvingPlaylistPreservesOrderAndIgnoresUnavailableSongs() {
        val playlist =
            UserPlaylist(
                id = "playlist",
                name = "Test",
                mediaIds = listOf("3", "99", "1"),
                createdAtMs = 1L,
                updatedAtMs = 1L,
            )
        val songs = listOf(song(1), song(2), song(3))

        val resolved = resolvePlaylistSongs(songs, playlist)

        assertEquals(listOf(3L, 1L), resolved.map(Song::id))
        assertEquals(1, countUnavailablePlaylistSongs(songs, playlist))
    }

    @Test
    fun decoderSkipsMalformedRowsAndDuplicateMediaIds() {
        val valid = "id\tName\t1\t2\t1,1,2"
        val decoded = decodeUserPlaylists("broken\n$valid")

        assertEquals(1, decoded.size)
        assertEquals(listOf("1", "2"), decoded.single().mediaIds)
    }

    @Test
    fun nameNormalizationCollapsesWhitespaceAndRejectsBlankNames() {
        assertEquals("Anime Mix", normalizePlaylistName("  Anime   Mix  "))

        val failure = runCatching { normalizePlaylistName("   ") }
        assertTrue(failure.isFailure)
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
