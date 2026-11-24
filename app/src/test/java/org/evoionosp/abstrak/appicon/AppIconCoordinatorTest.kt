package org.evoionosp.abstrak.appicon
import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.evoionosp.abstrak.presentation.launcher.LauncherIconSwitcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppIconCoordinatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val switcher = LauncherIconSwitcher(context)
    private val process =
        object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }

    @Test
    fun `nothing changes on the launcher while the app is in front`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = start()
            comeToFront()
            store.setAppIcon(AppIcon.FOREST_DARK)
            runCurrent()
            assertEquals(AppIcon.DEFAULT, switcher.current())
        }

    @Test
    fun `the chosen icon reaches the launcher when the app goes to the background`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = start()
            comeToFront()
            store.setAppIcon(AppIcon.ISLAND_LIGHT)
            store.setAppIcon(AppIcon.FOREST_DARK)
            goToBackground()
            assertEquals(AppIcon.FOREST_DARK, switcher.current())
        }

    @Test
    fun `a choice saved once the app is in the background is applied too`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = start()
            comeToFront()
            goToBackground()
            store.setAppIcon(AppIcon.ISLAND_DARK)
            runCurrent()
            assertEquals(AppIcon.ISLAND_DARK, switcher.current())
        }

    @Test
    fun `back in front, a new choice waits until the app goes to the background again`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = start()
            comeToFront()
            store.setAppIcon(AppIcon.FOREST_LIGHT)
            goToBackground()
            comeToFront()
            store.setAppIcon(AppIcon.FOREST_DARK)
            runCurrent()
            assertEquals(AppIcon.FOREST_LIGHT, switcher.current())
            goToBackground()
            assertEquals(AppIcon.FOREST_DARK, switcher.current())
        }

    @Test
    fun `a process that never comes to the front leaves the launcher alone`() =
        runTest(UnconfinedTestDispatcher()) {
            start(AppSettings(appIcon = AppIcon.FOREST_DARK))
            process.lifecycle.currentState = Lifecycle.State.CREATED
            runCurrent()
            assertEquals(AppIcon.DEFAULT, switcher.current())
        }

    private fun TestScope.start(settings: AppSettings = AppSettings()): FakeAppSettingsStore {
        val store = FakeAppSettingsStore(settings)
        AppIconCoordinator(backgroundScope, store, switcher).start(process.lifecycle)
        return store
    }

    private fun TestScope.comeToFront() {
        process.lifecycle.currentState = Lifecycle.State.RESUMED
        runCurrent()
    }

    private fun TestScope.goToBackground() {
        process.lifecycle.currentState = Lifecycle.State.CREATED
        runCurrent()
    }
}
