package org.evoionosp.abstrak.presentation.ui.catalog
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
internal fun AudiobookListCard(
    audiobook: AudiobookUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        AudiobookListRow(
            audiobook = audiobook,
            detail =
                if (audiobook.hasAudio) {
                    audiobook.durationLabel.ifBlank { stringResource(R.string.unknown_duration) }
                } else {
                    stringResource(R.string.catalog_ebook)
                },
            onClick = onClick,
            contentPadding = PaddingValues(),
            coverShape = RectangleShape,
            enabled = enabled,
        )
    }
}
