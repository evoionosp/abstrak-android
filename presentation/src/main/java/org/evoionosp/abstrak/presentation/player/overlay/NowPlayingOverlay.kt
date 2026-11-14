package org.evoionosp.abstrak.presentation.player.overlay
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.player.gesture.MiniSwipeState
import org.evoionosp.abstrak.presentation.player.gesture.SheetDragState
import org.evoionosp.abstrak.presentation.player.gesture.miniSwipeGesture
import org.evoionosp.abstrak.presentation.player.gesture.sheetDragGesture
import org.evoionosp.abstrak.presentation.player.theme.PlayerTheme
import org.evoionosp.abstrak.presentation.player.theme.rememberPlayerColorScheme

@Composable
internal fun NowPlayingOverlay(
    targetExpanded: Boolean,
    sheetDrag: SheetDragState,
    playingAudiobook: Audiobook?,
    themeFromCover: Boolean,
    bottomInsetDp: Dp,
    onMinimize: () -> Unit,
    onExpandMini: () -> Unit,
    onDismissMini: () -> Unit,
    onMiniHeightKnown: (Dp) -> Unit,
    miniPlayer: @Composable (Audiobook) -> Unit,
    fullPlayer: @Composable (Audiobook) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnMinimize by rememberUpdatedState(onMinimize)
    val currentOnExpandMini by rememberUpdatedState(onExpandMini)
    val currentOnDismissMini by rememberUpdatedState(onDismissMini)
    val currentOnMiniHeightKnown by rememberUpdatedState(onMiniHeightKnown)
    var miniHeightPx by remember { mutableFloatStateOf(0f) }
    val notCollapsed by remember(sheetDrag) {
        derivedStateOf { sheetDrag.expansionFraction > 0f }
    }
    SheetBackHandler(open = playingAudiobook != null && (targetExpanded || notCollapsed)) {
        currentOnMinimize()
    }
    if (playingAudiobook == null) return
    PlayerTheme(colorScheme = rememberPlayerColorScheme(playingAudiobook, themeFromCover)) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val fullHeight = maxHeight
            val maxHeightPx = with(density) { fullHeight.toPx() }
            val metrics =
                with(density) {
                    SheetMorphMetrics(
                        maxHeightPx = maxHeightPx,
                        miniHeightPx = if (miniHeightPx > 0f) miniHeightPx else MINI_FALLBACK_HEIGHT.toPx(),
                        bottomInsetPx = (bottomInsetDp + MINI_BOTTOM_GAP).toPx(),
                        miniSidePaddingPx = MINI_SIDE_PADDING.toPx(),
                        miniCornerRadiusPx = MINI_CORNER_RADIUS.toPx(),
                        miniShadowElevationPx = MINI_SHADOW_ELEVATION.toPx(),
                        parallaxDistancePx = FULL_PARALLAX_DISTANCE.toPx(),
                    )
                }
            LaunchedEffect(metrics.collapsedTopPx) {
                sheetDrag.onTravelKnown(metrics.collapsedTopPx)
            }
            if (!sheetDrag.travelKnown) return@BoxWithConstraints
            val showMini by remember(sheetDrag) {
                derivedStateOf { sheetDrag.expansionFraction < MINI_VISIBLE_BELOW_FRACTION }
            }
            val showFull by remember(sheetDrag) {
                derivedStateOf { sheetDrag.expansionFraction > FULL_VISIBLE_ABOVE_FRACTION }
            }
            val miniTappable by remember(sheetDrag) {
                derivedStateOf { sheetDrag.expansionFraction < MINI_TAP_BELOW_FRACTION }
            }
            val miniSwipeEnabled by remember(sheetDrag) {
                derivedStateOf { sheetDrag.expansionFraction == 0f }
            }
            val miniSwipeScope = rememberCoroutineScope()
            val miniSwipe = remember { MiniSwipeState(miniSwipeScope) { currentOnDismissMini() } }
            val maxWidthPx = constraints.maxWidth.toFloat()
            val dismissLabel = stringResource(R.string.now_playing_dismiss)
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            val outlineWidthPx = with(density) { MINI_OUTLINE_WIDTH.toPx() }
            val geometry = { sheetMorphGeometry(sheetDrag.expansionFraction, metrics) }
            Box(
                modifier =
                    Modifier
                        .layout { measurable, constraints ->
                            val g = geometry()
                            val sidePx = g.sidePaddingPx.roundToInt()
                            val placeable =
                                measurable.measure(
                                    Constraints.fixed(
                                        width = (constraints.maxWidth - 2 * sidePx).coerceAtLeast(0),
                                        height = g.cardHeightPx.roundToInt(),
                                    ),
                                )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeRelative(sidePx, g.cardTopPx.roundToInt())
                            }
                        }.graphicsLayer {
                            val g = geometry()
                            translationY = -sheetDrag.overshootPx
                            translationX = miniSwipe.offsetPx
                            alpha = (1f - abs(miniSwipe.offsetPx) / maxWidthPx).coerceIn(0f, 1f)
                            shadowElevation = g.shadowElevationPx
                            shape = RoundedCornerShape(g.cornerRadiusPx)
                            clip = true
                        }.background(MaterialTheme.colorScheme.surface)
                        .drawWithContent {
                            drawContent()
                            val g = geometry()
                            if (g.outlineAlpha > 0f) {
                                val inset = outlineWidthPx / 2
                                drawRoundRect(
                                    color = outlineColor,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - outlineWidthPx, size.height - outlineWidthPx),
                                    cornerRadius = CornerRadius((g.cornerRadiusPx - inset).coerceAtLeast(0f)),
                                    alpha = g.outlineAlpha,
                                    style = Stroke(width = outlineWidthPx),
                                )
                            }
                        }.semantics { isTraversalGroup = true }
                        .sheetDragGesture(sheetDrag)
                        .miniSwipeGesture(miniSwipe, enabled = miniSwipeEnabled, distancePx = maxWidthPx),
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    if (showFull) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .requiredHeight(fullHeight)
                                    .graphicsLayer {
                                        val g = geometry()
                                        alpha = g.fullAlpha
                                        translationY = g.parallaxPx
                                    },
                        ) {
                            fullPlayer(playingAudiobook)
                        }
                    }
                    if (showMini) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        miniHeightPx = size.height.toFloat()
                                        currentOnMiniHeightKnown(with(density) { size.height.toDp() })
                                    }.graphicsLayer { alpha = geometry().miniAlpha }
                                    .clickable(
                                        enabled = miniTappable,
                                        onClick = { currentOnExpandMini() },
                                    ).semantics {
                                        customActions =
                                            listOf(
                                                CustomAccessibilityAction(dismissLabel) {
                                                    currentOnDismissMini()
                                                    true
                                                },
                                            )
                                    },
                        ) {
                            miniPlayer(playingAudiobook)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SheetBackHandler(
    open: Boolean,
    onMinimize: () -> Unit,
) {
    if (open) BackHandler(onBack = onMinimize)
}
