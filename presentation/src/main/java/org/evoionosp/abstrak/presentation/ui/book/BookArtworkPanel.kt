package org.evoionosp.abstrak.presentation.ui.book
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakShapes

@Composable
fun BookArtworkPanel(
    coverUrl: String,
    title: String,
    author: String,
    progress: Float,
    modifier: Modifier = Modifier,
    titleSpacing: Dp = 16.dp,
    onTitleClick: (() -> Unit)? = null,
) {
    var coverWidthPx by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BookCover(
            coverUrl = coverUrl,
            title = title,
            author = author,
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .onSizeChanged { coverWidthPx = it.width }
                    .clip(AbstrakShapes.cover),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BookProgressBar(
            progress = progress,
            modifier =
                Modifier
                    .width(with(LocalDensity.current) { coverWidthPx.toDp() })
                    .height(3.dp)
                    .clip(AbstrakShapes.pill),
        )
        Spacer(modifier = Modifier.height(titleSpacing))
        BookTitleBlock(
            title = title,
            author = author,
            textAlign = TextAlign.Center,
            onClick = onTitleClick,
        )
    }
}
