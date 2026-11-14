package org.evoionosp.abstrak.presentation.downloads.components
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.header.HeaderPill
import org.evoionosp.abstrak.presentation.designsystem.component.header.HeaderPillDefaults
import org.evoionosp.abstrak.presentation.ui.catalog.RootHeader

@Composable
internal fun DownloadsHeader(
    selectedCount: Int,
    onOpenSettings: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelecting = selectedCount > 0
    val headerModifier = Modifier.heightIn(min = HeaderPillDefaults.Height)
    Box(modifier = modifier) {
        RootHeader(
            onOpenSettings = onOpenSettings,
            modifier =
                if (isSelecting) {
                    headerModifier.alpha(0f).clearAndSetSemantics {}
                } else {
                    headerModifier
                },
        ) {
            Text(
                text = stringResource(R.string.downloads_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        if (isSelecting) {
            SelectionBar(
                selectedCount = selectedCount,
                onClearSelection = onClearSelection,
                onSelectAll = onSelectAll,
                onDeleteSelected = onDeleteSelected,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HeaderPill(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.downloads_clear_selection),
                )
            }
            Text(
                text = pluralStringResource(R.plurals.downloads_selected_count, selectedCount, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Rounded.SelectAll,
                    contentDescription = stringResource(R.string.downloads_select_all),
                )
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.downloads_delete_selected),
                )
            }
        }
    }
}
