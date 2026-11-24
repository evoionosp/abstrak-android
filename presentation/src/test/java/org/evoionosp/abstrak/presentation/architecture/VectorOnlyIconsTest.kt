package org.evoionosp.abstrak.presentation.architecture
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorOnlyIconsTest {
    @Test
    fun `icons and drawables are vectors, never raster images`() {
        val resources = File("src/main/res")
        check(resources.isDirectory) { "Missing $resources; run from the :presentation module directory" }
        val raster =
            resources
                .walkTopDown()
                .filter { it.isFile && it.parentFile.name.substringBefore('-') in setOf("drawable", "mipmap") }
                .filter { it.extension.lowercase() in RASTER_EXTENSIONS }
                .map { it.relativeTo(resources).path }
                .toList()
        assertTrue("Raster images in $resources: $raster", raster.isEmpty())
    }

    private companion object {
        val RASTER_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg", "gif", "bmp")
    }
}
