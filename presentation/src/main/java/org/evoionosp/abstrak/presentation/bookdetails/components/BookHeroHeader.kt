package org.evoionosp.abstrak.presentation.bookdetails.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.book.BookCover

private val BACK_BUTTON_CENTER = 28.dp
private val TITLE_BOTTOM_INSET = 16.dp

@Composable
internal fun BookHeroHeader(
    audiobook: Audiobook,
    chaptersCount: Int,
    collapseFraction: Float,
    headerHeight: Dp,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = surfaceColor.luminance() < 0.5f
    val statusBarColor =
        if (isDarkTheme) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)
    val backgroundAlpha = collapseFraction
    val headerContentAlpha = 1f - (collapseFraction * 2).coerceAtMost(1f)
    val showExpandedArtwork = headerContentAlpha > 0.01f
    val headerOverlayBrush =
        remember(surfaceColor, headerContentAlpha) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color.Transparent,
                        surfaceColor.copy(alpha = 0.30f * headerContentAlpha),
                        surfaceColor.copy(alpha = 0.90f * headerContentAlpha),
                        surfaceColor.copy(alpha = headerContentAlpha),
                    ),
            )
        }
    val statusBarBrush =
        remember(statusBarColor) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        statusBarColor,
                        Color.Transparent,
                    ),
            )
        }
    val titleScale = lerp(1f, 0.75f, collapseFraction)
    val titlePaddingStart = lerp(24.dp, 64.dp, collapseFraction)
    val titleMaxLines = if (collapseFraction < 0.5f) 2 else 1
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val titleOrigin = TransformOrigin(pivotFractionX = if (isRtl) 1f else 0f, pivotFractionY = 0.5f)
    val metaLabel = bookMetaLabel(chaptersCount, audiobook.durationInSeconds)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(headerHeight)
                .clipToBounds(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(surfaceColor.copy(alpha = backgroundAlpha)),
        ) {
            if (showExpandedArtwork) {
                BookCover(
                    coverUrl = audiobook.coverUrl,
                    title = audiobook.title,
                    author = audiobook.author,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(headerOverlayBrush),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(statusBarBrush)
                        .align(Alignment.TopCenter),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
            ) {
                FilledIconButton(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 4.dp),
                    onClick = onBackClick,
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.preferences_back),
                    )
                }
                Column(
                    modifier =
                        Modifier
                            .heroTitlePlacement(collapseFraction)
                            .padding(start = titlePaddingStart, end = 16.dp)
                            .graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                                transformOrigin = titleOrigin
                            },
                ) {
                    Text(
                        text = audiobook.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = audiobook.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (metaLabel != null && headerContentAlpha > 0f) {
                        Text(
                            text = metaLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.partialHeight(headerContentAlpha),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun bookMetaLabel(
    chaptersCount: Int,
    durationSeconds: Long?,
): String? {
    val chapters =
        chaptersCount.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.book_details_chapters_count, it, it)
        }
    val length =
        durationSeconds?.let(::bookLength)?.let { (hours, minutes) ->
            listOfNotNull(
                hours.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.book_details_length_hours, it, it) },
                minutes.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.book_details_length_minutes, it, it) },
            ).joinToString(" ")
        }
    return listOfNotNull(chapters, length).takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

internal fun bookLength(seconds: Long): Pair<Int, Int>? {
    val totalMinutes = (seconds + 30) / 60
    return if (totalMinutes > 0) (totalMinutes / 60).toInt() to (totalMinutes % 60).toInt() else null
}

private fun Modifier.heroTitlePlacement(collapseFraction: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val top =
            heroTitleTop(
                collapseFraction = collapseFraction,
                containerHeight = constraints.maxHeight,
                blockHeight = placeable.height,
                bottomInset = TITLE_BOTTOM_INSET.roundToPx(),
                barCenter = BACK_BUTTON_CENTER.roundToPx(),
            )
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, top) }
    }

internal fun heroTitleTop(
    collapseFraction: Float,
    containerHeight: Int,
    blockHeight: Int,
    bottomInset: Int,
    barCenter: Int,
): Int {
    val anchorY = lerp((containerHeight - bottomInset).toFloat(), barCenter.toFloat(), collapseFraction)
    val anchorFraction = lerp(1f, 0.5f, (collapseFraction * 2).coerceAtMost(1f))
    return (anchorY - blockHeight * anchorFraction).roundToInt()
}

private fun Modifier.partialHeight(fraction: Float): Modifier =
    graphicsLayer {
        alpha = fraction
        clip = true
    }.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, (placeable.height * fraction).roundToInt()) { placeable.place(0, 0) }
    }
