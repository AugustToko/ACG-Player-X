package top.geek_studio.chenlongcould.musicplayer.playback

import top.geek_studio.chenlongcould.musicplayer.data.ListeningDataGeneration
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackProgressUpdate

/** Immutable evidence captured on the player thread before asynchronous persistence. */
internal data class PlaybackAnalyticsBatch(
    val generation: ListeningDataGeneration,
    val playedMediaId: String?,
    val playedAtMs: Long,
    val progress: PlaybackProgressUpdate?,
)

internal class PlaybackAnalyticsSampler {
    private var tracker = PlaybackProgressTracker()
    private var generation: ListeningDataGeneration? = null
    private var lastRecordedPlayingMediaId: String? = null

    fun onMediaItemTransition() {
        lastRecordedPlayingMediaId = null
    }

    fun sample(
        sample: PlaybackProgressSample,
        currentGeneration: ListeningDataGeneration,
    ): PlaybackAnalyticsBatch? {
        val previousGeneration = generation
        if (previousGeneration != null && previousGeneration.statistics != currentGeneration.statistics) {
            // Discard, do not flush, listening accumulated before clear/replace. Keep the play-start
            // marker: clearing statistics is not a new play, and clearing recent is not a new session.
            tracker = PlaybackProgressTracker()
        }
        generation = currentGeneration

        val mediaId = sample.mediaId?.trim()?.takeIf(String::isNotEmpty)
        if (mediaId == null) lastRecordedPlayingMediaId = null
        val playedMediaId =
            mediaId?.takeIf { sample.isPlaying && it != lastRecordedPlayingMediaId }
        if (playedMediaId != null) lastRecordedPlayingMediaId = playedMediaId

        val progress = tracker.update(sample)
        if (playedMediaId == null && progress == null) return null
        return PlaybackAnalyticsBatch(
            generation = currentGeneration,
            playedMediaId = playedMediaId,
            playedAtMs = sample.wallClockMs.coerceAtLeast(0L),
            progress = progress,
        )
    }
}
