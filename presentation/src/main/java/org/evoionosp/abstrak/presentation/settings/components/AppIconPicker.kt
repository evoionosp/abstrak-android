package org.evoionosp.abstrak.presentation.settings.components
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppIconPalette
import org.evoionosp.abstrak.domain.settings.AppIconVariant
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.button.SegmentedChoiceRow
import org.evoionosp.abstrak.presentation.designsystem.component.list.AbstrakListItemDefaults

/**
 * Picks the launcher icon: Light or Dark in the pill, a palette from the previews below it.
 * Switching between Light and Dark keeps the palette and animates every preview at once, its
 * background and the A's facets moving from one variant's colors to the other's.
 */
@Composable
internal fun AppIconPicker(
    selected: AppIcon,
    onVariantSelect: (AppIconVariant) -> Unit,
    onPaletteSelect: (AppIconPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkness =
        animateFloatAsState(
            targetValue = if (selected.variant == AppIconVariant.DARK) 1f else 0f,
            animationSpec = tween(AppIconPickerDefaults.VARIANT_SWITCH_MILLIS, easing = FastOutSlowInEasing),
            label = "appIconDarkness",
        )
    val variantLabels = AppIconVariant.entries.associateWith { stringResource(it.labelRes) }
    Column(
        modifier = modifier.fillMaxWidth().padding(AbstrakListItemDefaults.ContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SegmentedChoiceRow(
            options = AppIconVariant.entries,
            isSelected = { it == selected.variant },
            onSelect = onVariantSelect,
            label = variantLabels::getValue,
            icon = { it.icon },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AppIconPickerDefaults.OptionSpacing),
        ) {
            AppIconPalette.entries.forEach { palette ->
                AppIconOption(
                    palette = palette,
                    darkness = darkness,
                    selected = palette == selected.palette,
                    onClick = { onPaletteSelect(palette) },
                )
            }
        }
        Text(
            text = stringResource(R.string.app_icon_applies_in_background),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** One palette's preview with its name; the selected one is ringed, with a gap, like a focus ring. */
@Composable
private fun AppIconOption(
    palette: AppIconPalette,
    darkness: State<Float>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring = animateFloatAsState(targetValue = if (selected) 1f else 0f, label = "appIconRing")
    val ringColor = MaterialTheme.colorScheme.secondary
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(AppIconPickerDefaults.OptionPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(
                        AppIconPickerDefaults.IconSize +
                            (AppIconPickerDefaults.RingGap + AppIconPickerDefaults.RingWidth) * 2,
                    ).drawBehind { drawSelectionRing(progress = ring.value, color = ringColor) },
            contentAlignment = Alignment.Center,
        ) {
            AppIconPreview(
                palette = palette,
                darkness = darkness,
                modifier = Modifier.size(AppIconPickerDefaults.IconSize),
            )
        }
        Text(
            text = stringResource(palette.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * [palette]'s icon as a circular launcher draws it. The light and dark foregrounds share every
 * shape, so drawing the dark one over the light one at [darkness] blends each facet's color, while
 * the background blends the same way: one smooth change instead of a swap.
 */
@Composable
private fun AppIconPreview(
    palette: AppIconPalette,
    darkness: State<Float>,
    modifier: Modifier = Modifier,
) {
    val light = AppIcon.of(palette, AppIconVariant.LIGHT)
    val dark = AppIcon.of(palette, AppIconVariant.DARK)
    val lightBackground = colorResource(light.backgroundRes)
    val darkBackground = colorResource(dark.backgroundRes)
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .drawBehind { drawRect(lerp(lightBackground, darkBackground, darkness.value)) },
    ) {
        Image(
            painter = painterResource(light.foregroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleToLauncherMask() },
        )
        Image(
            painter = painterResource(dark.foregroundRes),
            contentDescription = null,
            modifier =
                Modifier.fillMaxSize().graphicsLayer {
                    scaleToLauncherMask()
                    alpha = darkness.value
                },
        )
    }
}

private fun GraphicsLayerScope.scaleToLauncherMask() {
    scaleX = AppIconPickerDefaults.ADAPTIVE_ICON_SCALE
    scaleY = AppIconPickerDefaults.ADAPTIVE_ICON_SCALE
}

/** The ring grows out from the icon's edge as it fades in, and shrinks back as it fades out. */
private fun DrawScope.drawSelectionRing(
    progress: Float,
    color: Color,
) {
    if (progress <= 0f) return
    val stroke = AppIconPickerDefaults.RingWidth.toPx()
    val scale = RING_START_SCALE + (1f - RING_START_SCALE) * progress
    drawCircle(
        color = color.copy(alpha = color.alpha * progress),
        radius = (size.minDimension - stroke) / 2f * scale,
        style = Stroke(width = stroke),
    )
}

private const val RING_START_SCALE = 0.9f

internal object AppIconPickerDefaults {
    val IconSize = 64.dp
    val RingWidth = 3.dp
    val RingGap = 3.dp
    val OptionSpacing = 24.dp
    val OptionPadding = 8.dp
    const val VARIANT_SWITCH_MILLIS = 500

    /** A launcher shows the middle 72dp of an adaptive icon's 108dp layers. */
    const val ADAPTIVE_ICON_SCALE = 108f / 72f
}

@get:DrawableRes
private val AppIcon.foregroundRes: Int
    get() =
        when (this) {
            AppIcon.ISLAND_LIGHT -> R.drawable.app_icon_island_light_foreground
            AppIcon.ISLAND_DARK -> R.drawable.app_icon_island_dark_foreground
            AppIcon.FOREST_LIGHT -> R.drawable.app_icon_forest_light_foreground
            AppIcon.FOREST_DARK -> R.drawable.app_icon_forest_dark_foreground
        }

@get:ColorRes
private val AppIcon.backgroundRes: Int
    get() =
        when (this) {
            AppIcon.ISLAND_LIGHT -> R.color.app_icon_island_light_background
            AppIcon.ISLAND_DARK -> R.color.app_icon_island_dark_background
            AppIcon.FOREST_LIGHT -> R.color.app_icon_forest_light_background
            AppIcon.FOREST_DARK -> R.color.app_icon_forest_dark_background
        }

@get:StringRes
private val AppIconPalette.labelRes: Int
    get() =
        when (this) {
            AppIconPalette.FOREST -> R.string.app_icon_forest
            AppIconPalette.ISLAND -> R.string.app_icon_island
        }

@get:StringRes
private val AppIconVariant.labelRes: Int
    get() =
        when (this) {
            AppIconVariant.LIGHT -> R.string.app_icon_light
            AppIconVariant.DARK -> R.string.app_icon_dark
        }

private val AppIconVariant.icon: ImageVector
    get() =
        when (this) {
            AppIconVariant.LIGHT -> Icons.Rounded.LightMode
            AppIconVariant.DARK -> Icons.Rounded.DarkMode
        }
