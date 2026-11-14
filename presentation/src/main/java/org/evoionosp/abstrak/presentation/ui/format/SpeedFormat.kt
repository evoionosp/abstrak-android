package org.evoionosp.abstrak.presentation.ui.format
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun formatPlaybackSpeed(
    speed: Float,
    locale: Locale = Locale.getDefault(),
): String = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale)).format(speed) + "x"
