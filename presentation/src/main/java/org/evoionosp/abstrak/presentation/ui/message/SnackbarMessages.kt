package org.evoionosp.abstrak.presentation.ui.message
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.AbstrakSnackbarVisuals

@Composable
fun ShowSnackbarMessages(
    controller: SnackbarController,
    hostState: SnackbarHostState,
) {
    val resources = LocalResources.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(controller, hostState, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            controller.messages.collectLatest { message ->
                hostState.showSnackbar(
                    AbstrakSnackbarVisuals(
                        message = message.text.resolve(resources),
                        type = message.type,
                        duration = message.duration,
                    ),
                )
            }
        }
    }
}
