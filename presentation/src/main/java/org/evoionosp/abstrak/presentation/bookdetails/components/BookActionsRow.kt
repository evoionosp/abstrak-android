package org.evoionosp.abstrak.presentation.bookdetails.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.bookdetails.BookDetailsAction
import org.evoionosp.abstrak.presentation.ui.download.DownloadIconButton
import org.evoionosp.abstrak.presentation.ui.download.DownloadIconButtonStyle
import org.evoionosp.abstrak.presentation.ui.download.DownloadUiState

@Composable
internal fun BookActionsRow(
    hasProgress: Boolean,
    download: DownloadUiState,
    onAction: (BookDetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { onAction(BookDetailsAction.Play) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(text = stringResource(if (hasProgress) R.string.continue_listening else R.string.start_listening))
        }
        DownloadIconButton(
            state = download,
            onDownload = { onAction(BookDetailsAction.DownloadBook) },
            onCancel = { onAction(BookDetailsAction.CancelBookDownload) },
            onDelete = { onAction(BookDetailsAction.DeleteBookDownload) },
            style = DownloadIconButtonStyle.PRIMARY,
        )
    }
}
