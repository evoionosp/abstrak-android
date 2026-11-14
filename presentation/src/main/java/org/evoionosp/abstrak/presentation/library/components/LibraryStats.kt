package org.evoionosp.abstrak.presentation.library.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.card.TonalCard
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing

@Composable
internal fun LibraryStats(
    booksCount: Int,
    authorsCount: Int,
    hoursCount: Double,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AbstrakSpacing.catalog),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatCard(
                title = stringResource(R.string.library_stats_books),
                value = booksCount.toString(),
                modifier = Modifier.fillParentMaxWidth(0.35f),
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.library_stats_authors),
                value = authorsCount.toString(),
                modifier = Modifier.fillParentMaxWidth(0.35f),
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.library_stats_hours),
                value = String.format("%.1f", hoursCount),
                modifier = Modifier.fillParentMaxWidth(0.35f),
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    TonalCard(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
