package org.evoionosp.abstrak.presentation.ui.catalog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.message.LocalSnackbarController
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

internal object UnsupportedBookDefaults {
    const val CONTENT_ALPHA = 0.38f
    val CoverFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

@Composable
internal fun rememberCatalogBookClick(onOpenAudiobook: (AudiobookUiModel) -> Unit): (AudiobookUiModel) -> Unit {
    val snackbar = LocalSnackbarController.current
    val currentOnOpenAudiobook by rememberUpdatedState(onOpenAudiobook)
    return remember(snackbar) {
        { audiobook ->
            if (audiobook.hasAudio) {
                currentOnOpenAudiobook(audiobook)
            } else {
                snackbar.warning(R.string.catalog_ebook_not_supported)
            }
        }
    }
}
