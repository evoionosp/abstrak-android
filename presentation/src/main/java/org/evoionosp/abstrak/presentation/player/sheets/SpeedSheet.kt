package org.evoionosp.abstrak.presentation.player.sheets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.picker.ValuePicker
import org.evoionosp.abstrak.presentation.ui.format.formatPlaybackSpeed

private const val MIN_SPEED = 0.5f
private const val MAX_SPEED = 3.0f
private const val SPEED_STEP = 0.05f
private const val STOP_SNAP_THRESHOLD = 0.04f
private val SPEED_STOPS = listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
private val SPEED_PRESETS = listOf(0.75f, 0.9f, 1.0f, 1.25f, 1.5f)

private fun Float.snapPlaybackSpeed(): Float {
    val nearestStop = SPEED_STOPS.minBy { abs(it - this) }
    return if (abs(nearestStop - this) <= STOP_SNAP_THRESHOLD) {
        nearestStop
    } else {
        (this / SPEED_STEP).roundToInt() * SPEED_STEP
    }
}

@Composable
internal fun SpeedSheet(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.now_playing_speed_sheet_title),
    ) {
        ValuePicker(
            valueLabel = formatPlaybackSpeed(speed),
            value = speed,
            valueRange = MIN_SPEED..MAX_SPEED,
            onValueChange = { onSpeedChange(it.snapPlaybackSpeed()) },
            presets = SPEED_PRESETS,
            isPresetSelected = { abs(speed - it) < 0.001f },
            onPresetSelect = onSpeedChange,
            presetLabel = { formatPlaybackSpeed(it) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
