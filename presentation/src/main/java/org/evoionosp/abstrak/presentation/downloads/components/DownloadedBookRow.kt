package org.evoionosp.abstrak.presentation.downloads.components
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.menu.AbstrakDropdownMenu
import org.evoionosp.abstrak.presentation.downloads.DownloadedBookStatus
import org.evoionosp.abstrak.presentation.downloads.DownloadedBookUiModel
import org.evoionosp.abstrak.presentation.ui.catalog.AudiobookListRow
import org.evoionosp.abstrak.presentation.ui.format.formatStorageSize

@Composable
internal fun DownloadedBookRow(
    book: DownloadedBookUiModel,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AudiobookListRow(
        audiobook = book.audiobook,
        detail = book.detailLabel(),
        onClick = onClick,
        onClickLabel =
            when {
                !selectionMode -> null
                selected -> stringResource(R.string.downloads_deselect)
                else -> stringResource(R.string.downloads_select)
            },
        onLongClick = onLongClick.takeUnless { selectionMode },
        onLongClickLabel = stringResource(R.string.downloads_select),
        selected = selected,
        modifier = if (selectionMode) modifier.semantics { this.selected = selected } else modifier,
        trailing =
            if (selectionMode) {
                null
            } else {
                { DownloadMenu(bookTitle = book.audiobook.title, onDelete = onDelete) }
            },
    )
}

@Composable
private fun DownloadedBookUiModel.detailLabel(): String {
    val size = formatStorageSize(sizeBytes)
    val status =
        when (status) {
            DownloadedBookStatus.DOWNLOADED -> {
                null
            }

            DownloadedBookStatus.DOWNLOADING -> {
                progressPercent?.let { stringResource(R.string.downloads_status_downloading_percent, it) }
                    ?: stringResource(R.string.downloads_status_downloading)
            }

            DownloadedBookStatus.FAILED -> {
                stringResource(R.string.downloads_status_failed)
            }
        }
    return if (status == null) size else stringResource(R.string.downloads_size_with_status, size, status)
}

@Composable
private fun DownloadMenu(
    bookTitle: String,
    onDelete: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.downloads_more_options, bookTitle),
            )
        }
        AbstrakDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.downloaded_delete_action)) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}
