package org.evoionosp.abstrak.presentation.navigation
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry

private const val DURATION_MS = 300
private const val OUTGOING_MS = 105
private const val INCOMING_MS = DURATION_MS - OUTGOING_MS
internal val SharedAxisSlideDistance = 30.dp

internal enum class NavMotion {
    FADE_THROUGH,
    SHARED_AXIS_FORWARD,
    SHARED_AXIS_BACKWARD,
}

internal fun navMotion(
    fromRoute: String?,
    toRoute: String?,
    isPop: Boolean,
): NavMotion =
    when {
        isRootTab(fromRoute) && isRootTab(toRoute) -> NavMotion.FADE_THROUGH
        isPop || toRoute == AppRoute.Auth.route -> NavMotion.SHARED_AXIS_BACKWARD
        else -> NavMotion.SHARED_AXIS_FORWARD
    }

private fun isRootTab(route: String?): Boolean = rootNavItems.any { it.route.route == route }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.navEnterTransition(
    isPop: Boolean,
    slideDistance: Int,
): EnterTransition =
    when (navMotion(initialState.destination.route, targetState.destination.route, isPop)) {
        NavMotion.FADE_THROUGH -> fadeThroughIn()
        NavMotion.SHARED_AXIS_FORWARD -> sharedAxisXIn(initialOffsetX = slideDistance)
        NavMotion.SHARED_AXIS_BACKWARD -> sharedAxisXIn(initialOffsetX = -slideDistance)
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.navExitTransition(
    isPop: Boolean,
    slideDistance: Int,
): ExitTransition =
    when (navMotion(initialState.destination.route, targetState.destination.route, isPop)) {
        NavMotion.FADE_THROUGH -> fadeThroughOut()
        NavMotion.SHARED_AXIS_FORWARD -> sharedAxisXOut(targetOffsetX = -slideDistance)
        NavMotion.SHARED_AXIS_BACKWARD -> sharedAxisXOut(targetOffsetX = slideDistance)
    }

private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(INCOMING_MS, delayMillis = OUTGOING_MS, easing = LinearOutSlowInEasing)) +
        scaleIn(tween(INCOMING_MS, delayMillis = OUTGOING_MS, easing = LinearOutSlowInEasing), initialScale = 0.92f)

private fun fadeThroughOut(): ExitTransition = fadeOut(tween(OUTGOING_MS, easing = FastOutLinearInEasing))

private fun sharedAxisXIn(initialOffsetX: Int): EnterTransition =
    slideInHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { initialOffsetX } +
        fadeIn(tween(INCOMING_MS, delayMillis = OUTGOING_MS, easing = LinearOutSlowInEasing))

private fun sharedAxisXOut(targetOffsetX: Int): ExitTransition =
    slideOutHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { targetOffsetX } +
        fadeOut(tween(OUTGOING_MS, easing = FastOutLinearInEasing))
