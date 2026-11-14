package org.evoionosp.abstrak.presentation.ui.catalog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.button.TonalCircleIconButton
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing

@Composable
fun RootHeader(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        title()
        TonalCircleIconButton(
            imageVector = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.settings_icon_desc),
            onClick = onOpenSettings,
        )
    }
}

object RootHeaderDefaults {
    val PinnedPadding =
        PaddingValues(start = AbstrakSpacing.catalog, end = AbstrakSpacing.catalog, top = 16.dp, bottom = 8.dp)
}
