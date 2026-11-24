package org.evoionosp.abstrak.data.download.file
import java.io.File

/**
 * Whether a completed track's file can still be played offline: a readable, non-empty regular
 * file. Playback, the start-up storage check and the downloader all use this one test, so a
 * completed track whose file fails it counts as missing everywhere: it is never played from, it
 * is pruned at the next start-up check, and it can then be downloaded again.
 */
internal fun isPlayableDownload(path: String?): Boolean {
    val file = path?.let(::File) ?: return false
    return file.isFile && file.canRead() && file.length() > 0
}
