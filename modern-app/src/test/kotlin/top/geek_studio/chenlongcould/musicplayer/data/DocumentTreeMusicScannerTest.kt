package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTreeMusicScannerTest {
    @Test
    fun audioDocumentsAreDetectedByMimeTypeOrExtension() {
        assertTrue(isSupportedAudioDocument("track.bin", "audio/flac"))
        assertTrue(isSupportedAudioDocument("track.MP3", "application/octet-stream"))
        assertTrue(isSupportedAudioDocument("track.ogg", "application/ogg"))
        assertFalse(isSupportedAudioDocument("cover.jpg", "image/jpeg"))
        assertFalse(isSupportedAudioDocument("notes.txt", "text/plain"))
    }

    @Test
    fun documentSongIdsAreStableNegativeAndUriSpecific() {
        val first = stableDocumentSongId("content://provider/tree/root/document/song-a")
        val second = stableDocumentSongId("content://provider/tree/root/document/song-b")

        assertTrue(first < 0L)
        assertEquals(first, stableDocumentSongId("content://provider/tree/root/document/song-a"))
        assertNotEquals(first, second)
    }

    @Test
    fun authorizedFolderPathsKeepStableIdentityButExposeReadablePath() {
        val encoded =
            encodeAuthorizedFolderPath(
                treeUriString = "content://provider/tree/primary%3AMusic",
                displayPath = "Music/Anime/Fate",
            )

        assertTrue(isAuthorizedFolderPath(encoded))
        assertEquals("Music/Anime/Fate", displayMusicFolderPath(encoded))
        assertEquals("Music/Anime/Fate", displayMusicFolderPath("Music/Anime/Fate"))
    }
}
