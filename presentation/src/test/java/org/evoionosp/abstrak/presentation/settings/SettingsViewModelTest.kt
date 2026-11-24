package org.evoionosp.abstrak.presentation.settings
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppIconPalette
import org.evoionosp.abstrak.domain.settings.AppIconVariant
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
            viewModel.onAppearanceAction(AppearanceAction.MoonlessNightChange(true))
            advanceUntilIdle()
            assertEquals(true, store.state.value.useMoonlessNight)
        }

    @Test
    fun `the stored app icon reaches the ui state`() =
        runTest(dispatcher) {
            val viewModel = SettingsViewModel(FakeAppSettingsStore(AppSettings(appIcon = AppIcon.FOREST_DARK)))
            viewModel.uiState.test {
                assertEquals(AppIcon.DEFAULT, awaitItem().appIcon)
                assertEquals(AppIcon.FOREST_DARK, awaitItem().appIcon)
            }
        }

    @Test
    fun `switching the app icon to dark keeps its palette`() =
        runTest(dispatcher) {
            val store = FakeAppSettingsStore(AppSettings(appIcon = AppIcon.FOREST_LIGHT))
            SettingsViewModel(store).onAppearanceAction(AppearanceAction.AppIconVariantChange(AppIconVariant.DARK))
            advanceUntilIdle()
            assertEquals(AppIcon.FOREST_DARK, store.state.value.appIcon)
        }

    @Test
    fun `picking an app icon palette keeps light or dark`() =
        runTest(dispatcher) {
            val store = FakeAppSettingsStore(AppSettings(appIcon = AppIcon.ISLAND_DARK))
            SettingsViewModel(store).onAppearanceAction(AppearanceAction.AppIconPaletteChange(AppIconPalette.FOREST))
            advanceUntilIdle()
            assertEquals(AppIcon.FOREST_DARK, store.state.value.appIcon)
        }

    @Test
    fun `a variant and a palette picked together both stick`() =
        runTest(dispatcher) {
            val store = FakeAppSettingsStore()
            val viewModel = SettingsViewModel(store)
            viewModel.onAppearanceAction(AppearanceAction.AppIconVariantChange(AppIconVariant.DARK))
            viewModel.onAppearanceAction(AppearanceAction.AppIconPaletteChange(AppIconPalette.FOREST))
            advanceUntilIdle()
            assertEquals(AppIcon.FOREST_DARK, store.state.value.appIcon)
        }
}
