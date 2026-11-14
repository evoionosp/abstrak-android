package org.evoionosp.abstrak.presentation.ui.message
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.staticCompositionLocalOf
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType

data class SnackbarMessage(
    val text: UiText,
    val type: SnackbarType = SnackbarType.INFO,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)

@Singleton
class SnackbarController
    @Inject
    constructor() {
        private val queue = Channel<SnackbarMessage>(Channel.BUFFERED)
        val messages: Flow<SnackbarMessage> = queue.receiveAsFlow()

        fun show(message: SnackbarMessage) {
            queue.trySend(message)
        }

        fun info(text: UiText) = show(SnackbarMessage(text, SnackbarType.INFO))

        fun success(text: UiText) = show(SnackbarMessage(text, SnackbarType.SUCCESS))

        fun warning(text: UiText) = show(SnackbarMessage(text, SnackbarType.WARNING))

        fun error(text: UiText) = show(SnackbarMessage(text, SnackbarType.ERROR))

        fun info(
            @StringRes id: Int,
        ) = info(UiText.Resource(id))

        fun success(
            @StringRes id: Int,
        ) = success(UiText.Resource(id))

        fun warning(
            @StringRes id: Int,
        ) = warning(UiText.Resource(id))

        fun error(
            @StringRes id: Int,
        ) = error(UiText.Resource(id))
    }

val LocalSnackbarController =
    staticCompositionLocalOf<SnackbarController> {
        error("No SnackbarController provided")
    }
