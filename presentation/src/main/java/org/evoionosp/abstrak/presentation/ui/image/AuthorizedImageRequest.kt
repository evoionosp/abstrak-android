package org.evoionosp.abstrak.presentation.ui.image
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest

val LocalAccessToken =
    compositionLocalOf<String> {
        error("No access token provided")
    }

internal fun authorizedImageRequest(
    context: Context,
    accessToken: String,
    data: Any,
): ImageRequest =
    ImageRequest
        .Builder(context)
        .data(data)
        .crossfade(true)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .addHeader("Authorization", "Bearer $accessToken")
        .build()

@Composable
internal fun rememberAuthorizedImageRequest(data: Any): ImageRequest {
    val context = LocalContext.current
    val accessToken = LocalAccessToken.current
    return remember(data, accessToken) {
        authorizedImageRequest(context, accessToken, data)
    }
}
