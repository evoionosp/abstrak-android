package org.evoionosp.abstrak.presentation.ui.download
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.dialog.ConfirmDialog
import org.evoionosp.abstrak.presentation.ui.format.formatStorageSize

@Composable
fun DeleteDownloadDialog(
    itemNames: List<String>,
    freedBytes: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean = false,
) {
    val freed = formatStorageSize(freedBytes)
    val itemName = itemNames.singleOrNull()
    ConfirmDialog(
        title =
            if (itemName != null) {
                stringResource(R.string.delete_download_title)
            } else {
                pluralStringResource(R.plurals.delete_downloads_title, itemNames.size, itemNames.size)
            },
        message =
            if (itemName != null) {
                stringResource(R.string.delete_download_message, itemName, freed)
            } else {
                stringResource(R.string.delete_downloads_message, freed)
            },
        confirmLabel = stringResource(R.string.delete_action),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
        inProgress = isDeleting,
    )
}
