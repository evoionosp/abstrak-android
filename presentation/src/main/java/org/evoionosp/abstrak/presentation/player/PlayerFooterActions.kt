package org.evoionosp.abstrak.presentation.player
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.player.sheets.PlayerSheet

private val FooterActionPadding = 4.dp

@Composable
internal fun PlayerFooterActions(
    sleepLabel: String,
    speedLabel: String,
    onOpenSheet: (PlayerSheet) -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            FooterValueAction(
                value = sleepLabel,
                label = stringResource(R.string.now_playing_sleep),
                onClick = { onOpenSheet(PlayerSheet.SLEEP) },
            )
            FooterValueAction(
                value = speedLabel,
                label = stringResource(R.string.now_playing_speed),
                onClick = { onOpenSheet(PlayerSheet.SPEED) },
            )
            FooterAction(
                label = stringResource(R.string.now_playing_chapters),
                labelStyle = MaterialTheme.typography.labelSmall,
                spacing = 4.dp,
                onClick = { onOpenSheet(PlayerSheet.CHAPTERS) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) { measurables, constraints ->
        val slotWidth = constraints.maxWidth / measurables.size
        val itemWidth =
            measurables
                .maxOf { it.maxIntrinsicWidth(constraints.maxHeight) }
                .coerceAtMost(slotWidth)
        val placeables =
            measurables.map {
                it.measure(constraints.copy(minWidth = itemWidth, maxWidth = itemWidth, minHeight = 0))
            }
        val height = placeables.maxOf { it.height }.coerceAtLeast(constraints.minHeight)
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = constraints.maxWidth * (2 * index + 1) / (2 * placeables.size) - itemWidth / 2,
                    y = (height - placeable.height) / 2,
                )
            }
        }
    }
}

@Composable
private fun FooterValueAction(
    value: String,
    label: String,
    onClick: () -> Unit,
) {
    FooterAction(
        label = label,
        labelStyle = MaterialTheme.typography.bodySmall,
        spacing = 6.dp,
        onClick = onClick,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FooterAction(
    label: String,
    labelStyle: TextStyle,
    spacing: Dp,
    onClick: () -> Unit,
    top: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(FooterActionPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        top()
        Text(
            text = label,
            style = labelStyle,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
