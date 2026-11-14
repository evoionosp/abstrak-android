package org.evoionosp.abstrak.presentation.player.transport
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.playback.PlaybackConnection
import org.evoionosp.abstrak.presentation.R

internal enum class SeekDirection(
    val seconds: Int,
) {
    BACKWARD((PlaybackConnection.SEEK_BACKWARD_MS / 1000).toInt()),
    FORWARD((PlaybackConnection.SEEK_FORWARD_MS / 1000).toInt()),
}

internal enum class SeekButtonSize(
    val size: Dp,
) {
    MINI(36.dp),
    FULL(64.dp),
}

@Composable
internal fun SeekButton(
    direction: SeekDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: SeekButtonSize = SeekButtonSize.FULL,
    diameter: Dp = size.size,
) {
    val description =
        pluralStringResource(
            if (direction == SeekDirection.BACKWARD) {
                R.plurals.now_playing_seek_backward
            } else {
                R.plurals.now_playing_seek_forward
            },
            direction.seconds,
            direction.seconds,
        )
    val labelStyle: TextStyle =
        when (size) {
            SeekButtonSize.MINI -> MaterialTheme.typography.labelLarge
            SeekButtonSize.FULL -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        }
    Box(
        modifier =
            modifier
                .size(diameter)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = direction.seconds.toString(),
            style = labelStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
