package top.geek_studio.chenlongcould.musicplayer.playback

import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.widget.PlaybackWidget
import top.geek_studio.chenlongcould.musicplayer.widget.PlaybackWidgetStateStore
import top.geek_studio.chenlongcould.musicplayer.widget.toPlaybackWidgetState

class PlaybackService : MediaSessionService() {
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private lateinit var playbackStateStore: PlaybackStateStore
    private lateinit var playbackWidgetStateStore: PlaybackWidgetStateStore
    private var mediaSession: MediaSession? = null
    private var queueSaveJob: Job? = null
    private var positionSaveJob: Job? = null
    private var widgetUpdateJob: Job? = null

    private val playerListener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                    persistQueue()
                }
                if (
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                    events.contains(Player.EVENT_REPEAT_MODE_CHANGED) ||
                    events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) ||
                    events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                ) {
                    persistPosition()
                }
                if (
                    events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) ||
                    events.contains(Player.EVENT_IS_PLAYING_CHANGED)
                ) {
                    publishWidgetState()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()

        playbackStateStore = PlaybackStateStore(this)
        playbackWidgetStateStore = PlaybackWidgetStateStore(this)
        val audioAttributes =
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

        player =
            ExoPlayer.Builder(this)
                .build()
                .apply {
                    setAudioAttributes(audioAttributes, true)
                    setHandleAudioBecomingNoisy(true)
                    addListener(playerListener)
                }

        mediaSession =
            MediaSession.Builder(this, player)
                .build()

        publishWidgetState()
        restorePlaybackState()
        startPeriodicPositionPersistence()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        playbackStateStore.savePositionAsync(player.capturePositionSnapshot())
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        queueSaveJob?.cancel()
        positionSaveJob?.cancel()
        widgetUpdateJob?.cancel()
        playbackStateStore.savePositionAsync(player.capturePositionSnapshot())
        playbackWidgetStateStore.write(
            player.toPlaybackWidgetState().copy(isPlaying = false),
        )
        player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun restorePlaybackState() {
        serviceScope.launch {
            val restored =
                withContext(Dispatchers.IO) {
                    playbackStateStore.restore()
                } ?: return@launch

            if (player.mediaItemCount != 0) return@launch

            player.repeatMode = restored.repeatMode
            player.shuffleModeEnabled = restored.shuffleEnabled
            player.setMediaItems(
                restored.mediaItems,
                restored.currentIndex,
                restored.positionMs,
            )
            player.prepare()
            player.pause()
            publishWidgetState()
        }
    }

    private fun persistQueue() {
        queueSaveJob?.cancel()
        queueSaveJob =
            serviceScope.launch {
                delay(QUEUE_SAVE_DEBOUNCE_MS)
                // Capture after the debounce so rapid repeat/shuffle changes cannot be overwritten
                // by the stale state that existed when the timeline event first arrived.
                val snapshot = player.captureQueueSnapshot()
                val queuePersisted =
                    withContext(Dispatchers.IO) {
                        playbackStateStore.saveQueue(snapshot)
                    }
                // Capture once more after the durable queue write. This closes the small window in
                // which playback modes can change while SharedPreferences is committing the queue.
                if (queuePersisted) {
                    persistPosition()
                }
            }
    }

    private fun persistPosition() {
        val snapshot = player.capturePositionSnapshot()
        positionSaveJob?.cancel()
        positionSaveJob =
            serviceScope.launch {
                withContext(Dispatchers.IO) {
                    playbackStateStore.savePosition(snapshot)
                }
            }
    }

    private fun publishWidgetState() {
        val snapshot = player.toPlaybackWidgetState()
        widgetUpdateJob?.cancel()
        widgetUpdateJob =
            serviceScope.launch {
                delay(WIDGET_UPDATE_DEBOUNCE_MS)
                withContext(Dispatchers.IO) {
                    playbackWidgetStateStore.write(snapshot)
                }
                PlaybackWidget().updateAll(this@PlaybackService)
            }
    }

    private fun startPeriodicPositionPersistence() {
        serviceScope.launch {
            while (isActive) {
                delay(POSITION_SAVE_INTERVAL_MS)
                if (player.mediaItemCount > 0) {
                    persistPosition()
                }
            }
        }
    }

    private companion object {
        const val QUEUE_SAVE_DEBOUNCE_MS = 300L
        const val WIDGET_UPDATE_DEBOUNCE_MS = 180L
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
    }
}
