package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.geek_studio.chenlongcould.musicplayer.model.Song

class PlaybackStatisticsImportIndexTest {
    @Test
    fun tenThousandPortableEntriesResolveAgainstTenThousandSongs() {
        val size = 10_000
        val songs =
            List(size) { index ->
                song(
                    id = index.toLong() + 1L,
                    title = "Track $index",
                    artist = "Artist ${index % 200}",
                    album = "Album ${index % 50}",
                    durationMs = 180_000L + (index % 7) * 1_000L,
                )
            }
        val entries =
            songs.mapIndexed { index, song ->
                entry(
                    mediaId = "legacy-$index",
                    title = "  ${song.title.lowercase()}  ",
                    artist = song.artist.uppercase(),
                    album = song.album,
                    durationMs = song.durationMs,
                    playCount = (index % 9) + 1,
                )
            }

        val preview =
            buildPlaybackStatisticsImportPreview(
                document = document(entries),
                songs = songs,
            )

        assertEquals(size, preview.portableMatchCount)
        assertEquals(size, preview.matchedStats.size)
        assertEquals(0, preview.exactMatchCount)
        assertEquals(0, preview.ambiguousEntryCount)
        assertEquals(0, preview.skippedEntryCount)
        assertEquals(
            (9_876 % 9) + 1,
            preview.matchedStats.getValue("9877").playCount,
        )
    }

    @Test
    fun normalizedTitleArtistAndAlbumKeepExistingPortableSemantics() {
        val preview =
            buildPlaybackStatisticsImportPreview(
                document =
                    document(
                        listOf(
                            entry(
                                mediaId = "old",
                                title = "  BRAVE    SHINE ",
                                artist = " aimer ",
                                album = " dawn ",
                                durationMs = 240_000L,
                            ),
                        ),
                    ),
                songs =
                    listOf(
                        song(
                            id = 7L,
                            title = "Brave Shine",
                            artist = "Aimer",
                            album = "DAWN",
                            durationMs = 240_000L,
                        ),
                    ),
            )

        assertEquals(1, preview.portableMatchCount)
        assertEquals(setOf("7"), preview.matchedStats.keys)
    }

    @Test
    fun durationDisambiguatesSongsInsideTheSameMetadataBucket() {
        val preview =
            buildPlaybackStatisticsImportPreview(
                document =
                    document(
                        listOf(
                            entry(
                                mediaId = "old",
                                title = "Episode",
                                artist = "Narrator",
                                album = "Series",
                                durationMs = 1_800_000L,
                            ),
                        ),
                    ),
                songs =
                    listOf(
                        song(1L, "Episode", "Narrator", "Series", 1_800_000L),
                        song(2L, "Episode", "Narrator", "Series", 1_804_000L),
                    ),
            )

        assertEquals(1, preview.portableMatchCount)
        assertEquals(setOf("1"), preview.matchedStats.keys)
    }

    @Test
    fun unknownSongDurationStillCountsAsCandidateAndPreservesAmbiguity() {
        val preview =
            buildPlaybackStatisticsImportPreview(
                document =
                    document(
                        listOf(
                            entry(
                                mediaId = "old",
                                title = "Episode",
                                durationMs = 1_800_000L,
                            ),
                        ),
                    ),
                songs =
                    listOf(
                        song(1L, "Episode", "", "", 1_800_000L),
                        song(2L, "Episode", "", "", 0L),
                    ),
            )

        assertEquals(1, preview.ambiguousEntryCount)
        assertEquals(0, preview.importableEntryCount)
        assertTrue(preview.warnings.any { "多个候选" in it })
    }

    private fun document(entries: List<PlaybackStatisticsEntry>) =
        PlaybackStatisticsImportDocument(
            schemaVersion = PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION,
            generatedAtMs = 1_000L,
            entries = entries,
        )

    private fun entry(
        mediaId: String,
        title: String,
        artist: String = "",
        album: String = "",
        durationMs: Long = 0L,
        playCount: Int = 1,
    ) = PlaybackStatisticsEntry(
        mediaId = mediaId,
        available = true,
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

    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
    ) = Song(
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
