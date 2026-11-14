package org.evoionosp.abstrak.data.download.remote
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import org.evoionosp.abstrak.data.test.MockWebServerRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RemoteFileDownloaderTest {
    @get:Rule
    val server = MockWebServerRule()

    @get:Rule
    val tempDir = TemporaryFolder()
    private lateinit var downloader: RemoteFileDownloader
    private lateinit var destFile: File

    @Before
    fun setUp() {
        downloader = RemoteFileDownloader(OkHttpClient(), Dispatchers.IO)
        destFile = File(tempDir.root, "track_0.part")
    }

    @Test
    fun `writes the body and reports completion with total`() =
        runBlocking {
            val body = "audio-bytes-".repeat(200)
            server.enqueuePlainText(200, body)
            val progress = mutableListOf<Pair<Long, Long?>>()
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok-123",
                    onProgress = { bytes, total -> progress += bytes to total },
                )
            assertEquals(
                DownloadOutcome.Success(body.length.toLong(), body.length.toLong(), resumed = false),
                outcome,
            )
            assertEquals(body, destFile.readText())
            assertEquals(body.length.toLong() to body.length.toLong(), progress.last())
        }

    @Test
    fun `drops the part when another writer touched it`() =
        runBlocking {
            server.enqueuePlainText(200, "audio-bytes-".repeat(200))
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> destFile.appendText("x") },
                )
            assertEquals(DownloadOutcome.Failed(DownloadError.Network), outcome)
            assertFalse(destFile.exists())
        }

    @Test
    fun `sends the bearer token and identity encoding`() =
        runBlocking {
            server.enqueuePlainText(200, "data")
            downloader.download(
                url = server.baseUrl() + "file",
                destFile = destFile,
                accessToken = "tok-123",
                onProgress = { _, _ -> },
            )
            val request = server.takeRequest()
            assertEquals("Bearer tok-123", request.getHeader("Authorization"))
            assertEquals("identity", request.getHeader("Accept-Encoding"))
        }

    @Test
    fun `reports response validators`() =
        runBlocking {
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("data")
                    .addHeader("ETag", "\"v1\"")
                    .addHeader("Last-Modified", "Tue, 01 Jan 2030 00:00:00 GMT"),
            )
            val seen = mutableListOf<Pair<String?, String?>>()
            downloader.download(
                url = server.baseUrl() + "file",
                destFile = destFile,
                accessToken = "tok",
                onProgress = { _, _ -> },
                onHeaders = { etag, lastModified -> seen += etag to lastModified },
            )
            assertEquals(listOf("\"v1\"" to "Tue, 01 Jan 2030 00:00:00 GMT"), seen)
        }

    @Test
    fun `resumes from existing bytes on 206`() =
        runBlocking {
            destFile.writeText("HEAD-")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("ABCDE")
                    .addHeader("Content-Range", "bytes 5-9/10"),
            )
            val progress = mutableListOf<Pair<Long, Long?>>()
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { bytes, total -> progress += bytes to total },
                )
            assertEquals(DownloadOutcome.Success(10L, 10L, resumed = true), outcome)
            assertEquals("HEAD-ABCDE", destFile.readText())
            assertEquals("bytes=5-", server.takeRequest().getHeader("Range"))
        }

    @Test
    fun `resume sends If-Range from stored validators`() =
        runBlocking {
            destFile.writeText("HEAD-")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("ABCDE")
                    .addHeader("Content-Range", "bytes 5-9/10"),
            )
            downloader.download(
                url = server.baseUrl() + "file",
                destFile = destFile,
                accessToken = "tok",
                resumeValidators = ResumeValidators("\"v1\"", "Tue, 01 Jan 2030 00:00:00 GMT"),
                onProgress = { _, _ -> },
            )
            assertEquals("\"v1\"", server.takeRequest().getHeader("If-Range"))
        }

    @Test
    fun `resume falls back to Last-Modified for If-Range`() =
        runBlocking {
            destFile.writeText("HEAD-")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("ABCDE")
                    .addHeader("Content-Range", "bytes 5-9/10"),
            )
            downloader.download(
                url = server.baseUrl() + "file",
                destFile = destFile,
                accessToken = "tok",
                resumeValidators = ResumeValidators(null, "Tue, 01 Jan 2030 00:00:00 GMT"),
                onProgress = { _, _ -> },
            )
            assertEquals("Tue, 01 Jan 2030 00:00:00 GMT", server.takeRequest().getHeader("If-Range"))
        }

    @Test
    fun `resume uses Last-Modified when the ETag is weak`() =
        runBlocking {
            destFile.writeText("HEAD-")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("ABCDE")
                    .addHeader("Content-Range", "bytes 5-9/10"),
            )
            downloader.download(
                url = server.baseUrl() + "file",
                destFile = destFile,
                accessToken = "tok",
                resumeValidators = ResumeValidators("W/\"v1\"", "Tue, 01 Jan 2030 00:00:00 GMT"),
                onProgress = { _, _ -> },
            )
            assertEquals("Tue, 01 Jan 2030 00:00:00 GMT", server.takeRequest().getHeader("If-Range"))
        }

    @Test
    fun `resume omits If-Range for a lone weak ETag`() =
        runBlocking {
            destFile.writeText("HEAD-")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("ABCDE")
                    .addHeader("Content-Range", "bytes 5-9/10"),
            )
            downloader.download(
                url = server.baseUrl() + "file",
                destFile = destFile,
                accessToken = "tok",
                resumeValidators = ResumeValidators("W/\"v1\"", null),
                onProgress = { _, _ -> },
            )
            val request = server.takeRequest()
            assertEquals("bytes=5-", request.getHeader("Range"))
            assertNull(request.getHeader("If-Range"))
        }

    @Test
    fun `restarts when the server ignores range`() =
        runBlocking {
            destFile.writeText("STALE")
            val body = "FRESH-FULL"
            server.enqueuePlainText(200, body)
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                )
            assertEquals(
                DownloadOutcome.Success(body.length.toLong(), body.length.toLong(), resumed = false),
                outcome,
            )
            assertEquals(body, destFile.readText())
        }

    @Test
    fun `restarts once when the 206 starts at the wrong offset`() =
        runBlocking {
            destFile.writeText("HEAD-")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setBody("WRONG")
                    .addHeader("Content-Range", "bytes 0-4/10"),
            )
            server.enqueuePlainText(200, "FRESH-FULL")
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                )
            assertEquals(
                DownloadOutcome.Success(10L, 10L, resumed = false),
                outcome,
            )
            assertEquals("FRESH-FULL", destFile.readText())
            assertEquals(2, server.server.requestCount)
            assertEquals("bytes=5-", server.takeRequest().getHeader("Range"))
            assertNull(server.takeRequest().getHeader("Range"))
        }

    @Test
    fun `maps status errors`() =
        runBlocking {
            server.enqueuePlainText(404, "gone")
            assertEquals(
                DownloadOutcome.Failed(DownloadError.Http(404)),
                downloader.download(
                    url = server.baseUrl() + "a",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                ),
            )
            server.enqueuePlainText(401, "nope")
            assertEquals(
                DownloadOutcome.Failed(DownloadError.Auth),
                downloader.download(
                    url = server.baseUrl() + "b",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                ),
            )
            server.enqueuePlainText(403, "nope")
            assertEquals(
                DownloadOutcome.Failed(DownloadError.Forbidden),
                downloader.download(
                    url = server.baseUrl() + "c",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                ),
            )
            server.enqueuePlainText(500, "boom")
            assertEquals(
                DownloadOutcome.Failed(DownloadError.Http(500)),
                downloader.download(
                    url = server.baseUrl() + "d",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                ),
            )
        }

    @Test
    fun `treats matching 416 as already complete`() =
        runBlocking {
            destFile.writeText("DONE!")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(416)
                    .addHeader("Content-Range", "bytes */5"),
            )
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                )
            assertEquals(DownloadOutcome.Success(5L, 5L, resumed = true), outcome)
            assertEquals("DONE!", destFile.readText())
        }

    @Test
    fun `restarts once on 416 size mismatch`() =
        runBlocking {
            destFile.writeText("TOOLONGDATA")
            server.server.enqueue(
                MockResponse()
                    .setResponseCode(416)
                    .addHeader("Content-Range", "bytes */5"),
            )
            server.enqueuePlainText(200, "FRESH")
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                )
            assertEquals(DownloadOutcome.Success(5L, 5L, resumed = false), outcome)
            assertEquals("FRESH", destFile.readText())
            assertEquals(2, server.server.requestCount)
        }

    @Test
    fun `maps connection failure to Network`() =
        runBlocking {
            val outcome =
                downloader.download(
                    url = "http://127.0.0.1:9/file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                )
            assertEquals(DownloadOutcome.Failed(DownloadError.Network), outcome)
        }

    @Test
    fun `maps mid-stream disconnect to Network and keeps the part`() =
        runBlocking {
            server.server.enqueue(
                MockResponse()
                    .setBody("audio-bytes-".repeat(20_000))
                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
            )
            val outcome =
                downloader.download(
                    url = server.baseUrl() + "file",
                    destFile = destFile,
                    accessToken = "tok",
                    onProgress = { _, _ -> },
                )
            assertEquals(DownloadOutcome.Failed(DownloadError.Network), outcome)
            assertTrue(destFile.exists())
        }

    @Test
    fun `stalled read aborts promptly on cancellation`() =
        runBlocking {
            val serverSocket = ServerSocket(0)
            serverSocket.soTimeout = 15_000
            val gate = CountDownLatch(1)
            val serverJob =
                async(Dispatchers.IO) {
                    runCatching {
                        serverSocket.accept().use { socket ->
                            val reader = socket.getInputStream().bufferedReader()
                            while (true) {
                                val line = reader.readLine() ?: break
                                if (line.isEmpty()) break
                            }
                            val head = "B".repeat(16_384)
                            val header =
                                "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/octet-stream\r\n" +
                                    "Content-Length: 32768\r\n" +
                                    "Connection: close\r\n\r\n"
                            val out = socket.getOutputStream()
                            out.write(header.toByteArray())
                            out.write(head.toByteArray())
                            out.flush()
                            gate.await(30, TimeUnit.SECONDS)
                        }
                    }
                }
            try {
                val stalledDownloader =
                    RemoteFileDownloader(
                        OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build(),
                        Dispatchers.IO,
                    )
                val run =
                    async(Dispatchers.IO) {
                        stalledDownloader.download(
                            url = "http://127.0.0.1:${serverSocket.localPort}/file",
                            destFile = destFile,
                            accessToken = "tok",
                            onProgress = { _, _ -> },
                        )
                    }
                withTimeout(15_000) {
                    while (destFile.length() < 100) delay(25)
                }
                run.cancel()
                try {
                    withTimeout(15_000) { run.await() }
                    fail("expected the stalled download to throw CancellationException")
                } catch (timeout: TimeoutCancellationException) {
                    throw timeout
                } catch (cancelled: CancellationException) {
                }
                assertTrue(destFile.exists())
            } finally {
                gate.countDown()
                runCatching { serverSocket.close() }
                withTimeout(15_000) { serverJob.join() }
            }
        }
}
