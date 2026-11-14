package org.evoionosp.abstrak.data.test
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.evoionosp.abstrak.data.network.ManagedOkHttpClients

fun testManagedClients(client: OkHttpClient = OkHttpClient()): ManagedOkHttpClients {
    val clients = mockk<ManagedOkHttpClients>()
    every { clients.apiClient } returns client
    every { clients.authClient } returns client
    every { clients.registerInvalidator(any()) } returns Unit
    return clients
}
