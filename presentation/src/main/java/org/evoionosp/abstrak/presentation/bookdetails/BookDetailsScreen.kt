package org.evoionosp.abstrak.presentation.bookdetails
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.presentation.R

sealed interface BookDetailsAction {
    data object Play : BookDetailsAction

    data class PlayChapter(
        val chapter: AudiobookChapter,
    ) : BookDetailsAction

    data object DownloadBook : BookDetailsAction

    data object CancelBookDownload : BookDetailsAction

    data object DeleteBookDownload : BookDetailsAction

    data class DownloadChapter(
        val index: Int,
    ) : BookDetailsAction

    data class CancelChapterDownload(
        val index: Int,
    ) : BookDetailsAction

    data class DeleteChapterDownload(
        val index: Int,
    ) : BookDetailsAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailsScreen(
    state: BookDetailsUiState,
    onBackClick: () -> Unit,
    onAction: (BookDetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
) {
    val audiobook = state.audiobook
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        modifier = modifier,
        topBar = {
            if (isLandscape) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.preferences_back),
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        when {
            audiobook == null -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            isLandscape -> {
                BookDetailsLandscape(
                    audiobook = audiobook,
                    state = state,
                    onAction = onAction,
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .padding(start = 24.dp),
                    miniTrailPadding = miniTrailPadding,
                )
            }

            else -> {
                BookDetailsPortrait(
                    audiobook = audiobook,
                    state = state,
                    onAction = onAction,
                    onBackClick = onBackClick,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                    miniTrailPadding = miniTrailPadding,
                )
            }
        }
    }
}
