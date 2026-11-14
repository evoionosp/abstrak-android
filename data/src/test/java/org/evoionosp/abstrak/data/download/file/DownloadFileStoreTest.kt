package org.evoionosp.abstrak.data.download.file
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DownloadFileStoreTest {
    @get:Rule
    val tempDir = TemporaryFolder()
    private lateinit var store: DownloadFileStore

    @Before
    fun setUp() {
        store = DownloadFileStore(tempDir.root)
    }

    @Test
    fun `extensionFor prefers the mime map and tolerates case and parameters`() {
        assertEquals("mp3", store.extensionFor("audio/mpeg", "https://example.com/file"))
        assertEquals("m4a", store.extensionFor("Audio/MP4; charset=utf-8", "https://example.com/file"))
        assertEquals("m4b", store.extensionFor("audio/x-m4b", "https://example.com/file"))
    }

    @Test
    fun `extensionFor falls back to the url path`() {
        assertEquals("ogg", store.extensionFor(null, "https://example.com/file/abc.ogg?token=x"))
        assertEquals("flac", store.extensionFor("application/octet-stream", "https://example.com/a.FLAC"))
    }

    @Test
    fun `extensionFor rejects dotted hosts and defaults to bin`() {
        assertEquals("bin", store.extensionFor(null, "https://example.com/track"))
        assertEquals("bin", store.extensionFor(null, "https://example.com/a.b/track"))
        assertEquals("bin", store.extensionFor(null, "https://example.com/file/toolongext"))
    }

    @Test
    fun `track and temp paths nest under the book dir`() {
        val bookDir = File(tempDir.root, "book-1")
        assertEquals(File(bookDir, "track_2.mp3"), store.trackFile("book-1", 2, "mp3"))
        assertEquals(File(bookDir, "track_2.part"), store.tempFile("book-1", 2))
        assertEquals(bookDir, store.bookDir("book-1"))
    }

    @Test
    fun `book ids with separators are rejected`() {
        listOf("../evil", "a/b", "a\\b", "", "..", ".").forEach { id ->
            assertTrue("expected rejection for $id", runCatching { store.bookDir(id) }.isFailure)
        }
    }

    @Test
    fun `publishTempFile replaces stale siblings`() {
        val temp = store.tempFile("book-1", 0)
        temp.parentFile?.mkdirs()
        temp.writeText("new-bytes")
        store.trackFile("book-1", 0, "mp3").writeText("stale-bytes")
        val final = store.trackFile("book-1", 0, "m4a")
        store.publishTempFile(temp, final)
        assertEquals("new-bytes", final.readText())
        assertFalse(store.trackFile("book-1", 0, "mp3").exists())
        assertFalse(temp.exists())
    }

    @Test
    fun `deleteTrackFiles removes every variant but not neighbors`() {
        store.trackFile("book-1", 1, "mp3").apply { parentFile?.mkdirs() }.writeText("a")
        store.tempFile("book-1", 1).writeText("b")
        val neighbor = store.trackFile("book-1", 10, "mp3")
        neighbor.writeText("c")
        store.deleteTrackFiles("book-1", 1)
        assertFalse(store.trackFile("book-1", 1, "mp3").exists())
        assertFalse(store.tempFile("book-1", 1).exists())
        assertEquals("c", neighbor.readText())
    }

    @Test
    fun `deleteTempFile leaves the finished file alone`() {
        val temp = store.tempFile("book-1", 0).apply { parentFile?.mkdirs() }
        temp.writeText("partial")
        val finished = store.trackFile("book-1", 0, "mp3")
        finished.writeText("audio")
        store.deleteTempFile("book-1", 0)
        assertFalse(temp.exists())
        assertEquals("audio", finished.readText())
    }

    @Test
    fun `deleteBookFiles and deleteAll clear the tree`() {
        store.trackFile("book-1", 0, "mp3").apply { parentFile?.mkdirs() }.writeText("a")
        store.trackFile("book-2", 0, "mp3").apply { parentFile?.mkdirs() }.writeText("b")
        store.deleteBookFiles("book-1")
        assertFalse(store.bookDir("book-1").exists())
        assertTrue(store.bookDir("book-2").exists())
        store.deleteAll()
        assertTrue(tempDir.root.listFiles().isNullOrEmpty())
    }
}
