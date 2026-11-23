package org.evoionosp.abstrak.data.network
import android.content.Context
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.evoionosp.abstrak.data.connectivity.OfflineModeState
import org.evoionosp.abstrak.data.test.FakeConnectionSettingsStore
import org.evoionosp.abstrak.domain.connection.model.ConnectionSettings
import org.evoionosp.abstrak.domain.connection.model.ServerRequestHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedOkHttpClientsTest {
    private val store = FakeConnectionSettingsStore()
    private var isOffline = false
    private val offlineMode = mockk<OfflineModeState> { every { awaitIsEnabled() } answers { isOffline } }

    private fun newHolder(): ManagedOkHttpClients =
        ManagedOkHttpClients(
            context = mockk<Context>(),
            connectionSettingsStore = store,
            tokenAuthenticator = Lazy { mockk<TokenAuthenticator>() },
            offlineModeState = offlineMode,
            ioDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun `clients carry the headers interceptor`() {
        val holder = newHolder()
        assertTrue(holder.apiClient.interceptors.any { it is ConnectionHeadersInterceptor })
        assertTrue(holder.authClient.interceptors.any { it is ConnectionHeadersInterceptor })
    }

    @Test
    fun `clients fail every request while offline mode is on, before it leaves the device`() {
        val holder = newHolder()
        assertTrue(holder.apiClient.interceptors.first() is OfflineModeInterceptor)
        assertTrue(holder.authClient.interceptors.first() is OfflineModeInterceptor)
        isOffline = true
        val request = Request.Builder().url("http://127.0.0.1:9/api/libraries").build()
        assertThrows(OfflineModeException::class.java) { holder.apiClient.newCall(request).execute() }
        assertThrows(OfflineModeException::class.java) { holder.authClient.newCall(request).execute() }
    }

    @Test
    fun `security change rebuilds both clients`() {
        val holder = newHolder()
        val firstApi = holder.apiClient
        val firstAuth = holder.authClient
        emit(storeCurrent().copy(bypassSsl = true))
        assertNotSame(firstApi, holder.apiClient)
        assertNotSame(firstAuth, holder.authClient)
    }

    @Test
    fun `headers only change keeps existing clients`() {
        val holder = newHolder()
        val firstApi = holder.apiClient
        emit(storeCurrent().copy(customHeaders = listOf(ServerRequestHeader("X-K", "v"))))
        assertSame(firstApi, holder.apiClient)
    }

    @Test
    fun `rebuild runs registered invalidators once per security change`() {
        val holder = newHolder()
        var invalidations = 0
        holder.registerInvalidator { invalidations++ }
        emit(storeCurrent().copy(bypassSsl = true))
        emit(storeCurrent().copy(userAgent = "Other/1.0"))
        assertEquals(1, invalidations)
    }

    private fun storeCurrent(): ConnectionSettings = runBlocking { store.settings.first() }

    private fun emit(settings: ConnectionSettings) {
        store.emit(settings)
    }
}
