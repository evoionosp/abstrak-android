package org.evoionosp.abstrak.data.storage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StorageMeterTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `cacheBytes sums every file under the cache, nested ones included`() {
        val cacheDir = tempDir.newFolder("cache")
        File(cacheDir, "top").writeBytes(ByteArray(10))
        File(cacheDir, "image_cache/journal").apply { parentFile?.mkdirs() }.writeBytes(ByteArray(25))
        File(cacheDir, "image_cache/covers/a").apply { parentFile?.mkdirs() }.writeBytes(ByteArray(5))
        val meter = StorageMeter(cacheDir = cacheDir, downloadRoot = File(tempDir.root, "downloads"))
        assertEquals(40L, meter.cacheBytes())
    }

    @Test
    fun `cacheBytes is zero while the cache does not exist`() {
        val meter = StorageMeter(cacheDir = File(tempDir.root, "missing"), downloadRoot = File(tempDir.root, "downloads"))
        assertEquals(0L, meter.cacheBytes())
    }

    @Test
    fun `deviceFreeBytes creates the download root and never goes negative`() {
        val downloadRoot = File(tempDir.root, "downloads")
        val meter = StorageMeter(cacheDir = tempDir.newFolder("cache"), downloadRoot = downloadRoot)
        assertTrue(meter.deviceFreeBytes() >= 0)
        assertTrue(downloadRoot.isDirectory)
    }
}
