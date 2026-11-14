package org.evoionosp.abstrak.presentation.ui.image
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withTranslation
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult

internal data class CoverArt(
    val url: String,
    val title: String,
    val author: String,
)

private val COVER_PALETTE =
    listOf(
        Color(0xFF722F37),
        Color(0xFF1C1C1E),
        Color(0xFF2A2763),
        Color(0xFF1F4D2E),
        Color(0xFF1B2A4A),
        Color(0xFF3E2C22),
        Color(0xFF3D2B56),
        Color(0xFF14424C),
        Color(0xFF5C1A1B),
        Color(0xFF4A3B1F),
        Color(0xFF333A1F),
        Color(0xFF2B3440),
        Color(0xFF0F2A43),
        Color(0xFF2E1F16),
        Color(0xFF14181F),
        Color(0xFF5E1224),
        Color(0xFF472B45),
        Color(0xFF5A2E18),
    )
private val COVER_TEXT = Color(0xFFF5ECD7)
private val GILT = Color(0xFFC9A227)
private const val GENERATED_WIDTH = 600
private const val GENERATED_HEIGHT = 600

internal fun bookCoverColor(
    title: String,
    author: String,
): Color {
    val seed = "$title\u001F$author"
    return COVER_PALETTE[(seed.hashCode() and Int.MAX_VALUE) % COVER_PALETTE.size]
}

internal fun generateCoverBitmap(
    title: String,
    author: String,
    width: Int = GENERATED_WIDTH,
    height: Int = GENERATED_HEIGHT,
): Bitmap {
    try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bookCoverColor(title, author).toArgb())
        val spineWidth = width * 0.09f
        val spinePaint =
            Paint().apply {
                color = AndroidColor.BLACK
                alpha = 77
            }
        canvas.drawRect(0f, 0f, spineWidth, height.toFloat(), spinePaint)
        val giltPaint =
            Paint().apply {
                color = GILT.toArgb()
                alpha = 128
            }
        canvas.drawRect(spineWidth + 8f, 0f, spineWidth + 11f, height.toFloat(), giltPaint)
        val textLeft = spineWidth + width * 0.05f
        val textWidth = (width - textLeft - width * 0.05f).toInt().coerceAtLeast(1)
        if (title.isNotEmpty()) {
            val titlePaint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textSize = width * 0.105f
                    color = COVER_TEXT.toArgb()
                    letterSpacing = 0.04f
                }
            val titleLayout =
                StaticLayout.Builder
                    .obtain(title, 0, title.length, titlePaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(6)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
            canvas.save()
            canvas.translate(textLeft, height * 0.09f)
            titleLayout.draw(canvas)
            canvas.restore()
        }
        if (author.isNotEmpty()) {
            val authorPaint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    textSize = width * 0.068f
                    color = COVER_TEXT.toArgb()
                    alpha = 217
                    letterSpacing = 0.12f
                }
            val authorLayout =
                StaticLayout.Builder
                    .obtain(author, 0, author.length, authorPaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
            canvas.withTranslation(textLeft, height * 0.92f - authorLayout.height) {
                authorLayout.draw(this)
            }
        }
        return bitmap
    } catch (_: Exception) {
        return createBitmap(1, 1).apply {
            eraseColor(AndroidColor.BLACK)
        }
    }
}

internal class CoverArtInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val model = chain.request.data
        if (model !is CoverArt) {
            return chain.proceed(chain.request)
        }
        if (model.url.isBlank()) {
            return generatedResult(chain, model)
        }
        val networkRequest =
            chain.request
                .newBuilder()
                .data(model.url)
                .build()
        val result =
            try {
                chain.proceed(networkRequest)
            } catch (_: Exception) {
                return generatedResult(chain, model)
            }
        if (result is ErrorResult) {
            return generatedResult(chain, model)
        }
        return result
    }

    private fun generatedResult(
        chain: Interceptor.Chain,
        model: CoverArt,
    ): ImageResult {
        val bitmap = generateCoverBitmap(model.title, model.author)
        val drawable = bitmap.toDrawable(chain.request.context.resources)
        return SuccessResult(drawable, chain.request, DataSource.MEMORY)
    }
}

fun newCoverArtInterceptor(): Interceptor = CoverArtInterceptor()
