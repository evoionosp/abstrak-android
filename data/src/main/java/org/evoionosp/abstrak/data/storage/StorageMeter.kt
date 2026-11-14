package org.evoionosp.abstrak.data.storage
import android.os.StatFs
import java.io.File

class StorageMeter(
    private val cacheDir: File,
    private val downloadRoot: File,
) {
    fun cacheBytes(): Long = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun deviceFreeBytes(): Long {
        downloadRoot.mkdirs()
        return StatFs(downloadRoot.path).availableBytes.coerceAtLeast(0L)
    }
}
