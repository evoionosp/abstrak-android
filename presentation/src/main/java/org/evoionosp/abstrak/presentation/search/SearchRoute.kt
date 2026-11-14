package org.evoionosp.abstrak.presentation.search
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.ui.catalog.rememberCatalogBookClick
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
fun SearchRoute(
    onOpenSettings: () -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    bottomBarPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val queryState = rememberTextFieldState()
    LaunchedEffect(queryState, viewModel) {
        snapshotFlow { queryState.text.toString() }.collect(viewModel::onQueryChange)
    }
    SearchScreen(
        state = state,
        queryState = queryState,
        bottomBarPadding = bottomBarPadding,
        onOpenSettings = onOpenSettings,
        onOpenAudiobook = rememberCatalogBookClick(onOpenAudiobook),
        modifier = modifier,
    )
}
