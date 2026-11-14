package org.evoionosp.abstrak.presentation.downloads
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.state.EmptyState
import org.evoionosp.abstrak.presentation.downloads.components.DownloadedBookRow
import org.evoionosp.abstrak.presentation.downloads.components.DownloadsHeader
import org.evoionosp.abstrak.presentation.ui.catalog.RootHeaderDefaults
import org.evoionosp.abstrak.presentation.ui.catalog.RootScreenSurface
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

sealed interface DownloadsAction {
    data object OpenStorageSettings : DownloadsAction

    data class OpenBook(
        val audiobook: AudiobookUiModel,
    ) : DownloadsAction

    data class ToggleSelection(
        val audiobookId: String,
    ) : DownloadsAction

    data object SelectAll : DownloadsAction

    data object ClearSelection : DownloadsAction

    data class DeleteBook(
        val audiobookId: String,
    ) : DownloadsAction

    data object DeleteSelected : DownloadsAction
}

@Composable
fun DownloadsScreen(
    state: DownloadsUiState,
    bottomBarPadding: Dp,
    onAction: (DownloadsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    RootScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            DownloadsHeader(
                selectedCount = state.selectedIds.size,
                onOpenSettings = { onAction(DownloadsAction.OpenStorageSettings) },
                onClearSelection = { onAction(DownloadsAction.ClearSelection) },
                onSelectAll = { onAction(DownloadsAction.SelectAll) },
                onDeleteSelected = { onAction(DownloadsAction.DeleteSelected) },
                modifier = Modifier.padding(RootHeaderDefaults.PinnedPadding),
            )
            if (state.isLoaded) {
                if (state.books.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.downloads_empty_title),
                        subtitle = stringResource(R.string.downloads_empty_hint),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomBarPadding + 16.dp),
                    ) {
                        items(
                            items = state.books,
                            key = { it.audiobook.id },
                            contentType = { "downloaded_book" },
                        ) { book ->
                            val id = book.audiobook.id
                            DownloadedBookRow(
                                book = book,
                                selectionMode = state.isSelectionMode,
                                selected = id in state.selectedIds,
                                onClick = {
                                    onAction(
                                        if (state.isSelectionMode) {
                                            DownloadsAction.ToggleSelection(id)
                                        } else {
                                            DownloadsAction.OpenBook(book.audiobook)
                                        },
                                    )
                                },
                                onLongClick = { onAction(DownloadsAction.ToggleSelection(id)) },
                                onDelete = { onAction(DownloadsAction.DeleteBook(id)) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}
