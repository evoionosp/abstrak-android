package org.evoionosp.abstrak.presentation.player.overlay
import androidx.compose.ui.unit.dp

internal val MINI_SIDE_PADDING = 8.dp
internal val MINI_CORNER_RADIUS = 16.dp
internal val MINI_BOTTOM_GAP = 8.dp
internal val MINI_FALLBACK_HEIGHT = 64.dp
internal val MINI_SHADOW_ELEVATION = 8.dp
internal val MINI_OUTLINE_WIDTH = 1.dp
internal val FULL_PARALLAX_DISTANCE = 28.dp
private const val MINI_GONE_BELOW_FRACTION = 0.15f
internal const val MINI_VISIBLE_BELOW_FRACTION = 0.18f
private const val FULL_FADES_ABOVE_FRACTION = 0.22f
internal const val FULL_VISIBLE_ABOVE_FRACTION = 0.20f
internal const val MINI_TAP_BELOW_FRACTION = 0.15f

internal data class SheetMorphMetrics(
    val maxHeightPx: Float,
    val miniHeightPx: Float,
    val bottomInsetPx: Float,
    val miniSidePaddingPx: Float,
    val miniCornerRadiusPx: Float,
    val miniShadowElevationPx: Float,
    val parallaxDistancePx: Float,
) {
    val collapsedTopPx: Float
        get() = (maxHeightPx - bottomInsetPx - miniHeightPx).coerceAtLeast(0f)
}

internal data class SheetMorphGeometry(
    val cardTopPx: Float,
    val cardHeightPx: Float,
    val sidePaddingPx: Float,
    val cornerRadiusPx: Float,
    val shadowElevationPx: Float,
    val outlineAlpha: Float,
    val miniAlpha: Float,
    val fullAlpha: Float,
    val parallaxPx: Float,
)

internal fun sheetMorphGeometry(
    fraction: Float,
    metrics: SheetMorphMetrics,
): SheetMorphGeometry {
    val collapsed = 1f - fraction
    val cardTopPx = collapsed * metrics.collapsedTopPx
    val cardBottomPx = metrics.maxHeightPx - metrics.bottomInsetPx * collapsed
    return SheetMorphGeometry(
        cardTopPx = cardTopPx,
        cardHeightPx = (cardBottomPx - cardTopPx).coerceAtLeast(0f),
        sidePaddingPx = metrics.miniSidePaddingPx * collapsed,
        cornerRadiusPx = metrics.miniCornerRadiusPx * collapsed,
        shadowElevationPx = metrics.miniShadowElevationPx * collapsed,
        outlineAlpha = collapsed,
        miniAlpha = (1f - fraction / MINI_GONE_BELOW_FRACTION).coerceIn(0f, 1f),
        fullAlpha =
            ((fraction - FULL_FADES_ABOVE_FRACTION) / (1f - FULL_FADES_ABOVE_FRACTION)).coerceIn(0f, 1f),
        parallaxPx = metrics.parallaxDistancePx * collapsed,
    )
}
