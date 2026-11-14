package org.evoionosp.abstrak.presentation.ui.event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    onEvent: suspend CoroutineScope.(T) -> Unit,
) {
    LaunchedEffect(flow) {
        flow.collectLatest { event ->
            onEvent(event)
        }
    }
}
