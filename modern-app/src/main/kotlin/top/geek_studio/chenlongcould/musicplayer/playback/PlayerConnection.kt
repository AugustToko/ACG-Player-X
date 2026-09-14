package top.geek_studio.chenlongcould.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.model.toMediaItem

data class PlaybackUiState(
    val isConnected: Boolean = false,
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

class PlayerConnection(
    context: Context,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val sessionToken =
        SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
    private val controllerFuture =
        MediaController.Builder(appContext, sessionToken)
            .buildAsync()

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var pendingAction: ((MediaController) -> Unit)? = null
    private var closed = false

    private val listener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                syncFrom(player)
            }
        }

    private val progressJob: Job =
        scope.launch {
            while (isActive) {
                controller?.let(::syncProgress)
                delay(if (_state.value.isPlaying) 500L else 1_000L)
            }
        }

    init {
        controllerFuture.addListener(
            {
                if (closed) return@addListener

                runCatching { controllerFuture.get() }
                    .onSuccess { mediaController ->
                        controller = mediaController
                        mediaController.addListener(listener)
                        syncFrom(mediaController)
                        pendingAction?.invoke(mediaController)
                        pendingAction = null
                    }
                    .onFailure {
                        _state.value = PlaybackUiState(isConnected = false)
                    }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun play(
        song: Song,
        queue: List<Song>,
    ) {
        withController { player ->
            val mediaItems = queue.map(Song::toMediaItem)
            val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

            player.setMediaItems(mediaItems, startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() {
        withController { player ->
            if (player.isPlaying) {
                player.pause()
            } else if (player.mediaItemCount > 0) {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        withController { player ->
            val duration = normalizedTime(player.duration)
            player.seekTo(positionMs.coerceIn(0L, duration.takeIf { it > 0L } ?: Long.MAX_VALUE))
        }
    }

    fun skipNext() {
        withController { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else {
                player.seekTo(0L)
            }
        }
    }

    fun skipPrevious() {
        withController { player ->
            if (player.currentPosition > RESTART_THRESHOLD_MS || !player.hasPreviousMediaItem()) {
                player.seekTo(0L)
            } else {
                player.seekToPreviousMediaItem()
            }
        }
    }

    fun toggleShuffle() {
        withController { player ->
            player.shuffleModeEnabled = !player.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        withController { player ->
            player.repeatMode =
                when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
        }
    }

    override fun close() {
        closed = true
        progressJob.cancel()
        pendingAction = null

        controller?.run {
            removeListener(listener)
            release()
        }
        controller = null
        controllerFuture.cancel(true)
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let(action) ?: run {
            pendingAction = action
        }
    }

    private fun syncFrom(player: Player) {
        val metadata = player.mediaMetadata

        _state.value =
            PlaybackUiState(
                isConnected = true,
                mediaId = player.currentMediaItem?.mediaId,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(),
                artworkUri = metadata.artworkUri,
                isPlaying = player.isPlaying,
                positionMs = normalizedTime(player.currentPosition),
                durationMs = normalizedTime(player.duration),
                bufferedPositionMs = normalizedTime(player.bufferedPosition),
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
            )
    }

    private fun syncProgress(player: Player) {
        val current = _state.value
        val position = normalizedTime(player.currentPosition)
        val duration = normalizedTime(player.duration)
        val buffered = normalizedTime(player.bufferedPosition)

        if (
            position != current.positionMs ||
                duration != current.durationMs ||
                buffered != current.bufferedPositionMs ||
                player.isPlaying != current.isPlaying
        ) {
            _state.value =
                current.copy(
                    isPlaying = player.isPlaying,
                    positionMs = position,
                    durationMs = duration,
                    bufferedPositionMs = buffered,
                )
        }
    }

    private fun normalizedTime(value: Long): Long =
        value
            .takeUnless { it == C.TIME_UNSET || it < 0L }
            ?: 0L

    private companion object {
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
