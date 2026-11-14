package org.evoionosp.abstrak.presentation.player.gesture
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val MINI_DISMISS_DRAG_FRACTION = 0.4f
const val MINI_DISMISS_FLING_VELOCITY_PX_PER_S = 1000f
private val MiniSwipeSpring =
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 1f,
    )

fun resolveMiniSwipeTarget(
    offsetPx: Float,
    distancePx: Float,
    velocityXPxPerS: Float,
): Float {
    if (abs(velocityXPxPerS) > MINI_DISMISS_FLING_VELOCITY_PX_PER_S) {
        return if (velocityXPxPerS * offsetPx < 0f) 0f else distancePx * sign(velocityXPxPerS)
    }
    return if (abs(offsetPx) > distancePx * MINI_DISMISS_DRAG_FRACTION) distancePx * sign(offsetPx) else 0f
}

@Stable
class MiniSwipeState(
    private val scope: CoroutineScope,
    private val onDismissed: () -> Unit,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set
    private var settleJob: Job? = null

    fun dragBy(deltaPx: Float) {
        settleJob?.cancel()
        offsetPx += deltaPx
    }

    fun onDragEnd(
        velocityXPxPerS: Float,
        distancePx: Float,
    ) {
        settleTo(resolveMiniSwipeTarget(offsetPx, distancePx, velocityXPxPerS), velocityXPxPerS)
    }

    fun onDragCancel() {
        settleTo(target = 0f, velocityXPxPerS = 0f)
    }

    private fun settleTo(
        target: Float,
        velocityXPxPerS: Float,
    ) {
        settleJob?.cancel()
        settleJob =
            scope.launch {
                Animatable(offsetPx).animateTo(
                    targetValue = target,
                    animationSpec = MiniSwipeSpring,
                    initialVelocity = velocityXPxPerS,
                ) {
                    offsetPx = value
                }
                if (target != 0f) onDismissed()
            }
    }
}
