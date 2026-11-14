package org.evoionosp.abstrak.presentation.designsystem.theme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class AbstrakBackgrounds(
    val screenTop: Color,
    val screenBottom: Color,
)

internal fun abstrakBackgrounds(
    scheme: ColorScheme,
    darkTheme: Boolean,
): AbstrakBackgrounds =
    if (darkTheme) {
        AbstrakBackgrounds(screenTop = scheme.surface, screenBottom = scheme.surface)
    } else {
        AbstrakBackgrounds(screenTop = scheme.surface, screenBottom = scheme.surfaceContainerLowest)
    }

internal fun ColorScheme.moonlessNight(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceContainerLow,
        surfaceContainerHigh = surfaceContainer,
        surfaceContainerHighest = surfaceContainerHigh,
        surfaceBright = surfaceContainerHighest,
    )

val LocalAbstrakBackgrounds =
    staticCompositionLocalOf { abstrakBackgrounds(lightColorScheme(), darkTheme = false) }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.backgrounds: AbstrakBackgrounds
    @Composable
    @ReadOnlyComposable
    get() = LocalAbstrakBackgrounds.current

@Composable
fun screenBackgroundBrush(): Brush =
    with(MaterialTheme.backgrounds) { Brush.verticalGradient(colors = listOf(screenTop, screenBottom)) }
