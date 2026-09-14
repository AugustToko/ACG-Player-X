package top.geek_studio.chenlongcould.musicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportEntryStatus
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportPreview
import top.geek_studio.chenlongcould.musicplayer.data.PlaylistImportPreviewEntry
import top.geek_studio.chenlongcould.musicplayer.model.Song

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlaylistImportPreviewInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun attentionFilterHidesResolvedEntriesAndKeepsConfirmAvailable() {
        var confirmed = false
        val preview =
            PlaylistImportPreview(
                preferredName = "Imported",
                entries =
                    listOf(
                        PlaylistImportPreviewEntry(
                            index = 0,
                            location = "Matched.mp3",
                            title = "Matched",
                            status = PlaylistImportEntryStatus.MATCHED,
                            automaticMediaId = "1",
                            selectedMediaId = "1",
                        ),
                        PlaylistImportPreviewEntry(
                            index = 1,
                            location = "Missing.mp3",
                            title = "Missing",
                            status = PlaylistImportEntryStatus.UNMATCHED,
                        ),
                    ),
            )

        composeRule.setContent {
            MaterialTheme {
                PlaylistImportPreviewDialog(
                    preview = preview,
                    librarySongs = listOf(song(1, "Matched")),
                    isWorking = false,
                    errorMessage = null,
                    onNameChange = {},
                    onSelectionChange = { _, _ -> },
                    onConfirm = { confirmed = true },
                    onDismiss = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("M3U 导入预览").assertIsDisplayed()
        composeRule.onNodeWithText("已选择 1 / 2 项").assertIsDisplayed()
        composeRule.onNodeWithText("仅看需处理 1").performClick()
        composeRule.onNodeWithText("#1 Matched").assertDoesNotExist()
        composeRule.onNodeWithText("#2 Missing").assertIsDisplayed()
        composeRule.onNodeWithText("创建歌单").performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun manualMappingReturnsSelectedMediaId() {
        var selectedEntryIndex: Int? = null
        var selectedMediaId: String? = null
        val preview =
            PlaylistImportPreview(
                preferredName = "Imported",
                entries =
                    listOf(
                        PlaylistImportPreviewEntry(
                            index = 0,
                            location = "Same.mp3",
                            title = "Same",
                            status = PlaylistImportEntryStatus.AMBIGUOUS,
                            candidateMediaIds = listOf("7"),
                        ),
                    ),
            )

        composeRule.setContent {
            MaterialTheme {
                PlaylistImportPreviewDialog(
                    preview = preview,
                    librarySongs = listOf(song(7, "Same")),
                    isWorking = false,
                    errorMessage = null,
                    onNameChange = {},
                    onSelectionChange = { index, mediaId ->
                        selectedEntryIndex = index
                        selectedMediaId = mediaId
                    },
                    onConfirm = {},
                    onDismiss = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("匹配").performClick()
        composeRule.onNodeWithText("选择对应歌曲").assertIsDisplayed()
        composeRule.onNodeWithText("选择").performClick()

        composeRule.runOnIdle {
            assertEquals(0, selectedEntryIndex)
            assertEquals("7", selectedMediaId)
        }
    }

    private fun song(
        id: Long,
        title: String,
    ): Song =
        Song(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            contentUri = "content://test/audio/$id",
            albumArtUri = null,
            folderName = "Music",
            folderPath = "Music",
            displayName = "$title.mp3",
        )
}
