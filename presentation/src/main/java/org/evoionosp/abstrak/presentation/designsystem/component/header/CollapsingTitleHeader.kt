package org.evoionosp.abstrak.presentation.designsystem.component.header
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.COLLAPSED_TITLE_SCALE
import org.evoionosp.abstrak.presentation.designsystem.theme.backgrounds
import org.evoionosp.abstrak.presentation.designsystem.theme.collapsingTitle

val CollapsingTitleBarHeight = 64.dp
val CollapsingTitleHeaderMaxHeight = 180.dp

@Composable
fun CollapsingTitleHeader(
    title: String,
    collapseFraction: Float,
    height: Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = collapseFraction.coerceIn(0f, 1f)
    val solidAlpha = (fraction * 2f).coerceIn(0f, 1f)
    val titleScale = lerp(1.2f, COLLAPSED_TITLE_SCALE, fraction)
    val titlePaddingStart = lerp(20.dp, 68.dp, fraction)
    val titleVerticalBias = lerp(1f, -1f, fraction)
    val animatedTitleAlignment =
        BiasAlignment(horizontalBias = -1f, verticalBias = titleVerticalBias)
    val titleContainerHeight = lerp(88.dp, 56.dp, fraction)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .background(MaterialTheme.backgrounds.screenTop.copy(alpha = solidAlpha)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.preferences_back),
                )
            }
            Box(
                modifier =
                    Modifier
                        .align(animatedTitleAlignment)
                        .height(titleContainerHeight)
                        .fillMaxWidth()
                        .padding(start = titlePaddingStart, end = 24.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.collapsingTitle(titleScale),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
    }
}
