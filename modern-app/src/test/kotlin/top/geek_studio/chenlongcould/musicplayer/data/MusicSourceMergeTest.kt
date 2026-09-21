package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class MusicSourceMergeTest {
    @Test
    fun mediaStoreEntryWinsWhenSafEntryRepresentsSameTrack() {
        val mediaStoreSong = song(
            id = 42L,
            uri = "content://media/external/audio/media/42",
            title = "Brave Shine",
            artist = "Aimer",
            album = "DAWN",
            durationMs = 233_200L,
        )
        val duplicateSafSong = song(
            id = -42L,
            uri = "content://documents/tree/music/document/brave-shine",
            title = "Brave Shine",
            artist = "Aimer",
            album = "DAWN",
            durationMs = 233_800L,
        )
        val distinctSafSong = song(
            id = -43L,
            uri = "content://documents/tree/music/document/last-stardust",
            title = "Last Stardust",
            artist = "Aimer",
            album = "DAWN",
            durationMs = 258_000L,
        )

        val merged =
            mergeMusicSources(
                mediaStoreSongs = listOf(mediaStoreSong),
                authorizedFolderSongs = listOf(duplicateSafSong, distinctSafSong),
            )

        assertEquals(2, merged.size)
        assertSame(mediaStoreSong, merged.first { it.title == "Brave Shine" })
        assertEquals(distinctSafSong, merged.first { it.title == "Last Stardust" })
    }

    @Test
    fun overlappingAuthorizedTreesDeduplicateExactDocumentUri() {
        val original = song(
            id = -1L,
            uri = "content://documents/document/shared-track",
            title = "Shared",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
        )
        val duplicate = original.copy(id = -2L)

        val merged =
            mergeMusicSources(
                mediaStoreSongs = emptyList(),
                authorizedFolderSongs = listOf(original, duplicate),
            )

        assertEquals(listOf(original), merged)
    }

    private fun song(
        id: Long,
        uri: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        contentUri = uri,
        albumArtUri = null,
        folderName = "Music",
        folderPath = "Music",
    )
}
