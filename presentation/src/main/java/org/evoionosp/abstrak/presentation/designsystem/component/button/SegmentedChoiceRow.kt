package org.evoionosp.abstrak.presentation.designsystem.component.button
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A pill of mutually exclusive options. [icon], when given, leads each option's label in every
 * state; the label already names the option, so the icon has no content description.
 */
@Composable
fun <T> SegmentedChoiceRow(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector)? = null,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedChoiceDefaults.colors(),
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                onClick = { onSelect(option) },
                selected = isSelected(option),
                enabled = enabled,
                label = { Text(text = label(option)) },
                colors = colors,
                icon = {
                    if (icon != null) {
                        Icon(
                            imageVector = icon(option),
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    }
                },
            )
        }
    }
}

object SegmentedChoiceDefaults {
    @Composable
    fun colors(inactiveBorderColor: Color = MaterialTheme.colorScheme.outline): SegmentedButtonColors =
        SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.primary,
            activeContentColor = MaterialTheme.colorScheme.onPrimary,
            activeBorderColor = MaterialTheme.colorScheme.primary,
            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            inactiveBorderColor = inactiveBorderColor,
        )
}
