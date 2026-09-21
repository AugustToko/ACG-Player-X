package top.geek_studio.chenlongcould.musicplayer.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlaybackStatisticsImportRepositoryInstrumentedTest {
    @Test
    fun exportedJsonCanBeReadBackFromFileUri(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "statistics-round-trip.json")
        val snapshot =
            PlaybackStatisticsSnapshot(
                schemaVersion = PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION,
                generatedAtMs = 123_456L,
                summary = PlaybackStatisticsSummary(2, 1, 5L, 1L, 90_000L),
                entries = listOf(entry("1", true, "Song", 4), entry("-2", false, "", 1)),
            )
        file.writeText(encodePlaybackStatistics(snapshot, PlaybackStatisticsExportFormat.JSON), Charsets.UTF_8)

        try {
            val document = PlaybackStatisticsImportRepository(context).read(Uri.fromFile(file))
            assertEquals(PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION, document.schemaVersion)
            assertEquals(123_456L, document.generatedAtMs)
            assertEquals(listOf("1", "-2"), document.entries.map(PlaybackStatisticsEntry::mediaId))
            assertEquals(4, document.entries.first().playCount)
            assertTrue(document.entries.last().available.not())
        } finally {
            file.delete()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun futureSchemaIsRejected(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "statistics-future-schema.json")
        file.writeText(
            """
            {
              "schemaVersion": ${PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION + 1},
              "generatedAtMs": 1,
              "entries": []
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )
        try {
            PlaybackStatisticsImportRepository(context).read(Uri.fromFile(file))
        } finally {
            file.delete()
        }
    }

    private fun entry(mediaId: String, available: Boolean, title: String, playCount: Int) =
        PlaybackStatisticsEntry(
            mediaId = mediaId,
            available = available,
            title = title,
            artist = if (available) "Artist" else "",
            album = if (available) "Album" else "",
            folderPath = if (available) "Music" else "",
            playCount = playCount,
            lastPlayedAtMs = 2_000L,
            completedCount = if (available) 1 else 0,
            lastCompletedAtMs = if (available) 1_900L else 0L,
            totalListenTimeMs = 45_000L,
            lastPositionMs = 0L,
            durationMs = if (available) 180_000L else 0L,
        )
}
