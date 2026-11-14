package org.evoionosp.abstrak.presentation.player.transport
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Date
import kotlin.math.floor
import kotlinx.coroutines.delay
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.player.PlayerAction

private const val MINUTE_MS = 60_000L

@Composable
internal fun PlayerTransport(
    chapterTitle: String?,
    chapterTimeLeftMs: Long?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    Column(modifier = modifier) {
        if (!chapterTitle.isNullOrBlank()) {
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            if (chapterTimeLeftMs != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text =
                        stringResource(
                            R.string.now_playing_chapter_ends_at,
                            rememberEndsAtLabel(chapterTimeLeftMs, isPlaying),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        PlayerSeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeekTo = { onAction(PlayerAction.SeekTo(it)) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sizes = transportButtonSizes(maxWidth)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChapterSkipButton(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.now_playing_previous_chapter),
                    onClick = { tick { onAction(PlayerAction.PreviousChapter) } },
                )
                SeekButton(
                    direction = SeekDirection.BACKWARD,
                    onClick = { onAction(PlayerAction.SeekBackward) },
                    diameter = sizes.seek,
                )
                PlayPauseButton(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    onClick = { tick { onAction(PlayerAction.TogglePlayPause) } },
                    buttonSize = sizes.playPause,
                    iconSize = sizes.playPauseIcon,
                )
                SeekButton(
                    direction = SeekDirection.FORWARD,
                    onClick = { onAction(PlayerAction.SeekForward) },
                    diameter = sizes.seek,
                )
                ChapterSkipButton(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.now_playing_next_chapter),
                    onClick = { tick { onAction(PlayerAction.NextChapter) } },
                )
            }
        }
    }
}

@Composable
private fun rememberEndsAtLabel(
    timeLeftMs: Long,
    isPlaying: Boolean,
): String {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormat = remember(context, is24Hour) { DateFormat.getTimeFormat(context) }
    val endsAtMs by produceState(System.currentTimeMillis() + timeLeftMs, timeLeftMs, isPlaying) {
        value = System.currentTimeMillis() + timeLeftMs
        while (!isPlaying) {
            delay(MINUTE_MS - System.currentTimeMillis() % MINUTE_MS)
            value = System.currentTimeMillis() + timeLeftMs
        }
    }
    return timeFormat.format(Date(endsAtMs))
}

internal data class TransportButtonSizes(
    val seek: Dp,
    val playPause: Dp,
) {
    val playPauseIcon: Dp get() = playPause * PLAY_PAUSE_ICON_FRACTION
}

internal val TransportMinGap = 12.dp
private val TransportTouchTarget = 48.dp
private val PlayPauseFullSize = 96.dp
private const val PLAY_PAUSE_ICON_FRACTION = 56f / 96f

internal fun transportButtonSizes(width: Dp): TransportButtonSizes {
    val seekFull = SeekButtonSize.FULL.size
    val room = width - TransportTouchTarget * 2 - TransportMinGap * 4
    val full = seekFull * 2 + PlayPauseFullSize
    if (room >= full) return TransportButtonSizes(seek = seekFull, playPause = PlayPauseFullSize)
    val seek = (seekFull * (room / full)).floored().coerceAtLeast(TransportTouchTarget)
    val playPause = (room - seek * 2).floored().coerceIn(TransportTouchTarget, PlayPauseFullSize)
    return TransportButtonSizes(seek = seek, playPause = playPause)
}

private fun Dp.floored(): Dp = floor(value).dp

@Composable
private fun ChapterSkipButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(TransportTouchTarget),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(32.dp),
        )
    }
}
