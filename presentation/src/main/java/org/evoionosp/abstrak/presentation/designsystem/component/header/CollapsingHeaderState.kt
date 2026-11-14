package org.evoionosp.abstrak.presentation.designsystem.component.header
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class CollapsingHeaderState internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    initialHeightPx: Float,
) {
    private val height = Animatable(initialHeightPx)
    var minHeightPx: Float = 0f
        private set
    var maxHeightPx: Float = initialHeightPx
        private set
    val heightPx: Float
        get() = height.value
    val collapseFraction: Float
        get() {
            val range = maxHeightPx - minHeightPx
            if (range <= 0f) return 1f
            return 1f - ((height.value - minHeightPx) / range).coerceIn(0f, 1f)
        }
    private val listAtTop: Boolean
        get() = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
    val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0
                if (!isScrollingDown && !listAtTop) return Offset.Zero
                val previousHeight = height.value
                val newHeight = (previousHeight + delta).coerceIn(minHeightPx, maxHeightPx)
                val consumed = newHeight - previousHeight
                if (consumed.roundToInt() != 0) {
                    scope.launch { height.snapTo(newHeight) }
                }
                val canConsumeScroll = !(isScrollingDown && newHeight == minHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }

    internal fun updateBounds(
        minHeightPx: Float,
        maxHeightPx: Float,
    ) {
        this.minHeightPx = minHeightPx
        this.maxHeightPx = maxHeightPx
    }

    internal fun settle() {
        val shouldExpand = height.value > (minHeightPx + maxHeightPx) / 2
        val target = if (shouldExpand && listAtTop) maxHeightPx else minHeightPx
        if (height.value != target) {
            scope.launch {
                height.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
            }
        }
    }
}

@Composable
fun rememberCollapsingHeaderState(
    listState: LazyListState,
    minHeight: Dp,
    maxHeight: Dp,
): CollapsingHeaderState {
    val density = LocalDensity.current
    val minHeightPx = with(density) { minHeight.toPx() }
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val scope = rememberCoroutineScope()
    val state = remember(listState) { CollapsingHeaderState(listState, scope, maxHeightPx) }
    state.updateBounds(minHeightPx, maxHeightPx)
    LaunchedEffect(state, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) state.settle()
    }
    return state
}
