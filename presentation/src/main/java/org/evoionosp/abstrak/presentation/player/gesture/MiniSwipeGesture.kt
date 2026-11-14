package org.evoionosp.abstrak.presentation.player.gesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange

internal fun Modifier.miniSwipeGesture(
    miniSwipe: MiniSwipeState,
    enabled: Boolean,
    distancePx: Float,
): Modifier =
    pointerInput(miniSwipe, enabled, distancePx) {
        if (!enabled) return@pointerInput
        val tracker = VelocityTracker()
        var dragging = false
        try {
            detectHorizontalDragGestures(
                onDragStart = {
                    dragging = true
                    tracker.resetTracking()
                },
                onDragEnd = {
                    dragging = false
                    miniSwipe.onDragEnd(tracker.calculateVelocity().x, distancePx)
                },
                onDragCancel = {
                    dragging = false
                    miniSwipe.onDragCancel()
                },
                onHorizontalDrag = { change, dragDelta ->
                    tracker.addPointerInputChange(change)
                    miniSwipe.dragBy(dragDelta)
                },
            )
        } finally {
            if (dragging) miniSwipe.onDragCancel()
        }
    }
