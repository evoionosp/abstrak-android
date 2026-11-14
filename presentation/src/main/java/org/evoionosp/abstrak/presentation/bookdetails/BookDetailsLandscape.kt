package org.evoionosp.abstrak.presentation.bookdetails
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.bookdetails.components.BookActionsRow
import org.evoionosp.abstrak.presentation.bookdetails.components.bookChapterItems
import org.evoionosp.abstrak.presentation.ui.book.BookArtworkPanel

private val PANE_HORIZONTAL_PADDING = 24.dp

@Composable
internal fun BookDetailsLandscape(
    audiobook: Audiobook,
    state: BookDetailsUiState,
    onAction: (BookDetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookArtworkPanel(
            coverUrl = audiobook.coverUrl,
            title = audiobook.title,
            author = audiobook.author,
            progress = state.bookProgress,
            modifier = Modifier.weight(0.4f),
        )
        Column(
            modifier =
                Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
        ) {
            BookActionsRow(
                hasProgress = state.hasProgress,
                download = state.bookDownload,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = PANE_HORIZONTAL_PADDING),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.now_playing_chapters),
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PANE_HORIZONTAL_PADDING),
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (state.chapters.isEmpty()) {
                Text(
                    text = stringResource(R.string.now_playing_chapters_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = PANE_HORIZONTAL_PADDING),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = miniTrailPadding),
                ) {
                    bookChapterItems(state = state, onAction = onAction, horizontalPadding = PANE_HORIZONTAL_PADDING)
                }
            }
        }
    }
}
