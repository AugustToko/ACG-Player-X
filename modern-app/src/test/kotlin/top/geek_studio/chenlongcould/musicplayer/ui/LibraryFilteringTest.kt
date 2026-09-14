package top.geek_studio.chenlongcould.musicplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class LibraryFilteringTest {
    private val songs =
        listOf(
            song(1, "Brave Shine", "Aimer", "DAWN"),
            song(2, "青鸟", "生物股长", "My Song Your Song"),
            song(3, "Again", "YUI", "HOLIDAYS IN THE SUN"),
        )

    @Test
    fun blankQueryReturnsWholeLibrary() {
        assertEquals(songs, filterSongs(songs, "  "))
    }

    @Test
    fun queryMatchesTitleArtistAndAlbumIgnoringCase() {
        assertEquals(listOf(songs[0]), filterSongs(songs, "aimer"))
        assertEquals(listOf(songs[1]), filterSongs(songs, "青鸟"))
        assertEquals(listOf(songs[2]), filterSongs(songs, "holidays"))
    }

    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 180_000L,
        contentUri = "content://test/$id",
        albumArtUri = null,
    )
}
