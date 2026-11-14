package org.evoionosp.abstrak.presentation.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.list.AbstrakListItemDefaults
import org.evoionosp.abstrak.presentation.ui.format.formatStorageSize

@Composable
internal fun StorageSpaceSummary(
    storage: StorageUsage,
    modifier: Modifier = Modifier,
    colors: StorageSpaceBarColors = StorageSpaceBarDefaults.colors(),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(AbstrakListItemDefaults.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.storage_space_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text =
                    stringResource(
                        R.string.storage_space_summary,
                        formatStorageSize(storage.abstrakBytes),
                        formatStorageSize(storage.deviceFreeBytes),
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StorageSpaceBar(storage = storage, colors = colors)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StorageLegendItem(
                color = colors.downloads,
                label = stringResource(R.string.storage_space_downloads),
                bytes = storage.downloadsBytes,
            )
            StorageLegendItem(
                color = colors.cache,
                label = stringResource(R.string.storage_space_cache),
                bytes = storage.cacheBytes,
            )
            StorageLegendItem(
                color = colors.free,
                label = stringResource(R.string.storage_space_free),
                bytes = storage.deviceFreeBytes,
            )
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    bytes: Long,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatStorageSize(bytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
