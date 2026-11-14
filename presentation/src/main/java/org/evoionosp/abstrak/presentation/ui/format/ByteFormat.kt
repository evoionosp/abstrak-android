package org.evoionosp.abstrak.presentation.ui.format
import java.util.Locale

internal fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes / BYTES_PER_MEGABYTE
    if (megabytes < MEGABYTES_PER_GIGABYTE - HALF_TENTH) {
        return "%.1f MB".format(Locale.US, megabytes.coerceAtLeast(MIN_SHOWN_MEGABYTES))
    }
    return "%.1f GB".format(Locale.US, megabytes / MEGABYTES_PER_GIGABYTE)
}

private const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0
private const val MEGABYTES_PER_GIGABYTE = 1024.0
private const val HALF_TENTH = 0.05
private const val MIN_SHOWN_MEGABYTES = 0.1
