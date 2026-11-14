package org.evoionosp.abstrak.presentation.settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.download.model.StorageUsage

@Composable
internal fun StorageSpaceBar(
    storage: StorageUsage,
    modifier: Modifier = Modifier,
    colors: StorageSpaceBarColors = StorageSpaceBarDefaults.colors(),
) {
    val values = listOf(storage.downloadsBytes, storage.cacheBytes, storage.deviceFreeBytes)
    val segmentColors = listOf(colors.downloads, colors.cache, colors.free)
    Canvas(modifier = modifier.fillMaxWidth().height(StorageSpaceBarDefaults.Height)) {
        val segments =
            barSegments(
                values = values,
                width = size.width,
                gap = StorageSpaceBarDefaults.Gap.toPx(),
                minWidth = StorageSpaceBarDefaults.MinSegmentWidth.toPx(),
            )
        val cornerRadius = CornerRadius(StorageSpaceBarDefaults.CornerRadius.toPx())
        segments.forEachIndexed { index, segment ->
            if (segment == null) return@forEachIndexed
            val left = if (layoutDirection == LayoutDirection.Rtl) size.width - segment.end else segment.start
            drawRoundRect(
                color = segmentColors[index],
                topLeft = Offset(left, 0f),
                size = Size(segment.width, size.height),
                cornerRadius = cornerRadius,
            )
        }
    }
}

internal data class BarSegment(
    val start: Float,
    val end: Float,
) {
    val width: Float get() = end - start
}

internal fun barSegments(
    values: List<Long>,
    width: Float,
    gap: Float,
    minWidth: Float,
): List<BarSegment?> {
    val shown = values.indices.filter { values[it] > 0L }
    val available = (width - gap * (shown.size - 1)).toDouble()
    if (shown.isEmpty() || available <= 0.0) return List(values.size) { null }
    val total = shown.sumOf { values[it].toDouble() }
    val shares = shown.map { available * values[it] / total }
    val floor = minOf(minWidth.toDouble(), available / shown.size)
    val deficit = shares.sumOf { (floor - it).coerceAtLeast(0.0) }
    val excess = shares.sumOf { (it - floor).coerceAtLeast(0.0) }
    val giveUp = if (excess > 0.0) (deficit / excess).coerceAtMost(1.0) else 0.0
    val widths = shares.map { if (it < floor) floor else floor + (it - floor) * (1 - giveUp) }
    val segments = arrayOfNulls<BarSegment>(values.size)
    var start = 0.0
    shown.forEachIndexed { i, index ->
        val end = if (i == shown.lastIndex) width.toDouble() else start + widths[i]
        segments[index] = BarSegment(start = start.toFloat(), end = end.toFloat())
        start = end + gap
    }
    return segments.asList()
}

@Immutable
internal data class StorageSpaceBarColors(
    val downloads: Color,
    val cache: Color,
    val free: Color,
)

internal object StorageSpaceBarDefaults {
    val Height = 16.dp
    val Gap = 4.dp
    val CornerRadius = 4.dp
    val MinSegmentWidth = 8.dp

    @Composable
    fun colors(
        downloads: Color = MaterialTheme.colorScheme.primary,
        cache: Color = MaterialTheme.colorScheme.tertiary,
        free: Color = MaterialTheme.colorScheme.secondaryFixed,
    ): StorageSpaceBarColors = StorageSpaceBarColors(downloads, cache, free)
}
