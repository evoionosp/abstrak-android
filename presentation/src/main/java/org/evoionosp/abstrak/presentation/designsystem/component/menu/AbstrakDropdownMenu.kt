package org.evoionosp.abstrak.presentation.designsystem.component.menu
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AbstrakDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = AbstrakDropdownMenuDefaults.Shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = AbstrakDropdownMenuDefaults.Elevation,
        shadowElevation = AbstrakDropdownMenuDefaults.Elevation,
        content = content,
    )
}

object AbstrakDropdownMenuDefaults {
    val Shape = RoundedCornerShape(16.dp)
    val Elevation = 8.dp
}
