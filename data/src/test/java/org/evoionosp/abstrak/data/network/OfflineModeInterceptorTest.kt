package org.evoionosp.abstrak.data.network
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.evoionosp.abstrak.data.test.MockWebServerRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OfflineModeInterceptorTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private var isOffline = false
    private val client = OkHttpClient.Builder().addInterceptor(OfflineModeInterceptor { isOffline }).build()

    @Test
    fun `requests go out while offline mode is off`() {
        serverRule.enqueuePlainText(200, "ok")
        client.newCall(request()).execute().use { assertEquals(200, it.code) }
        assertEquals(1, serverRule.server.requestCount)
    }

    @Test
    fun `offline mode fails the request as no network without reaching the server`() {
        isOffline = true
        val error = assertThrows(OfflineModeException::class.java) { client.newCall(request()).execute() }
        assertTrue(error is IOException)
        assertEquals(0, serverRule.server.requestCount)
    }

    private fun request(): Request = Request.Builder().url(serverRule.server.url("/api/libraries")).build()
}
