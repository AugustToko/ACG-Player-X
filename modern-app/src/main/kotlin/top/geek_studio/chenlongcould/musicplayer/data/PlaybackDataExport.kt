package top.geek_studio.chenlongcould.musicplayer.data

/**
 * Compatibility model for callers that only need the persisted playback counters.
 * Rich exports with current song metadata use [PlaybackStatisticsSnapshot].
 */
data class PlaybackDataExport(
    val version: Int = PLAYBACK_STATISTICS_EXPORT_SCHEMA_VERSION,
    val exportedAtMs: Long,
    val statistics: List<PlaybackStatisticEntry>,
)

data class PlaybackStatisticEntry(
    val mediaId: String,
    val playCount: Int,
    val completedCount: Int,
    val totalListenTimeMs: Long,
    val lastPlayedAtMs: Long,
    val lastCompletedAtMs: Long,
    val lastPositionMs: Long,
    val durationMs: Long,
)

object PlaybackDataExporter {
    fun encode(
        stats: Map<String, PlaybackStats>,
        exportedAtMs: Long = System.currentTimeMillis(),
    ): String =
        encodePlaybackStatistics(
            snapshot =
                buildPlaybackStatisticsSnapshot(
                    songs = emptyList(),
                    playbackStats = stats,
                    generatedAtMs = exportedAtMs,
                ),
            format = PlaybackStatisticsExportFormat.JSON,
        )
}
