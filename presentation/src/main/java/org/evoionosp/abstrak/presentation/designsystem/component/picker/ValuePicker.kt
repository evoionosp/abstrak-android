package org.evoionosp.abstrak.presentation.designsystem.component.picker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.designsystem.component.button.SegmentedChoiceDefaults
import org.evoionosp.abstrak.presentation.designsystem.component.button.SegmentedChoiceRow

@Composable
fun <T> ValuePicker(
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    presets: List<T>,
    isPresetSelected: (T) -> Boolean,
    onPresetSelect: (T) -> Unit,
    presetLabel: (T) -> String,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    sliderColors: SliderColors = ValuePickerDefaults.sliderColors(),
    presetColors: SegmentedButtonColors = ValuePickerDefaults.presetColors(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            colors = sliderColors,
        )
        SegmentedChoiceRow(
            options = presets,
            isSelected = isPresetSelected,
            onSelect = onPresetSelect,
            label = presetLabel,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            colors = presetColors,
        )
    }
}

object ValuePickerDefaults {
    @Composable
    fun sliderColors(): SliderColors =
        SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

    @Composable
    fun presetColors(): SegmentedButtonColors =
        SegmentedChoiceDefaults.colors(inactiveBorderColor = MaterialTheme.colorScheme.surfaceVariant)
}
