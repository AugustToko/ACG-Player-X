package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class M3uPlaylistCodecTest {
    @Test
    fun exportedPlaylistRoundTripsAvailableAndUnavailableIds() {
        val playlist =
            UserPlaylist(
                id = "playlist",
                name = "Anime Mix",
                mediaIds = listOf("1", "-7", "99"),
                createdAtMs = 1L,
                updatedAtMs = 2L,
            )
        val songs =
            listOf(
                song(1, "Brave Shine", "Aimer", "Brave Shine.mp3", "Music/Anime"),
                song(-7, "青鸟", "生物股长", "青鸟.flac", "SD/Anime"),
            )

        val parsed = parseM3uPlaylist(encodeM3uPlaylist(playlist, songs))
        val resolved = resolveM3uPlaylist(parsed, songs)

        assertEquals("Anime Mix", parsed.name)
        assertEquals(listOf("1", "-7", "99"), resolved.mediaIds)
        assertEquals(2, resolved.matchedCount)
        assertEquals(1, resolved.preservedUnavailableCount)
        assertEquals(0, resolved.unmatchedCount)
    }

    @Test
    fun staleDeviceMediaIdFallsBackToPortableFileHints() {
        val sourcePlaylist =
            UserPlaylist(
                id = "playlist",
                name = "Portable",
                mediaIds = listOf("1"),
                createdAtMs = 1L,
                updatedAtMs = 1L,
            )
        val sourceSong = song(1, "Again", "YUI", "Again.m4a", "Music/JPop")
        val targetSong =
            sourceSong.copy(
                id = 42,
                contentUri = "content://target/audio/42",
            )

        val parsed = parseM3uPlaylist(encodeM3uPlaylist(sourcePlaylist, listOf(sourceSong)))
        val resolved = resolveM3uPlaylist(parsed, listOf(targetSong))

        assertEquals(listOf("42"), resolved.mediaIds)
        assertEquals(1, resolved.matchedCount)
        assertEquals(0, resolved.preservedUnavailableCount)
    }

    @Test
    fun externalPlaylistMatchesByFileNameAndMetadata() {
        val content =
            """
            #EXTM3U
            #PLAYLIST:Imported
            #EXTINF:270,Aimer - Brave Shine
            /storage/emulated/0/Music/Anime/Brave%20Shine.mp3
            """.trimIndent()
        val songs =
            listOf(song(8, "Brave Shine", "Aimer", "Brave Shine.mp3", "Music/Anime"))

        val resolved = resolveM3uPlaylist(parseM3uPlaylist(content), songs)

        assertEquals(listOf("8"), resolved.mediaIds)
        assertEquals(0, resolved.unmatchedCount)
    }

    @Test
    fun ambiguousFileNameIsNotGuessed() {
        val content =
            """
            #EXTM3U
            Same.mp3
            """.trimIndent()
        val songs =
            listOf(
                song(1, "Same", "A", "Same.mp3", "Music/A"),
                song(2, "Same", "B", "Same.mp3", "Music/B"),
            )

        val resolved = resolveM3uPlaylist(parseM3uPlaylist(content), songs)

        assertTrue(resolved.mediaIds.isEmpty())
        assertEquals(1, resolved.ambiguousCount)
    }

    @Test
    fun parserAcceptsBomAndDeduplicatesResolvedEntries() {
        val content =
            "\uFEFF#EXTM3U\n#PLAYLIST:测试\n#ACGPLAYER-MEDIA-ID:1\nacg-player://media/1\n" +
                "#ACGPLAYER-MEDIA-ID:1\nacg-player://media/1\n"
        val parsed = parseM3uPlaylist(content)
        val resolved = resolveM3uPlaylist(parsed, listOf(song(1, "Song", "Artist", "Song.mp3", "Music")))

        assertEquals("测试", parsed.name)
        assertEquals(listOf("1"), resolved.mediaIds)
        assertEquals(1, resolved.duplicateCount)
    }

    @Test
    fun importedNameGenerationAvoidsCaseInsensitiveCollisions() {
        val existing =
            listOf(
                UserPlaylist("1", "Anime Mix", emptyList(), 1L, 1L),
                UserPlaylist("2", "Anime Mix (2)", emptyList(), 1L, 1L),
            )

        assertEquals("Anime Mix (3)", uniquePlaylistName(existing, "Anime Mix"))
    }

    @Test
    fun exportFileNameRemovesUnsafeCharacters() {
        val name = m3uExportFileName("  A/B: C?  ")

        assertEquals("A_B_ C_.m3u8", name)
        assertFalse(name.contains('/'))
    }

    private fun song(
        id: Long,
        title: String,
        artist: String,
        displayName: String,
        folderPath: String,
    ): Song =
        Song(
            id = id,
            title = title,
            artist = artist,
            album = "Album",
            durationMs = 270_000L,
            contentUri = "content://test/audio/$id",
            albumArtUri = null,
            folderName = folderPath.substringAfterLast('/'),
            folderPath = folderPath,
            displayName = displayName,
        )
}
