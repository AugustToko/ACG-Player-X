package top.geek_studio.chenlongcould.musicplayer.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import top.geek_studio.chenlongcould.musicplayer.MainActivity

class PlaybackWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val state = PlaybackWidgetStateStore(context).read()
        val openPlayerIntent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        provideContent {
            PlaybackWidgetContent(
                state = state,
                openPlayerIntent = openPlayerIntent,
            )
        }
    }
}

class PlaybackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlaybackWidget()
}

@Composable
private fun PlaybackWidgetContent(
    state: PlaybackWidgetState,
    openPlayerIntent: Intent,
) {
    val backgroundColor =
        ColorProvider(
            day = Color(0xFFFFF8FF),
            night = Color(0xFF1D1B20),
        )
    val titleColor =
        ColorProvider(
            day = Color(0xFF1D1B20),
            night = Color(0xFFE6E0E9),
        )
    val subtitleColor =
        ColorProvider(
            day = Color(0xFF49454F),
            night = Color(0xFFCAC4D0),
        )
    val openPlayerAction = actionStartActivity(openPlayerIntent)

    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp),
    ) {
        Text(
            text = state.title,
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .clickable(openPlayerAction),
            style =
                TextStyle(
                    color = titleColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = state.artist,
            modifier = GlanceModifier.fillMaxWidth(),
            style =
                TextStyle(
                    color = subtitleColor,
                    fontSize = 13.sp,
                ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(12.dp))

        if (state.hasMedia) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Button(
                    text = "上一首",
                    onClick = actionRunCallback<PreviousPlaybackWidgetAction>(),
                )
                Spacer(GlanceModifier.width(8.dp))
                Button(
                    text = if (state.isPlaying) "暂停" else "播放",
                    onClick = actionRunCallback<TogglePlaybackWidgetAction>(),
                )
                Spacer(GlanceModifier.width(8.dp))
                Button(
                    text = "下一首",
                    onClick = actionRunCallback<NextPlaybackWidgetAction>(),
                    enabled = state.hasNext,
                )
            }
        } else {
            Button(
                text = "打开播放器",
                onClick = openPlayerAction,
            )
        }
    }
}
