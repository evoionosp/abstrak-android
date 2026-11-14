package org.evoionosp.abstrak.presentation.designsystem.component.list
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing

@Composable
fun AbstrakListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingMaxLines: Int = Int.MAX_VALUE,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    colors: AbstrakListItemColors = AbstrakListItemDefaults.colors(),
    contentPadding: PaddingValues = AbstrakListItemDefaults.ContentPadding,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = colors.headlineColor,
            )
            if (supporting != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.supportingColor,
                    maxLines = supportingMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun NavigationListItem(
    headline: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingMaxLines: Int = Int.MAX_VALUE,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = { ListItemChevron() },
    colors: AbstrakListItemColors = AbstrakListItemDefaults.colors(),
) {
    AbstrakListItem(
        headline = headline,
        modifier = modifier.clickable(onClick = onClick),
        supporting = supporting,
        supportingMaxLines = supportingMaxLines,
        leading = leading,
        trailing = trailing,
        colors = colors,
    )
}

@Composable
fun SwitchListItem(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingMaxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    colors: AbstrakListItemColors = AbstrakListItemDefaults.colors(),
    switchColors: SwitchColors = SwitchDefaults.colors(),
    contentPadding: PaddingValues = AbstrakListItemDefaults.ContentPadding,
) {
    AbstrakListItem(
        headline = headline,
        modifier =
            modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        supporting = supporting,
        supportingMaxLines = supportingMaxLines,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = switchColors,
            )
        },
        colors = colors,
        contentPadding = contentPadding,
    )
}

@Composable
fun RadioListItem(
    headline: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    colors: AbstrakListItemColors = AbstrakListItemDefaults.colors(),
) {
    AbstrakListItem(
        headline = headline,
        modifier =
            modifier.selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        supporting = supporting,
        trailing = { RadioButton(selected = selected, onClick = null) },
        colors = colors,
    )
}

@Composable
fun ListItemChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Immutable
data class AbstrakListItemColors(
    val headlineColor: Color,
    val supportingColor: Color,
)

object AbstrakListItemDefaults {
    val ContentPadding = PaddingValues(horizontal = AbstrakSpacing.content, vertical = AbstrakSpacing.item)

    @Composable
    fun colors(
        headlineColor: Color = Color.Unspecified,
        supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ): AbstrakListItemColors = AbstrakListItemColors(headlineColor, supportingColor)
}
