package org.evoionosp.abstrak.presentation.player
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.player.overlay.MINI_CORNER_RADIUS
import org.evoionosp.abstrak.presentation.player.transport.PlayPauseButton
import org.evoionosp.abstrak.presentation.player.transport.SeekButton
import org.evoionosp.abstrak.presentation.player.transport.SeekButtonSize
import org.evoionosp.abstrak.presentation.player.transport.SeekDirection
import org.evoionosp.abstrak.presentation.player.transport.rememberHapticTick
import org.evoionosp.abstrak.presentation.ui.book.BookCover

@Composable
internal fun MiniPlayer(
    audiobook: Audiobook,
    chapterTitle: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    Row(
        modifier =
            modifier
                .height(64.dp)
                .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(
            coverUrl = audiobook.coverUrl,
            title = audiobook.title,
            author = audiobook.author,
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(MINI_CORNER_RADIUS)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            MarqueeText(
                text = chapterTitle ?: audiobook.title,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
            )
            MarqueeText(
                text = stringResource(R.string.now_playing_mini_subtitle, audiobook.title, audiobook.author),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        SeekButton(
            direction = SeekDirection.BACKWARD,
            onClick = { tick { onAction(PlayerAction.SeekBackward) } },
            size = SeekButtonSize.MINI,
        )
        Spacer(modifier = Modifier.width(8.dp))
        PlayPauseButton(
            isPlaying = isPlaying,
            isLoading = isLoading,
            onClick = { tick { onAction(PlayerAction.TogglePlayPause) } },
        )
        Spacer(modifier = Modifier.width(8.dp))
        SeekButton(
            direction = SeekDirection.FORWARD,
            onClick = { tick { onAction(PlayerAction.SeekForward) } },
            size = SeekButtonSize.MINI,
        )
    }
}

@Composable
private fun MarqueeText(
    text: String,
    style: TextStyle,
) {
    Text(
        text = text,
        style = style,
        maxLines = 1,
        modifier =
            Modifier
                .basicMarquee(iterations = Int.MAX_VALUE)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.08f to Color.Black,
                                0.92f to Color.Black,
                                1f to Color.Transparent,
                            ),
                        blendMode = BlendMode.DstIn,
                    )
                },
    )
}
