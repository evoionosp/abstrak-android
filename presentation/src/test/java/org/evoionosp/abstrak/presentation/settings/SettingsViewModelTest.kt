package org.evoionosp.abstrak.presentation.settings
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the stored moonless night choice reaches the ui state`() =
        runTest(dispatcher) {
            val viewModel = SettingsViewModel(FakeAppSettingsStore(AppSettings(useMoonlessNight = true)))
            viewModel.uiState.test {
                assertEquals(false, awaitItem().useMoonlessNight)
                assertEquals(true, awaitItem().useMoonlessNight)
            }
        }

    @Test
    fun `turning moonless night on saves it`() =
        runTest(dispatcher) {
            val store = FakeAppSettingsStore()
            val viewModel = SettingsViewModel(store)
            viewModel.onMoonlessNightChange(true)
            advanceUntilIdle()
            assertEquals(true, store.state.value.useMoonlessNight)
        }
}
