package top.geek_studio.chenlongcould.musicplayer.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PlaybackDataExport(
    val version: Int = CURRENT_VERSION,
    val exportedAtMs: Long,
    val statistics: List<PlaybackStatisticEntry>,
)

@Serializable
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
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    fun encode(
        stats: Map<String, PlaybackStats>,
        exportedAtMs: Long,
    ): String =
        json.encodeToString(
            PlaybackDataExport(
                exportedAtMs = exportedAtMs,
                statistics =
                    stats
                        .asSequence()
                        .filter { (id, value) ->
                            id.isNotBlank() &&
                                (value.playCount > 0 ||
                                    value.completedCount > 0 ||
                                    value.totalListenTimeMs > 0L)
                        }
                        .map { (id, value) ->
                            PlaybackStatisticEntry(
                                mediaId = id,
                                playCount = value.playCount,
                                completedCount = value.completedCount,
                                totalListenTimeMs = value.totalListenTimeMs,
                                lastPlayedAtMs = value.lastPlayedAtMs,
                                lastCompletedAtMs = value.lastCompletedAtMs,
                                lastPositionMs = value.lastPositionMs,
                                durationMs = value.durationMs,
                            )
                        }
                        .sortedBy { it.mediaId }
                        .toList(),
            ),
        )

    private const val CURRENT_VERSION = 1
}
