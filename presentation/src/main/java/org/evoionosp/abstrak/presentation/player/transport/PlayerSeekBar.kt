package org.evoionosp.abstrak.presentation.player.transport
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToLong
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.format.msToDurationLabel

internal const val SEEK_LATCH_TIMEOUT_MS = 5_000L
internal const val SEEK_LATCH_TOLERANCE_FRACTION = 0.04f

internal fun progressFraction(
    positionMs: Long,
    durationMs: Long,
): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

internal fun shouldClearSeekTarget(
    elapsedMs: Long,
    diffFraction: Float,
): Boolean = elapsedMs > SEEK_LATCH_TIMEOUT_MS || diffFraction < SEEK_LATCH_TOLERANCE_FRACTION

@Composable
internal fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val progress = progressFraction(positionMs, durationMs)
    var dragValue by remember { mutableStateOf<Float?>(null) }
    var seekTarget by remember { mutableFloatStateOf(-1f) }
    var seekFinishedAt by remember { mutableLongStateOf(0L) }
    val lastHapticStep = remember { intArrayOf(-1) }
    LaunchedEffect(durationMs) {
        dragValue = null
        seekTarget = -1f
        seekFinishedAt = 0L
    }
    LaunchedEffect(progress) {
        if (dragValue != null) return@LaunchedEffect
        val target = seekTarget
        if (target < 0f) return@LaunchedEffect
        if (
            shouldClearSeekTarget(
                System.currentTimeMillis() - seekFinishedAt,
                abs(progress - target),
            )
        ) {
            seekTarget = -1f
        }
    }
    val displayFraction = dragValue ?: if (seekTarget >= 0f) seekTarget else progress
    val displayState = rememberUpdatedState(displayFraction)
    val displayedMs =
        (displayFraction * durationMs).roundToLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
    val positionLabel = ((displayedMs / 1000) * 1000).msToDurationLabel()
    val durationLabel = durationMs.coerceAtLeast(0L).msToDurationLabel()
    Column(modifier = modifier.fillMaxWidth()) {
        WavySliderExpressive(
            value = { displayState.value },
            onValueChange = { newFraction ->
                dragValue = newFraction
                val quantized = (newFraction.coerceIn(0f, 1f) * 20f).toInt()
                if (quantized != lastHapticStep[0]) {
                    lastHapticStep[0] = quantized
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onValueCommit = { finalFraction ->
                seekTarget = finalFraction
                seekFinishedAt = System.currentTimeMillis()
                dragValue = null
                onSeekTo(
                    (finalFraction * durationMs).roundToLong().coerceIn(0L, durationMs.coerceAtLeast(0L)),
                )
            },
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thumbColor = MaterialTheme.colorScheme.primary,
            isPlaying = isPlaying,
            trackEdgePadding = 0.dp,
            semanticsLabel = stringResource(R.string.now_playing_position),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = positionLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = durationLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
