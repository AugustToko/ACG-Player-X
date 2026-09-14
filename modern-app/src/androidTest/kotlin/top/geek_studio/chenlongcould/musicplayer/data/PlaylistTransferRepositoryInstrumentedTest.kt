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
import top.geek_studio.chenlongcould.musicplayer.model.Song

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlaylistTransferRepositoryInstrumentedTest {
    @Test
    fun fileUriImportPreviewAndExportRoundTrip() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = PlaylistTransferRepository(context)
        val suffix = System.nanoTime()
        val source = File(context.cacheDir, "import-$suffix.m3u8")
        val destination = File(context.cacheDir, "export-$suffix.m3u8")
        val song =
            Song(
                id = 7L,
                title = "Brave Shine",
                artist = "Aimer",
                album = "DAWN",
                durationMs = 270_000L,
                contentUri = "content://test/audio/7",
                albumArtUri = null,
                folderName = "Anime",
                folderPath = "Music/Anime",
                displayName = "Brave Shine.mp3",
            )
        source.writeText(
            """
            #EXTM3U
            #PLAYLIST:Instrumented Import
            #EXTINF:270,Aimer - Brave Shine
            content://test/audio/7
            """.trimIndent(),
            Charsets.UTF_8,
        )

        try {
            val preview =
                repository.prepareImport(
                    uri = Uri.fromFile(source),
                    librarySongs = listOf(song),
                )
            assertEquals("Instrumented Import", preview.preferredName)
            assertEquals(1, preview.selectedCount)
            assertEquals("7", preview.entries.single().selectedMediaId)

            val playlist =
                UserPlaylist(
                    id = "instrumented",
                    name = "Instrumented Export",
                    mediaIds = listOf("7", "99"),
                    createdAtMs = 1L,
                    updatedAtMs = 2L,
                )
            val exported =
                repository.exportPlaylist(
                    uri = Uri.fromFile(destination),
                    playlist = playlist,
                    librarySongs = listOf(song),
                )
            assertEquals(2, exported.exportedCount)
            assertEquals(1, exported.unavailableCount)

            val content = destination.readText(Charsets.UTF_8)
            assertTrue(content.startsWith("#EXTM3U"))
            assertTrue(content.contains("#PLAYLIST:Instrumented Export"))
            assertTrue(content.contains("acg-player://media/99"))

            val roundTrip =
                repository.prepareImport(
                    uri = Uri.fromFile(destination),
                    librarySongs = listOf(song),
                )
            assertEquals(listOf("7", "99"), roundTrip.entries.mapNotNull { it.selectedMediaId })
        } finally {
            source.delete()
            destination.delete()
        }
    }
}
