package org.evoionosp.abstrak.presentation.ui.download
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R

enum class DownloadIconButtonStyle {
    TONAL,
    PRIMARY,
}

@Composable
fun DownloadIconButton(
    state: DownloadUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    style: DownloadIconButtonStyle = DownloadIconButtonStyle.TONAL,
) {
    val look = style.look()
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state.status) {
            DownloadButtonStatus.NOT_DOWNLOADED -> {
                FilledIconButton(onClick = onDownload, modifier = look.buttonModifier, colors = look.colors) {
                    Icon(
                        imageVector = look.downloadIcon,
                        contentDescription = stringResource(R.string.download_action),
                        modifier = Modifier.size(look.iconSize),
                    )
                }
            }

            DownloadButtonStatus.DOWNLOADING -> {
                RingCenteredAction(
                    progressFraction = state.progressFraction,
                    onClick = onCancel,
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cancel_download),
                    colors = look.colors,
                )
            }

            DownloadButtonStatus.DOWNLOADED -> {
                FilledIconButton(onClick = onDelete, modifier = look.buttonModifier, colors = look.colors) {
                    Icon(
                        imageVector = Icons.Rounded.OfflinePin,
                        contentDescription = stringResource(R.string.downloaded_delete_action),
                        modifier = Modifier.size(look.iconSize),
                    )
                }
            }
        }
    }
}

private class DownloadButtonLook(
    val colors: IconButtonColors,
    val buttonModifier: Modifier,
    val downloadIcon: ImageVector,
    val iconSize: Dp,
)

@Composable
private fun DownloadIconButtonStyle.look(): DownloadButtonLook =
    when (this) {
        DownloadIconButtonStyle.TONAL -> {
            DownloadButtonLook(
                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                buttonModifier = Modifier,
                downloadIcon = Icons.Rounded.DownloadForOffline,
                iconSize = 20.dp,
            )
        }

        DownloadIconButtonStyle.PRIMARY -> {
            DownloadButtonLook(
                colors = IconButtonDefaults.filledIconButtonColors(),
                buttonModifier = Modifier.fillMaxSize(),
                downloadIcon = Icons.Rounded.Download,
                iconSize = 24.dp,
            )
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RingCenteredAction(
    progressFraction: Float?,
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    colors: IconButtonColors,
) {
    val progress = progressFraction
    if (progress != null) {
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(46.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        CircularWavyProgressIndicator(
            modifier = Modifier.size(46.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        colors = colors,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
        )
    }
}
