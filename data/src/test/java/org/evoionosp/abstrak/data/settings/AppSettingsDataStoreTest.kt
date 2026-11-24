package org.evoionosp.abstrak.data.settings
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppSettingsDataStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = AppSettingsDataStore(context)

    @After
    fun tearDown() =
        runTest {
            store.setThemePreference(AppSettings.DEFAULT_THEME_PREFERENCE)
            store.setDynamicColor(true)
            store.setMoonlessNight(false)
            store.setSleepTimerMinutes(AppSettings.DEFAULT_SLEEP_TIMER_MINUTES)
            store.setSearchAllLibraries(true)
            store.setDownloadWifiOnly(false)
            store.setAutoDeleteFinishedDownloads(false)
            store.setOfflineMode(false)
            store.setAppIcon(AppIcon.DEFAULT)
        }

    @Test
    fun `defaults to system theme with dynamic color`() =
        runTest {
            store.settings.test {
                assertEquals(AppSettings(), awaitItem())
            }
        }

    @Test
    fun `theme preference updates are emitted`() =
        runTest {
            store.settings.test {
                awaitItem()
                store.setThemePreference("DARK")
                assertEquals("DARK", awaitItem().themePreference)
            }
        }

    @Test
    fun `dynamic color updates are emitted`() =
        runTest {
            store.settings.test {
                awaitItem()
                store.setDynamicColor(false)
                assertEquals(false, awaitItem().useDynamicColor)
            }
        }

    @Test
    fun `moonless night defaults off and the choice round-trips`() =
        runTest {
            store.settings.test {
                assertEquals(false, awaitItem().useMoonlessNight)
                store.setMoonlessNight(true)
                assertEquals(true, awaitItem().useMoonlessNight)
                store.setMoonlessNight(false)
                assertEquals(false, awaitItem().useMoonlessNight)
            }
        }

    @Test
    fun `sleep timer minutes round-trip and clamp to the slider range`() =
        runTest {
            store.settings.test {
                assertEquals(AppSettings.DEFAULT_SLEEP_TIMER_MINUTES, awaitItem().sleepTimerMinutes)
                store.setSleepTimerMinutes(45)
                assertEquals(45, awaitItem().sleepTimerMinutes)
                store.setSleepTimerMinutes(180)
                assertEquals(AppSettings.MAX_SLEEP_TIMER_MINUTES, awaitItem().sleepTimerMinutes)
                store.setSleepTimerMinutes(0)
                assertEquals(AppSettings.MIN_SLEEP_TIMER_MINUTES, awaitItem().sleepTimerMinutes)
            }
        }

    @Test
    fun `download toggles default off and the choices round-trip`() =
        runTest {
            store.settings.test {
                val defaults = awaitItem()
                assertEquals(false, defaults.downloadWifiOnly)
                assertEquals(false, defaults.autoDeleteFinishedDownloads)
                store.setDownloadWifiOnly(true)
                assertEquals(true, awaitItem().downloadWifiOnly)
                store.setAutoDeleteFinishedDownloads(true)
                assertEquals(true, awaitItem().autoDeleteFinishedDownloads)
                store.setDownloadWifiOnly(false)
                assertEquals(false, awaitItem().downloadWifiOnly)
                store.setAutoDeleteFinishedDownloads(false)
                assertEquals(false, awaitItem().autoDeleteFinishedDownloads)
            }
        }

    @Test
    fun `offline mode defaults off and the choice round-trips`() =
        runTest {
            store.settings.test {
                assertEquals(false, awaitItem().useOfflineMode)
                store.setOfflineMode(true)
                assertEquals(true, awaitItem().useOfflineMode)
                store.setOfflineMode(false)
                assertEquals(false, awaitItem().useOfflineMode)
            }
        }

    @Test
    fun `search covers all libraries by default and the choice round-trips`() =
        runTest {
            store.settings.test {
                assertEquals(true, awaitItem().searchAllLibraries)
                store.setSearchAllLibraries(false)
                assertEquals(false, awaitItem().searchAllLibraries)
                store.setSearchAllLibraries(true)
                assertEquals(true, awaitItem().searchAllLibraries)
            }
        }

    @Test
    fun `the app icon defaults to island light and the choice round-trips`() =
        runTest {
            store.settings.test {
                assertEquals(AppIcon.ISLAND_LIGHT, awaitItem().appIcon)
                store.setAppIcon(AppIcon.FOREST_DARK)
                assertEquals(AppIcon.FOREST_DARK, awaitItem().appIcon)
                store.setAppIcon(AppIcon.ISLAND_DARK)
                assertEquals(AppIcon.ISLAND_DARK, awaitItem().appIcon)
            }
        }
}
