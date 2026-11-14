package org.evoionosp.abstrak.presentation.player.gesture
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val SHEET_SETTLE_DRAG_FRACTION = 0.15f
const val SHEET_FLING_VELOCITY_PX_PER_S = 800f
private const val SHEET_OVERSHOOT_FRACTION = 0.08f
private val SheetSettleSpring =
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.0005f,
    )

enum class SheetTarget {
    EXPANDED,
    COLLAPSED,
}

fun resolveSheetTarget(
    startFraction: Float,
    endFraction: Float,
    velocityYPxPerS: Float,
): SheetTarget {
    if (abs(velocityYPxPerS) > SHEET_FLING_VELOCITY_PX_PER_S) {
        return if (velocityYPxPerS < 0f) SheetTarget.EXPANDED else SheetTarget.COLLAPSED
    }
    val dragged = endFraction - startFraction
    if (abs(dragged) > SHEET_SETTLE_DRAG_FRACTION) {
        return if (dragged > 0f) SheetTarget.EXPANDED else SheetTarget.COLLAPSED
    }
    return if (endFraction > 0.5f) SheetTarget.EXPANDED else SheetTarget.COLLAPSED
}

@Stable
class SheetDragState(
    private val scope: CoroutineScope,
    private val onCollapsed: () -> Unit,
    private val onExpanded: () -> Unit,
) {
    private var position by mutableFloatStateOf(0f)
    var travelKnown by mutableStateOf(false)
        private set
    private var travelPx by mutableFloatStateOf(0f)
    val expansionFraction: Float
        get() = position.coerceIn(0f, 1f)
    val overshootPx: Float
        get() = (position - 1f).coerceAtLeast(0f) * travelPx
    private var settleJob: Job? = null
    private var dragging = false
    private var dragStartFraction = 0f

    fun onTravelKnown(travelPx: Float) {
        if (travelPx <= 0f) return
        this.travelPx = travelPx
        travelKnown = true
    }

    fun dragBy(deltaPx: Float): Float {
        val travel = travelPx
        if (deltaPx == 0f || travel <= 0f) return 0f
        settleJob?.cancel()
        if (!dragging) {
            dragging = true
            dragStartFraction = expansionFraction
        }
        val old = position
        position = (old - deltaPx / travel).coerceIn(0f, 1f + SHEET_OVERSHOOT_FRACTION)
        return (old - position) * travel
    }

    fun onDragEnd(velocityYPxPerS: Float) {
        settleJob?.cancel()
        settleJob = scope.launch { settle(velocityYPxPerS) }
    }

    fun onDragCancel() {
        settleJob?.cancel()
        settleJob = scope.launch { settle(0f) }
    }

    suspend fun settle(velocityYPxPerS: Float) {
        dragging = false
        val travel = travelPx
        if (travel <= 0f) return
        val velocity = -velocityYPxPerS / travel
        when (resolveSheetTarget(dragStartFraction, expansionFraction, velocityYPxPerS)) {
            SheetTarget.EXPANDED -> {
                animateTo(1f, velocity)
                onExpanded()
            }

            SheetTarget.COLLAPSED -> {
                animateTo(0f, velocity)
                onCollapsed()
            }
        }
    }

    fun expand() {
        settleJob?.cancel()
        if (!travelKnown) return
        dragging = false
        settleJob = scope.launch { animateTo(1f) }
    }

    fun collapse() {
        settleJob?.cancel()
        if (!travelKnown) return
        dragging = false
        settleJob =
            scope.launch {
                animateTo(0f)
                onCollapsed()
            }
    }

    private suspend fun animateTo(
        target: Float,
        initialVelocity: Float = 0f,
    ) {
        if (position == target) return
        Animatable(position).animateTo(
            targetValue = target,
            animationSpec = SheetSettleSpring,
            initialVelocity = initialVelocity,
        ) {
            position = value
        }
    }
}
