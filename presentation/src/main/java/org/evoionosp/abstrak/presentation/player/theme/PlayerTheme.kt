package org.evoionosp.abstrak.presentation.player.theme
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.presentation.ui.image.CoverArt
import org.evoionosp.abstrak.presentation.ui.image.rememberAuthorizedImageRequest

private const val PLAYER_TINT_BLEND = 0.38f

@Composable
internal fun rememberPlayerColorScheme(
    audiobook: Audiobook,
    themeFromCover: Boolean,
): ColorScheme {
    val appScheme = MaterialTheme.colorScheme
    val artPalette =
        rememberArtPalette(
            rememberAuthorizedImageRequest(
                CoverArt(url = audiobook.coverUrl, title = audiobook.title, author = audiobook.author),
            ),
        )
    val cardColor by animateColorAsState(
        targetValue =
            playerCardColor(
                surface = appScheme.surface,
                dominant = artPalette?.dominant,
                themeFromCover = themeFromCover,
                blend = PLAYER_TINT_BLEND,
            ),
        label = "PlayerCardTint",
    )
    val roles =
        rememberPlayerColors(
            seed = if (themeFromCover) artPalette?.vibrant else null,
            background = cardColor,
            darkTheme = appScheme.surface.luminance() < 0.5f,
        )
    return appScheme.withPlayerRoles(roles, cardColor)
}

@Composable
internal fun PlayerTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
