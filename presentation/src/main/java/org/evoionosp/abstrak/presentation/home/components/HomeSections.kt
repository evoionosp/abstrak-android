package org.evoionosp.abstrak.presentation.home.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.card.TonalCard
import org.evoionosp.abstrak.presentation.designsystem.component.header.SectionHeader
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakShapes
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.ui.catalog.AudiobookGridCard
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
internal fun SectionBlock(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.padding(horizontal = AbstrakSpacing.catalog),
        )
        content()
    }
}

@Composable
internal fun HorizontalBookRow(
    audiobooks: List<AudiobookUiModel>,
    onBookClick: (AudiobookUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (audiobooks.isEmpty()) {
        PlaceholderSectionCard(
            title = stringResource(R.string.home_section_empty_title),
            body = stringResource(R.string.home_section_empty_body),
            modifier = modifier,
        )
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = AbstrakSpacing.catalog),
    ) {
        items(
            items = audiobooks,
            key = { it.id },
        ) { audiobook ->
            AudiobookGridCard(
                audiobook = audiobook,
                onClick = { onBookClick(audiobook) },
                modifier = Modifier.width(150.dp),
                enabled = audiobook.hasAudio,
            )
        }
    }
}

@Composable
internal fun PlaceholderSectionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    TonalCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = AbstrakSpacing.catalog),
        shape = AbstrakShapes.card,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
