package top.geek_studio.chenlongcould.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import top.geek_studio.chenlongcould.musicplayer.playback.PlaybackUiState

@Composable
fun ArtworkPlaceholder(
    seed: Long,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glyphSize: Int = 48,
) {
    val palettes =
        listOf(
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.secondaryContainer,
        )
    val pair = palettes[Math.floorMod(seed, palettes.size.toLong()).toInt()]

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(Brush.linearGradient(listOf(pair.first, pair.second))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "♫",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = glyphSize.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun MiniPlayer(
    playback: PlaybackUiState,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlayer),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(
                artworkUri = playback.artworkUri,
                fallbackUri = playback.mediaUri,
                seed = playback.mediaId?.toLongOrNull() ?: 0L,
                modifier = Modifier.size(52.dp),
                cornerRadius = 14.dp,
                glyphSize = 24,
                requestSize = 96.dp,
                contentDescription = playback.title,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playback.title.ifBlank { "准备播放" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playback.artist.ifBlank { "ACG Player X" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledIconButton(
                onClick = onPlayPause,
                modifier =
                    Modifier.semantics {
                        contentDescription = if (playback.isPlaying) "暂停" else "播放"
                    },
            ) {
                Text(
                    text = if (playback.isPlaying) "Ⅱ" else "▶",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun PlaybackControls(
    playback: PlaybackUiState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            glyph = if (playback.shuffleEnabled) "随机✓" else "随机",
            description = "随机播放",
            onClick = onShuffle,
        )
        GlyphButton(
            glyph = "｜◀",
            description = "上一首",
            onClick = onPrevious,
        )
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
        ) {
            Text(
                text = if (playback.isPlaying) "Ⅱ" else "▶",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
        }
        GlyphButton(
            glyph = "▶｜",
            description = "下一首",
            onClick = onNext,
        )
        GlyphButton(
            glyph =
                when (playback.repeatMode) {
                    Player.REPEAT_MODE_ONE -> "单曲"
                    Player.REPEAT_MODE_ALL -> "循环"
                    else -> "顺序"
                },
            description = "切换循环模式",
            onClick = onRepeat,
        )
    }
}

@Composable
private fun GlyphButton(
    glyph: String,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
