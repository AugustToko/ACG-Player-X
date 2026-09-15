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

        val encoded = encodeM3uPlaylist(playlist, songs)
        val parsed = parseM3uPlaylist(encoded)
        val resolved = resolveM3uPlaylist(parsed, songs)

        assertEquals("Anime Mix", parsed.name)
        assertEquals(listOf("1", "-7", "99"), resolved.mediaIds)
        assertEquals(2, resolved.matchedCount)
        assertEquals(1, resolved.preservedUnavailableCount)
        assertEquals(0, resolved.unmatchedCount)
        assertTrue(encoded.contains("Music/Anime/Brave Shine.mp3"))
        assertTrue(encoded.contains("SD/Anime/青鸟.flac"))
        assertTrue(encoded.contains("#ACGPLAYER-CONTENT-URI:content://test/audio/1"))
    }

    @Test
    fun sameDeviceRoundTripUsesContentUriHintBehindRelativeLocation() {
        val playlist =
            UserPlaylist(
                id = "playlist",
                name = "Exact URI",
                mediaIds = listOf("1"),
                createdAtMs = 1L,
                updatedAtMs = 1L,
            )
        val expected = song(1, "Same", "Artist", "Same.mp3", "Music/A")
        val collision =
            expected.copy(
                id = 2,
                contentUri = "content://test/audio/2",
                folderPath = "Music/B",
            )

        val parsed = parseM3uPlaylist(encodeM3uPlaylist(playlist, listOf(expected)))
        val resolved = resolveM3uPlaylist(parsed, listOf(collision, expected))

        assertEquals("Music/A/Same.mp3", parsed.entries.single().location)
        assertEquals("content://test/audio/1", parsed.entries.single().contentUriHint)
        assertEquals(listOf("1"), resolved.mediaIds)
    }

    @Test
    fun portableLocationStripsStorageRootsAndNeutralizesTraversalSegments() {
        val removable =
            song(
                id = 1,
                title = "Track",
                artist = "Artist",
                displayName = "Track.flac",
                folderPath = "/storage/1234-5678/Music/../Anime",
            )
        val primary =
            removable.copy(
                id = 2,
                folderPath = "/storage/emulated/0/Music/Anime",
            )

        assertEquals("Music/_/Anime/Track.flac", portableM3uLocation(removable))
        assertEquals("Music/Anime/Track.flac", portableM3uLocation(primary))
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
    fun collidingDeviceMediaIdDoesNotOverridePortableHints() {
        val sourcePlaylist =
            UserPlaylist(
                id = "playlist",
                name = "Portable",
                mediaIds = listOf("1"),
                createdAtMs = 1L,
                updatedAtMs = 1L,
            )
        val sourceSong = song(1, "Again", "YUI", "Again.m4a", "Music/JPop")
        val unrelatedTargetSong =
            song(1, "Different", "Other", "Different.mp3", "Music/Other")
        val correctTargetSong =
            sourceSong.copy(
                id = 42,
                contentUri = "content://target/audio/42",
            )

        val parsed = parseM3uPlaylist(encodeM3uPlaylist(sourcePlaylist, listOf(sourceSong)))
        val resolved =
            resolveM3uPlaylist(
                parsed = parsed,
                songs = listOf(unrelatedTargetSong, correctTargetSong),
            )

        assertEquals(listOf("42"), resolved.mediaIds)
        assertEquals(1, resolved.matchedCount)
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
