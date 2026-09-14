package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class PlaylistImportPreviewTest {
    @Test
    fun ambiguousEntryExposesAllReliableCandidatesWithoutGuessing() {
        val parsed = parseM3uPlaylist("#EXTM3U\nSame.mp3")
        val songs =
            listOf(
                song(1, "Same", "Artist A", "Same.mp3", "Music/A"),
                song(2, "Same", "Artist B", "Same.mp3", "Music/B"),
            )

        val preview = buildPlaylistImportPreview(parsed, "Imported", songs)
        val entry = preview.entries.single()

        assertEquals(PlaylistImportEntryStatus.AMBIGUOUS, entry.status)
        assertEquals(listOf("1", "2"), entry.candidateMediaIds)
        assertEquals(2, entry.candidateCount)
        assertNull(entry.selectedMediaId)
    }

    @Test
    fun manualMappingsPreserveOriginalEntryOrderAndClearAttentionCounts() {
        val content =
            """
            #EXTM3U
            #PLAYLIST:Review
            #EXTINF:270,Aimer - Brave Shine
            Brave%20Shine.mp3
            Same.mp3
            Missing.mp3
            """.trimIndent()
        val songs =
            listOf(
                song(1, "Brave Shine", "Aimer", "Brave Shine.mp3", "Music/Anime"),
                song(2, "Same", "Artist A", "Same.mp3", "Music/A"),
                song(3, "Same", "Artist B", "Same.mp3", "Music/B"),
                song(4, "Manual", "Artist C", "Manual.mp3", "Music/C"),
            )
        var preview =
            buildPlaylistImportPreview(
                parsed = parseM3uPlaylist(content),
                fallbackName = "Imported",
                librarySongs = songs,
            )
        preview = updatePlaylistImportSelection(preview, entryIndex = 1, mediaId = "3")
        preview = updatePlaylistImportSelection(preview, entryIndex = 2, mediaId = "4")

        val result = finalizePlaylistImport(preview)

        assertEquals("Review", result.preferredName)
        assertEquals(listOf("1", "3", "4"), result.mediaIds)
        assertEquals(1, result.matchedCount)
        assertEquals(2, result.manuallyMappedCount)
        assertEquals(0, result.ambiguousCount)
        assertEquals(0, result.unmatchedCount)
    }

    @Test
    fun finalizationDeduplicatesConflictingManualSelections() {
        val parsed = parseM3uPlaylist("#EXTM3U\nOne.mp3\nMissing.mp3")
        val songs = listOf(song(1, "One", "Artist", "One.mp3", "Music"))
        var preview = buildPlaylistImportPreview(parsed, "Imported", songs)
        preview = updatePlaylistImportSelection(preview, entryIndex = 1, mediaId = "1")

        val result = finalizePlaylistImport(preview)

        assertEquals(listOf("1"), result.mediaIds)
        assertEquals(1, result.matchedCount)
        assertEquals(0, result.manuallyMappedCount)
        assertEquals(1, result.duplicateCount)
    }

    @Test
    fun userCanExcludeAnAutomaticMatchBeforeCreatingPlaylist() {
        val parsed = parseM3uPlaylist("#EXTM3U\nOne.mp3")
        val songs = listOf(song(1, "One", "Artist", "One.mp3", "Music"))
        val preview =
            updatePlaylistImportSelection(
                preview = buildPlaylistImportPreview(parsed, "Imported", songs),
                entryIndex = 0,
                mediaId = null,
            )

        val result = finalizePlaylistImport(preview)

        assertEquals(emptyList<String>(), result.mediaIds)
        assertEquals(1, result.skippedCount)
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
