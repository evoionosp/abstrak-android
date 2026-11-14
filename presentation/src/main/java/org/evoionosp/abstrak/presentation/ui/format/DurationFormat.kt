package org.evoionosp.abstrak.presentation.ui.format

internal fun Long.toDurationLabel(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%d:%02d".format(minutes, seconds)
    }
}

internal fun Long.msToDurationLabel(): String = (this / 1000).toDurationLabel()
