package org.evoionosp.abstrak.presentation.player.transport
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R

private const val PLAY_PAUSE_PAUSED_CORNER_FRACTION = 0.5f
private const val PLAY_PAUSE_PLAYING_CORNER_FRACTION = 0.23f

internal fun playPauseCorner(
    isPlaying: Boolean,
    buttonSize: Dp,
): Dp =
    if (isPlaying) {
        buttonSize * PLAY_PAUSE_PLAYING_CORNER_FRACTION
    } else {
        buttonSize * PLAY_PAUSE_PAUSED_CORNER_FRACTION
    }

internal enum class PlayPauseGlyph { PLAY, PAUSE, LOADING }

internal fun playPauseGlyph(
    isPlaying: Boolean,
    isLoading: Boolean,
): PlayPauseGlyph =
    when {
        isLoading -> PlayPauseGlyph.LOADING
        isPlaying -> PlayPauseGlyph.PAUSE
        else -> PlayPauseGlyph.PLAY
    }

@Composable
internal fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 28.dp,
) {
    val corner by animateDpAsState(
        targetValue = playPauseCorner(isPlaying, buttonSize),
        label = "PlayPauseCorner",
    )
    Box(
        modifier =
            modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = playPauseGlyph(isPlaying, isLoading),
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.6f)) using
                    SizeTransform(clip = false)
            },
            contentAlignment = Alignment.Center,
            label = "PlayPauseIcon",
        ) { glyph ->
            when (glyph) {
                PlayPauseGlyph.LOADING -> {
                    val loading = stringResource(R.string.now_playing_loading)
                    ContainedLoadingIndicator(
                        containerColor = Color.Transparent,
                        indicatorColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(buttonSize).semantics { contentDescription = loading },
                    )
                }

                PlayPauseGlyph.PAUSE, PlayPauseGlyph.PLAY -> {
                    val playing = glyph == PlayPauseGlyph.PAUSE
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription =
                            stringResource(if (playing) R.string.now_playing_pause else R.string.now_playing_play),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
        }
    }
}
