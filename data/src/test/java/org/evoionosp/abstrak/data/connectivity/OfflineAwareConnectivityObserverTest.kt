package org.evoionosp.abstrak.data.connectivity
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.data.test.FakeConnectivityObserver
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAwareConnectivityObserverTest {
    private val storedSettings = MutableStateFlow(AppSettings())
    private val deviceNetwork = FakeConnectivityObserver(connected = true)
    private val offlineMode =
        OfflineModeState(
            appSettingsStore = mockk<AppSettingsStore> { every { settings } returns storedSettings },
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    private val observer = OfflineAwareConnectivityObserver(deviceNetwork, offlineMode)

    @Test
    fun `follows the device network while offline mode is off`() {
        assertTrue(observer.isConnected())
        deviceNetwork.setConnected(false)
        assertFalse(observer.isConnected())
    }

    @Test
    fun `offline mode reads as no network even with one`() {
        setOfflineMode(true)
        assertTrue(deviceNetwork.isConnected())
        assertFalse(observer.isConnected())
        setOfflineMode(false)
        assertTrue(observer.isConnected())
    }

    @Test
    fun `observe goes offline with offline mode and back online without it`() =
        runTest {
            observer.observe().test {
                assertEquals(true, awaitItem())
                setOfflineMode(true)
                assertEquals(false, awaitItem())
                deviceNetwork.setConnected(false)
                deviceNetwork.setConnected(true)
                expectNoEvents()
                setOfflineMode(false)
                assertEquals(true, awaitItem())
            }
        }

    @Test
    fun `offline mode state reads the stored setting`() =
        runTest {
            assertFalse(offlineMode.awaitIsEnabled())
            setOfflineMode(true)
            assertTrue(offlineMode.isEnabledNow())
            assertTrue(offlineMode.awaitIsEnabled())
            offlineMode.isEnabled.test {
                assertEquals(true, awaitItem())
                setOfflineMode(false)
                assertEquals(false, awaitItem())
            }
        }

    private fun setOfflineMode(enabled: Boolean) {
        storedSettings.value = storedSettings.value.copy(useOfflineMode = enabled)
    }
}
