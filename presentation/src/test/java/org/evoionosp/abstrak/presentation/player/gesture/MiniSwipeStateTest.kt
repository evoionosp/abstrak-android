package org.evoionosp.abstrak.presentation.player.gesture
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MiniSwipeStateTest {
    private val distance = 1000f

    @Test
    fun `short slow swipe springs back`() {
        assertEquals(0f, resolveMiniSwipeTarget(offsetPx = 300f, distancePx = distance, velocityXPxPerS = 0f), 0f)
    }

    @Test
    fun `slow swipe past the threshold dismisses toward its side`() {
        assertEquals(distance, resolveMiniSwipeTarget(500f, distance, 0f), 0f)
        assertEquals(-distance, resolveMiniSwipeTarget(-500f, distance, 0f), 0f)
    }

    @Test
    fun `fling outward dismisses even a short swipe`() {
        assertEquals(-distance, resolveMiniSwipeTarget(-100f, distance, -2000f), 0f)
    }

    @Test
    fun `fling back toward the middle keeps the card even past the threshold`() {
        assertEquals(0f, resolveMiniSwipeTarget(600f, distance, -2000f), 0f)
    }

    @Test
    fun `card follows the finger both ways, so a swipe can be reversed`() {
        val state = MiniSwipeState(CoroutineScope(Dispatchers.Unconfined)) {}
        state.dragBy(450f)
        assertEquals(450f, state.offsetPx, 0f)
        state.dragBy(-400f)
        assertEquals(50f, state.offsetPx, 0f)
        state.dragBy(-150f)
        assertEquals(-100f, state.offsetPx, 0f)
    }

    @Test
    fun `a dismissing release stops the book only once the card is off screen`() =
        runTest {
            var dismissals = 0
            val state = MiniSwipeState(CoroutineScope(coroutineContext + FrameClock())) { dismissals++ }
            state.dragBy(600f)
            state.onDragEnd(velocityXPxPerS = 0f, distancePx = distance)
            advanceTimeBy(FRAME_MS * 3 + 1)
            assertTrue("expected mid-slide, at ${state.offsetPx}", state.offsetPx > 600f && state.offsetPx < distance)
            assertEquals(0, dismissals)
            advanceUntilIdle()
            assertEquals(distance, state.offsetPx, 0f)
            assertEquals(1, dismissals)
        }

    @Test
    fun `catching the card mid-slide keeps the book playing`() =
        runTest {
            var dismissals = 0
            val state = MiniSwipeState(CoroutineScope(coroutineContext + FrameClock())) { dismissals++ }
            state.dragBy(600f)
            state.onDragEnd(velocityXPxPerS = 0f, distancePx = distance)
            advanceTimeBy(FRAME_MS * 3 + 1)
            val caughtAt = state.offsetPx
            state.dragBy(-10f)
            advanceUntilIdle()
            assertEquals(caughtAt - 10f, state.offsetPx, 0f)
            assertEquals(0, dismissals)
        }

    @Test
    fun `a release short of the threshold springs back and keeps the book playing`() =
        runTest {
            var dismissals = 0
            val state = MiniSwipeState(CoroutineScope(coroutineContext + FrameClock())) { dismissals++ }
            state.dragBy(300f)
            state.onDragEnd(velocityXPxPerS = 0f, distancePx = distance)
            advanceUntilIdle()
            assertEquals(0f, state.offsetPx, 0f)
            assertEquals(0, dismissals)
        }

    private class FrameClock : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(FRAME_MS)
            frameTimeNanos += FRAME_MS * 1_000_000
            return onFrame(frameTimeNanos)
        }
    }

    private companion object {
        const val FRAME_MS = 16L
    }
}
