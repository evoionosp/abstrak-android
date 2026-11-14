package org.evoionosp.abstrak.presentation.player.gesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange

internal fun Modifier.sheetDragGesture(sheetDrag: SheetDragState): Modifier =
    pointerInput(sheetDrag) {
        val tracker = VelocityTracker()
        detectVerticalDragGestures(
            onDragStart = { tracker.resetTracking() },
            onDragEnd = { sheetDrag.onDragEnd(tracker.calculateVelocity().y) },
            onDragCancel = { sheetDrag.onDragCancel() },
            onVerticalDrag = { change, dragDelta ->
                tracker.addPointerInputChange(change)
                sheetDrag.dragBy(dragDelta)
            },
        )
    }
