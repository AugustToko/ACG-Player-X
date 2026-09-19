package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class PlaybackStatisticsExportTest {
    @Test
    fun snapshotKeepsUnavailableStatsAndBuildsStableTotals() {
        val songs =
            listOf(
                song(
                    id = 1,
                    title = "A \"quoted\" song",
                    artist = "Artist A",
                    album = "Album A",
                ),
            )
        val stats =
            mapOf(
                "1" to
                    PlaybackStats(
                        playCount = 3,
                        completedCount = 2,
                        totalListenTimeMs = 10_000L,
                    ),
                "99" to
                    PlaybackStats(
                        playCount = 1,
                        totalListenTimeMs = 5_000L,
                    ),
            )

        val snapshot = buildPlaybackStatisticsSnapshot(songs, stats, generatedAtMs = 123L)

        assertEquals(2, snapshot.summary.trackedMediaCount)
        assertEquals(1, snapshot.summary.availableMediaCount)
        assertEquals(4L, snapshot.summary.totalPlayCount)
        assertEquals(2L, snapshot.summary.totalCompletedCount)
        assertEquals(15_000L, snapshot.summary.totalListenTimeMs)
        assertEquals("1", snapshot.entries.first().mediaId)
        assertFalse(snapshot.entries.last().available)
    }

    @Test
    fun jsonEscapesMetadataAndCarriesSchemaVersion() {
        val snapshot =
            buildPlaybackStatisticsSnapshot(
                songs =
                    listOf(
                        song(
                            id = 1,
                            title = "Line 1\nLine 2",
                            artist = "A, B",
                            album = "Quote \"Album\"",
                        ),
                    ),
                playbackStats = mapOf("1" to PlaybackStats(playCount = 1)),
                generatedAtMs = 456L,
            )

        val json = encodePlaybackStatistics(snapshot, PlaybackStatisticsExportFormat.JSON)

        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("Line 1\\nLine 2"))
        assertTrue(json.contains("Quote \\\"Album\\\""))
    }

    @Test
    fun csvQuotesCommaQuoteAndNewlineFields() {
        val snapshot =
            buildPlaybackStatisticsSnapshot(
                songs =
                    listOf(
                        song(
                            id = 1,
                            title = "Hello, world",
                            artist = "A \"Singer\"",
                            album = "One\nTwo",
                        ),
                    ),
                playbackStats = mapOf("1" to PlaybackStats(playCount = 1)),
                generatedAtMs = 789L,
            )

        val csv = encodePlaybackStatistics(snapshot, PlaybackStatisticsExportFormat.CSV)

        assertTrue(csv.contains("\"Hello, world\""))
        assertTrue(csv.contains("\"A \"\"Singer\"\"\""))
        assertTrue(csv.contains("\"One\nTwo\""))
    }

    @Test
    fun artistInsightsAggregateSongsByListeningTime() {
        val snapshot =
            buildPlaybackStatisticsSnapshot(
                songs =
                    listOf(
                        song(1, "One", "Artist A", "Album"),
                        song(2, "Two", "Artist A", "Album"),
                        song(3, "Three", "Artist B", "Album"),
                    ),
                playbackStats =
                    mapOf(
                        "1" to PlaybackStats(playCount = 2, totalListenTimeMs = 10_000L),
                        "2" to PlaybackStats(playCount = 1, totalListenTimeMs = 20_000L),
                        "3" to PlaybackStats(playCount = 8, totalListenTimeMs = 5_000L),
                    ),
                generatedAtMs = 0L,
            )

        val insights = topArtistInsights(snapshot)

        assertEquals("Artist A", insights.first().name)
        assertEquals(2, insights.first().songCount)
        assertEquals(3L, insights.first().playCount)
        assertEquals(30_000L, insights.first().listenTimeMs)
    }

    @Test
    fun generatedFileNameUsesRequestedExtension() {
        assertTrue(
            playbackStatisticsFileName(
                PlaybackStatisticsExportFormat.JSON,
                timestampMs = 0L,
            ).endsWith(".json"),
        )
        assertTrue(
            playbackStatisticsFileName(
                PlaybackStatisticsExportFormat.CSV,
                timestampMs = 0L,
            ).endsWith(".csv"),
        )
    }

    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String,
    ): Song =
        Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = 180_000L,
            contentUri = "content://test/$id",
            albumArtUri = null,
            folderName = "Music",
            folderPath = "Music/",
            displayName = "$title.mp3",
        )
}
