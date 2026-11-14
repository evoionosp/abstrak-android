package org.evoionosp.abstrak.presentation.player.transport
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
internal fun rememberHapticTick(): (action: () -> Unit) -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        { action ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            action()
        }
    }
}
