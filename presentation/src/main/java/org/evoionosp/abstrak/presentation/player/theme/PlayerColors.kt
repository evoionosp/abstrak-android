@file:SuppressLint("RestrictedApi")

package org.evoionosp.abstrak.presentation.player.theme
import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeVibrant

internal data class PlayerColors(
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val containerSubtle: Color,
    val trackSubtle: Color,
)

internal fun playerColorsFallback(scheme: ColorScheme): PlayerColors =
    PlayerColors(
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        containerSubtle = scheme.surfaceContainerHigh,
        trackSubtle = scheme.surfaceVariant,
    )

private const val MIN_VIBRANT_SEED_SATURATION = 0.18f

internal fun vibrantPlayerColors(
    seedArgb: Int,
    background: Color,
    darkTheme: Boolean,
): PlayerColors {
    val seedSat =
        rgbToHsv(
            (seedArgb shr 16) and 0xFF,
            (seedArgb shr 8) and 0xFF,
            seedArgb and 0xFF,
        )[1]
    val source = Hct.fromInt(seedArgb)
    val scheme: DynamicScheme =
        if (seedSat >= MIN_VIBRANT_SEED_SATURATION) {
            SchemeVibrant(source, darkTheme, 0.0)
        } else {
            SchemeMonochrome(source, darkTheme, 0.0)
        }
    val roles = MaterialDynamicColors()
    val accent = Color(roles.primary().getArgb(scheme))
    return PlayerColors(
        accent = accent,
        onAccent = Color(roles.onPrimary().getArgb(scheme)),
        textPrimary = Color(roles.onSurface().getArgb(scheme)),
        textSecondary = Color(roles.onSurfaceVariant().getArgb(scheme)),
        containerSubtle = background.mixedWith(accent, 0.16f),
        trackSubtle = background.mixedWith(accent, 0.28f),
    )
}

internal fun playerCardColor(
    surface: Color,
    dominant: Color?,
    themeFromCover: Boolean,
    blend: Float,
): Color =
    if (themeFromCover && dominant != null) {
        surface.mixedWith(dominant, blend)
    } else {
        surface
    }

@Composable
internal fun rememberPlayerColors(
    seed: Color?,
    background: Color,
    darkTheme: Boolean,
): PlayerColors {
    val target =
        if (seed != null) {
            vibrantPlayerColors(seed.toArgb(), background, darkTheme)
        } else {
            playerColorsFallback(MaterialTheme.colorScheme)
        }
    val accent by animateColorAsState(target.accent, label = "PlayerAccent")
    val onAccent by animateColorAsState(target.onAccent, label = "PlayerOnAccent")
    val textPrimary by animateColorAsState(target.textPrimary, label = "PlayerTextPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, label = "PlayerTextSecondary")
    val containerSubtle by animateColorAsState(target.containerSubtle, label = "PlayerContainer")
    val trackSubtle by animateColorAsState(target.trackSubtle, label = "PlayerTrack")
    return PlayerColors(
        accent = accent,
        onAccent = onAccent,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        containerSubtle = containerSubtle,
        trackSubtle = trackSubtle,
    )
}

internal fun ColorScheme.withPlayerRoles(
    roles: PlayerColors,
    cardColor: Color,
): ColorScheme =
    copy(
        primary = roles.accent,
        onPrimary = roles.onAccent,
        onSurface = roles.textPrimary,
        onSurfaceVariant = roles.textSecondary,
        surfaceContainerHigh = roles.containerSubtle,
        surfaceVariant = roles.trackSubtle,
        surface = cardColor,
    )
