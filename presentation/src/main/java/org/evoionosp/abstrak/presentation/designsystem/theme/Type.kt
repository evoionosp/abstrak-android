package org.evoionosp.abstrak.presentation.designsystem.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import org.evoionosp.abstrak.presentation.R

private val provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )
private val bodyFontFamily =
    FontFamily(
        Font(
            googleFont = GoogleFont("Montserrat"),
            fontProvider = provider,
        ),
        Font(
            googleFont = GoogleFont("Montserrat"),
            fontProvider = provider,
            weight = FontWeight.Light,
        ),
    )
private val displayFontFamily =
    FontFamily(
        Font(
            googleFont = GoogleFont("Lexend"),
            fontProvider = provider,
        ),
    )
private val baseline = Typography()
val AbstrakTypography =
    Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
    )
const val COLLAPSED_TITLE_SCALE = 0.8f

fun Typography.collapsingTitle(scale: Float = COLLAPSED_TITLE_SCALE): TextStyle =
    headlineMedium.copy(
        fontSize = headlineMedium.fontSize * scale,
        lineHeight = headlineMedium.fontSize * scale * 1.1f,
        fontWeight = FontWeight.Bold,
    )
