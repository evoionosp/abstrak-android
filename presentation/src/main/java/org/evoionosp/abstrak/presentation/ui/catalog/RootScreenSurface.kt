package org.evoionosp.abstrak.presentation.ui.catalog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.evoionosp.abstrak.presentation.designsystem.theme.screenBackgroundBrush

@Composable
fun RootScreenSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .background(screenBackgroundBrush()),
            content = content,
        )
    }
}
