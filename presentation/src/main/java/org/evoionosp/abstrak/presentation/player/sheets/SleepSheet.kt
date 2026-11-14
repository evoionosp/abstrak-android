package org.evoionosp.abstrak.presentation.player.sheets
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.playback.SleepTimerState
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.list.SwitchListItem
import org.evoionosp.abstrak.presentation.designsystem.component.picker.ValuePicker
import org.evoionosp.abstrak.presentation.player.PlayerAction

private val SLEEP_PRESETS = listOf(15, 30, 45, 60, 90)
private const val PRESET_SNAP_THRESHOLD_MINUTES = 2f

internal fun formatSleepDuration(minutes: Int): String = "$minutes:00"

internal fun formatSleepRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs.coerceAtLeast(0) + 999) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun formatSleepPreset(minutes: Int): String = "${minutes}m"

internal fun snapSleepMinutes(raw: Float): Int {
    val nearestPreset = SLEEP_PRESETS.minBy { abs(it - raw) }
    return if (abs(nearestPreset - raw) <= PRESET_SNAP_THRESHOLD_MINUTES) {
        nearestPreset
    } else {
        raw.roundToInt().coerceIn(
            AppSettings.MIN_SLEEP_TIMER_MINUTES,
            AppSettings.MAX_SLEEP_TIMER_MINUTES,
        )
    }
}

@Composable
internal fun SleepSheet(
    sleepTimer: SleepTimerState,
    sleepRemainingMs: Long?,
    currentChapterTitle: String?,
    endOfChapterAvailable: Boolean,
    persistedMinutes: Int,
    onAction: (PlayerAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val timerArmed = sleepTimer is SleepTimerState.Timer
    val chapterArmed = sleepTimer is SleepTimerState.EndOfChapter
    var minutes by rememberSaveable(sleepTimer, persistedMinutes) {
        mutableStateOf(stagedSleepMinutes(sleepTimer, sleepRemainingMs, persistedMinutes))
    }
    val timerControlsEnabled = sleepTimer == SleepTimerState.Off
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    PlayerBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.now_playing_sleep_sheet_title),
    ) {
        val chapterSwitch: @Composable () -> Unit = {
            SwitchListItem(
                headline = stringResource(R.string.now_playing_sleep_stop_at_end_of_chapter),
                checked = chapterArmed,
                onCheckedChange = { checked ->
                    onAction(if (checked) PlayerAction.ArmSleepEndOfChapter else PlayerAction.CancelSleepTimer)
                },
                supporting = currentChapterTitle,
                supportingMaxLines = 1,
                enabled = endOfChapterAvailable && !timerArmed,
                switchColors = SwitchDefaults.colors(uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant),
                contentPadding = PaddingValues(0.dp),
            )
        }
        val timerPicker: @Composable (Modifier) -> Unit = { modifier ->
            ValuePicker(
                valueLabel = sleepHeadline(sleepTimer, sleepRemainingMs, minutes),
                value = minutes.toFloat(),
                valueRange =
                    AppSettings.MIN_SLEEP_TIMER_MINUTES.toFloat()..AppSettings.MAX_SLEEP_TIMER_MINUTES.toFloat(),
                onValueChange = { minutes = snapSleepMinutes(it) },
                presets = SLEEP_PRESETS,
                isPresetSelected = { timerControlsEnabled && minutes == it },
                onPresetSelect = { preset ->
                    minutes = preset
                    onAction(PlayerAction.SaveSleepTimerMinutes(preset))
                },
                presetLabel = { formatSleepPreset(it) },
                modifier = modifier,
                onValueChangeFinished = { onAction(PlayerAction.SaveSleepTimerMinutes(minutes)) },
                enabled = timerControlsEnabled,
            )
        }
        val timerButton: @Composable () -> Unit = {
            Button(
                onClick = {
                    onAction(if (timerArmed) PlayerAction.CancelSleepTimer else PlayerAction.ArmSleepTimer(minutes))
                },
                enabled = !chapterArmed,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    if (timerArmed) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
            ) {
                Text(
                    text =
                        stringResource(
                            if (timerArmed) {
                                R.string.now_playing_sleep_stop_timer
                            } else {
                                R.string.now_playing_sleep_start_timer
                            },
                        ),
                )
            }
        }
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                timerPicker(Modifier.weight(1f))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    chapterSwitch()
                    timerButton()
                }
            }
        } else {
            chapterSwitch()
            timerPicker(Modifier.fillMaxWidth())
            timerButton()
        }
    }
}

private fun stagedSleepMinutes(
    sleepTimer: SleepTimerState,
    remainingMs: Long?,
    persistedMinutes: Int,
): Int {
    val remaining =
        if (sleepTimer is SleepTimerState.Timer) {
            remainingMs
        } else {
            null
        }
    return if (remaining != null) {
        ((remaining + 59_999) / 60_000).toInt()
    } else {
        persistedMinutes
    }.coerceIn(
        AppSettings.MIN_SLEEP_TIMER_MINUTES,
        AppSettings.MAX_SLEEP_TIMER_MINUTES,
    )
}

@Composable
private fun sleepHeadline(
    sleepTimer: SleepTimerState,
    remainingMs: Long?,
    minutes: Int,
): String {
    val chapterLabel = stringResource(R.string.now_playing_sleep_end_of_chapter)
    return when (sleepTimer) {
        is SleepTimerState.Timer -> {
            formatSleepRemaining(remainingMs ?: 0L)
        }

        is SleepTimerState.EndOfChapter -> {
            chapterLabel
        }

        SleepTimerState.Off -> {
            formatSleepDuration(minutes)
        }
    }
}
