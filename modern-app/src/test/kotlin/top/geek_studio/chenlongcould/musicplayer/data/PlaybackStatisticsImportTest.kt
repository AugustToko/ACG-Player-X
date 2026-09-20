package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class PlaybackStatisticsImportTest {
    @Test
    fun exactIdRequiresPortableIdentityConfirmation() {
        val preview =
            buildPlaybackStatisticsImportPreview(
                document = document(entry(mediaId = "1", title = "Different", artist = "Other", durationMs = 180_000L)),
                songs = listOf(song(1, "Song", "Artist", "Album")),
            )
        assertEquals(0, preview.exactMatchCount)
        assertEquals(0, preview.importableEntryCount)
        assertEquals(1, preview.skippedEntryCount)
    }

    @Test
    fun collidingIdFallsBackToUniquePortableMetadata() {
        val preview =
            buildPlaybackStatisticsImportPreview(
                document = document(entry(mediaId = "1", title = "Target", artist = "Artist B", durationMs = 180_000L, playCount = 7)),
                songs = listOf(song(1, "Wrong", "Artist A", "Album A"), song(2, "Target", "Artist B", "Album B")),
            )
        assertEquals(0, preview.exactMatchCount)
        assertEquals(1, preview.portableMatchCount)
        assertEquals(7, preview.matchedStats.getValue("2").playCount)
        assertFalse("1" in preview.matchedStats)
    }

    @Test
    fun ambiguousPortableMatchesAreSkipped() {
        val preview =
            buildPlaybackStatisticsImportPreview(
                document = document(entry(mediaId = "99", title = "Same", artist = "Artist", durationMs = 180_000L)),
                songs = listOf(song(1, "Same", "Artist", "One"), song(2, "Same", "Artist", "Two")),
            )
        assertEquals(1, preview.ambiguousEntryCount)
        assertEquals(0, preview.importableEntryCount)
        assertTrue(preview.warnings.any { "多个候选" in it })
    }

    @Test
    fun unavailableEntriesCanBeRetainedWithoutTouchingCurrentLibrary() {
        val preview =
            buildPlaybackStatisticsImportPreview(
                document = document(entry(mediaId = "-77", available = false, playCount = 3)),
                songs = emptyList(),
            )
        assertEquals(1, preview.unavailableEntryCount)
        assertEquals(3, preview.selectedStats(retainUnavailable = true).getValue("-77").playCount)
        assertTrue(preview.selectedStats(retainUnavailable = false).isEmpty())
    }

    @Test
    fun mergeIsIdempotentAndNeverAddsCountersTwice() {
        val local = mapOf("1" to PlaybackStats(playCount = 10, completedCount = 2, totalListenTimeMs = 1_000L, lastPlayedAtMs = 5_000L))
        val backup = mapOf("1" to PlaybackStats(playCount = 8, completedCount = 4, totalListenTimeMs = 900L, lastPlayedAtMs = 4_000L))
        val first = applyPlaybackStatisticsImport(local, backup, PlaybackStatisticsImportMode.MERGE)
        val second = applyPlaybackStatisticsImport(first, backup, PlaybackStatisticsImportMode.MERGE)
        assertEquals(first, second)
        assertEquals(10, first.getValue("1").playCount)
        assertEquals(4, first.getValue("1").completedCount)
        assertEquals(1_000L, first.getValue("1").totalListenTimeMs)
    }

    @Test
    fun newerImportedCompletionClearsOlderResumePosition() {
        val local = PlaybackStats(lastPlayedAtMs = 5_000L, lastPositionMs = 60_000L, durationMs = 180_000L)
        val imported = PlaybackStats(lastPlayedAtMs = 6_000L, completedCount = 1, lastCompletedAtMs = 7_000L, lastPositionMs = 0L, durationMs = 180_000L)
        val merged = mergePlaybackStatsIdempotently(local, imported)
        assertEquals(0L, merged.lastPositionMs)
        assertEquals(1, merged.completedCount)
    }

    @Test
    fun replaceDropsLocalStatisticsNotPresentInBackup() {
        val replaced =
            applyPlaybackStatisticsImport(
                current = mapOf("1" to PlaybackStats(playCount = 5)),
                imported = mapOf("2" to PlaybackStats(playCount = 2)),
                mode = PlaybackStatisticsImportMode.REPLACE,
            )
        assertEquals(setOf("2"), replaced.keys)
        assertEquals(2, replaced.getValue("2").playCount)
    }

    private fun document(vararg entries: PlaybackStatisticsEntry) =
        PlaybackStatisticsImportDocument(
            schemaVersion = PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION,
            generatedAtMs = 1_000L,
            entries = entries.toList(),
        )

    private fun entry(
        mediaId: String,
        available: Boolean = true,
        title: String = "",
        artist: String = "",
        album: String = "",
        durationMs: Long = 0L,
        playCount: Int = 1,
    ) = PlaybackStatisticsEntry(
        mediaId = mediaId,
        available = available,
        title = title,
        artist = artist,
        album = album,
        folderPath = "",
        playCount = playCount,
        lastPlayedAtMs = 1_000L,
        completedCount = 0,
        lastCompletedAtMs = 0L,
        totalListenTimeMs = 30_000L,
        lastPositionMs = 0L,
        durationMs = durationMs,
    )

    private fun song(id: Long, title: String, artist: String, album: String, durationMs: Long = 180_000L) =
        Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            contentUri = "content://test/$id",
            albumArtUri = null,
            folderName = "Music",
            folderPath = "Music/",
            displayName = "$title.mp3",
        )
}
