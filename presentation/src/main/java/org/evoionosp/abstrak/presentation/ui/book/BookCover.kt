package org.evoionosp.abstrak.presentation.ui.book
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import org.evoionosp.abstrak.presentation.ui.image.CoverArt
import org.evoionosp.abstrak.presentation.ui.image.rememberAuthorizedImageRequest

@Composable
fun BookCover(
    coverUrl: String,
    title: String,
    author: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    colorFilter: ColorFilter? = null,
) {
    AsyncImage(
        model = rememberAuthorizedImageRequest(CoverArt(url = coverUrl, title = title, author = author)),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
    )
}
