package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.geek_studio.chenlongcould.musicplayer.data.AuthorizedFolder
import top.geek_studio.chenlongcould.musicplayer.data.PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsExportFormat
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportDocument
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportMode
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsSnapshot
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStats
import top.geek_studio.chenlongcould.musicplayer.data.parsePlaybackStatisticsImportJson
import top.geek_studio.chenlongcould.musicplayer.model.Song

/** Real ViewModel + Compose, with deliberately suspended I/O; not a system-picker/OEM test. */
@RunWith(AndroidJUnit4::class)
class StatisticsImportPreparationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var store: ViewModelStore
    private lateinit var operations: DeferredOperations
    private lateinit var model: StatisticsViewModel
    private lateinit var document: PlaybackStatisticsImportDocument

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            store = ViewModelStore()
            operations = DeferredOperations()
            model = StatisticsViewModel(instrumentation.targetContext.applicationContext as Application, operations)
            store.put("statistics", model)
            document = parsePlaybackStatisticsImportJson(
                """{"schemaVersion":$PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION,"generatedAtMs":1000,
                "entries":[{"mediaId":"7","available":true,"title":"Deferred Song","artist":"Artist",
                "album":"Album","durationMs":180000,"playCount":5}]}""",
            )
        }
        compose.setContent {
            val state by model.uiState.collectAsState()
            MaterialTheme {
                Column {
                    StatisticsImportPreparationStatus(state, model::cancelImport, model::retryLibraryMetadata)
                    state.importPreview?.let { preview ->
                        PlaybackStatisticsImportDialog(
                            preview, state.importMode, state.retainUnavailable, state.isWorking,
                            model::setImportMode, model::setRetainUnavailable, model::confirmImport, model::cancelImport,
                        )
                    }
                }
            }
        }
    }

    @After
    fun tearDown() {
        if (::store.isInitialized) InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
    }

    @Test
    fun selectedFileWaitsForScanThenRequiresOneExplicitConfirmation() {
        beginPendingImport()
        awaitPhase(StatisticsImportPhase.WAITING_FOR_METADATA)
        compose.onNodeWithTag("statistics_import_preparation").assertIsDisplayed()
        compose.runOnIdle {
            model.confirmImport() // A stale callback must not submit an incomplete preview.
            assertEquals(0, operations.writes)
            operations.scans[0].complete(listOf(song(7)))
        }
        awaitPhase(StatisticsImportPhase.PREVIEW)
        compose.onNodeWithTag("statistics_import_confirm").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) { model.uiState.value.isWorking }
        compose.runOnIdle {
            model.confirmImport()
            assertEquals(1, operations.writes)
            assertEquals(5, operations.imported.getValue("7").playCount)
            assertEquals(PlaybackStatisticsImportMode.MERGE, operations.mode)
            operations.commit.complete(1)
        }
        awaitPhase(StatisticsImportPhase.NONE)
        compose.waitUntil(10_000) { !model.uiState.value.isWorking }
    }

    @Test
    fun cancelWhileWaitingDoesNotResurrectPreview() {
        beginPendingImport()
        awaitPhase(StatisticsImportPhase.WAITING_FOR_METADATA)
        compose.onNodeWithTag("statistics_import_cancel").performClick()
        compose.runOnIdle { operations.scans[0].complete(listOf(song(7))) }
        compose.waitUntil(10_000) { model.uiState.value.hasLibraryMetadata }
        compose.runOnIdle {
            assertEquals(StatisticsImportPhase.NONE, model.uiState.value.importPhase)
            assertNull(model.uiState.value.importPreview)
            assertEquals(0, operations.writes)
        }
    }

    @Test
    fun scanFailureCanRetryWithoutSelectingOrReadingFileAgain() {
        beginPendingImport()
        awaitPhase(StatisticsImportPhase.WAITING_FOR_METADATA)
        compose.runOnIdle { operations.scans[0].completeExceptionally(IOException("provider offline")) }
        awaitPhase(StatisticsImportPhase.METADATA_FAILED)
        compose.onNodeWithTag("statistics_import_retry").performClick()
        compose.runOnIdle { operations.scans[1].complete(listOf(song(7))) }
        awaitPhase(StatisticsImportPhase.PREVIEW)
        compose.runOnIdle {
            assertEquals(1, operations.reads)
            assertEquals(2, operations.loads)
            assertEquals(0, operations.writes)
        }
    }

    @Test
    fun newMetadataInvalidatesPreviewBeforeConfirmation() {
        beginPendingImport()
        compose.runOnIdle { operations.scans[0].complete(listOf(song(7))) }
        awaitPhase(StatisticsImportPhase.PREVIEW)
        compose.runOnIdle {
            model.refreshLibraryMetadata(true, emptyList(), 2)
            assertNull(model.uiState.value.importPreview)
            model.confirmImport()
            assertEquals(0, operations.writes)
            // Same portable identity now has a different MediaStore ID.
            operations.scans[1].complete(listOf(song(8)))
        }
        awaitPhase(StatisticsImportPhase.PREVIEW)
        compose.runOnIdle {
            assertEquals(setOf("8"), model.uiState.value.importPreview!!.matchedStats.keys)
            assertEquals(0, operations.writes)
        }
    }

    @Test
    fun cancelWhileReadingIgnoresLaterFileCompletion() {
        compose.runOnIdle {
            model.refreshLibraryMetadata(true, emptyList(), 1)
            operations.scans[0].complete(listOf(song(7)))
            model.prepareImport(Uri.parse("content://test/backup.json"))
        }
        awaitPhase(StatisticsImportPhase.READING)
        compose.onNodeWithTag("statistics_import_cancel").performClick()
        compose.runOnIdle { operations.read.complete(document) }
        compose.waitUntil(10_000) { model.uiState.value.hasLibraryMetadata }
        compose.runOnIdle {
            assertEquals(StatisticsImportPhase.NONE, model.uiState.value.importPhase)
            assertNull(model.uiState.value.importPreview)
            assertEquals(0, operations.writes)
        }
    }

    @Test
    fun identicalMetadataRequestsShareTheInFlightScan() {
        compose.runOnIdle {
            model.refreshLibraryMetadata(true, emptyList(), 1)
            model.refreshLibraryMetadata(true, emptyList(), 1)
            model.refreshLibraryMetadata(true, emptyList(), 1)
            operations.read.complete(document)
            model.prepareImport(Uri.parse("content://test/backup.json"))
        }
        awaitPhase(StatisticsImportPhase.WAITING_FOR_METADATA)
        compose.runOnIdle {
            assertEquals(1, operations.loads)
            operations.scans[0].complete(listOf(song(7)))
        }
        awaitPhase(StatisticsImportPhase.PREVIEW)
    }

    private fun beginPendingImport() = compose.runOnIdle {
        model.refreshLibraryMetadata(true, emptyList(), 1)
        operations.read.complete(document)
        model.prepareImport(Uri.parse("content://test/backup.json"))
    }

    private fun awaitPhase(phase: StatisticsImportPhase) {
        compose.waitUntil(10_000) { model.uiState.value.importPhase == phase }
    }

    private fun song(id: Long) = Song(
        id = id, title = "Deferred Song", artist = "Artist", album = "Album",
        durationMs = 180_000, contentUri = "content://media/external/audio/media/$id",
        albumArtUri = null, folderName = "Music", folderPath = "Music/",
    )

    private class DeferredOperations : StatisticsOperations {
        val scans = List(3) { CompletableDeferred<List<Song>>() }
        val read = CompletableDeferred<PlaybackStatisticsImportDocument>()
        val commit = CompletableDeferred<Int>()
        var loads = 0
        var reads = 0
        var writes = 0
        var imported: Map<String, PlaybackStats> = emptyMap()
        var mode: PlaybackStatisticsImportMode? = null

        override suspend fun loadMetadata(includeMediaStore: Boolean, folders: List<AuthorizedFolder>): List<Song> =
            scans[loads++].await()
        override suspend fun readImport(uri: Uri): PlaybackStatisticsImportDocument {
            reads += 1
            return read.await()
        }
        override suspend fun importStatistics(stats: Map<String, PlaybackStats>, mode: PlaybackStatisticsImportMode): Int {
            writes += 1
            imported = stats.toMap()
            this.mode = mode
            return commit.await()
        }
        override suspend fun export(uri: Uri, format: PlaybackStatisticsExportFormat, snapshot: PlaybackStatisticsSnapshot) {
            error("export is not part of an import preparation")
        }
        override suspend fun clearListeningData(clearRecent: Boolean, clearPlaybackStats: Boolean) {
            error("privacy reset is not part of an import preparation")
        }
    }
}
