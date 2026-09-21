package top.geek_studio.chenlongcould.musicplayer.widget

import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.playback.PlaybackService

class TogglePlaybackWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        controlPlayback(context, glanceId) { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else if (controller.mediaItemCount > 0) {
                controller.play()
            }
        }
    }
}

class PreviousPlaybackWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        controlPlayback(context, glanceId) { controller ->
            if (
                controller.currentPosition > RESTART_THRESHOLD_MS ||
                !controller.hasPreviousMediaItem()
            ) {
                controller.seekTo(0L)
            } else {
                controller.seekToPreviousMediaItem()
            }
        }
    }
}

class NextPlaybackWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        controlPlayback(context, glanceId) { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else if (controller.mediaItemCount > 0) {
                controller.seekTo(0L)
            }
        }
    }
}

private suspend fun controlPlayback(
    context: Context,
    glanceId: GlanceId,
    command: (MediaController) -> Unit,
) {
    val appContext = context.applicationContext
    val sessionToken =
        SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
    val controllerFuture =
        MediaController.Builder(appContext, sessionToken)
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()

    val controller =
        try {
            withContext(Dispatchers.IO) {
                controllerFuture.get(CONTROLLER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        } catch (throwable: Throwable) {
            controllerFuture.cancel(true)
            throw throwable
        }

    try {
        val snapshot =
            withContext(Dispatchers.Main.immediate) {
                command(controller)
                controller.toPlaybackWidgetState()
            }
        PlaybackWidgetStateStore(appContext).write(snapshot)
        PlaybackWidget().update(appContext, glanceId)
    } finally {
        withContext(Dispatchers.Main.immediate) {
            controller.release()
        }
    }
}

private const val CONTROLLER_TIMEOUT_SECONDS = 15L
private const val RESTART_THRESHOLD_MS = 3_000L
