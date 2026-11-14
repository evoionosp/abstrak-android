package org.evoionosp.abstrak.presentation.home
import androidx.compose.runtime.Composable
import org.evoionosp.abstrak.presentation.ui.event.ForegroundRefreshEffect
import org.evoionosp.abstrak.presentation.ui.event.ObserveAsEvents

@Composable
internal fun CatalogEffects(
    viewModel: HomeViewModel,
    onSessionExpired: () -> Unit,
) {
    ForegroundRefreshEffect(onForeground = viewModel::refreshSilently)
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeUiEvent.SessionExpired -> onSessionExpired()
        }
    }
}
