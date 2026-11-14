package org.evoionosp.abstrak.presentation.designsystem.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.ColorRoles
import com.google.android.material.color.MaterialColors

@Immutable
data class AbstrakExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

private val SuccessSeed = Color(0xFF4CAF50)
private val WarningSeed = Color(0xFFFF9800)

fun abstrakExtendedColors(
    primary: Color,
    isLight: Boolean,
): AbstrakExtendedColors {
    val success = customColorRoles(SuccessSeed, primary, isLight)
    val warning = customColorRoles(WarningSeed, primary, isLight)
    return AbstrakExtendedColors(
        success = Color(success.accent),
        onSuccess = Color(success.onAccent),
        successContainer = Color(success.accentContainer),
        onSuccessContainer = Color(success.onAccentContainer),
        warning = Color(warning.accent),
        onWarning = Color(warning.onAccent),
        warningContainer = Color(warning.accentContainer),
        onWarningContainer = Color(warning.onAccentContainer),
    )
}

private fun customColorRoles(
    seed: Color,
    primary: Color,
    isLight: Boolean,
): ColorRoles = MaterialColors.getColorRoles(MaterialColors.harmonize(seed.toArgb(), primary.toArgb()), isLight)

val LocalAbstrakExtendedColors = staticCompositionLocalOf { abstrakExtendedColors(primaryLight, isLight = true) }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.extendedColors: AbstrakExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAbstrakExtendedColors.current
