package org.evoionosp.abstrak.presentation.ui.time
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * The current time, updated every [tickMillis] while the screen is started and straight away
 * when it starts again, for labels such as "5 minutes ago" that should not go stale.
 */
@Composable
fun rememberCurrentTimeMillis(tickMillis: Long = 60_000L): Long {
    val lifecycleOwner = LocalLifecycleOwner.current
    val now by produceState(System.currentTimeMillis(), lifecycleOwner, tickMillis) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = System.currentTimeMillis()
                delay(tickMillis)
            }
        }
    }
    return now
}
