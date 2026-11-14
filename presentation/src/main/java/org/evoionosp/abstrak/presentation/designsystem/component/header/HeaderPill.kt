package org.evoionosp.abstrak.presentation.designsystem.component.header
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.designsystem.component.button.TonalCircleIconButtonDefaults

@Composable
fun HeaderPill(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = color,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = modifier.heightIn(min = HeaderPillDefaults.Height),
        content = content,
    )
}

@Composable
fun HeaderPillTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

object HeaderPillDefaults {
    val Height = TonalCircleIconButtonDefaults.Size
    val ContentPadding = PaddingValues(horizontal = 16.dp)
}
