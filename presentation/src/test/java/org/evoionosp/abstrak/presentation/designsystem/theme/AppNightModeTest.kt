package org.evoionosp.abstrak.presentation.designsystem.theme
import android.app.UiModeManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppNightModeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val uiModeManager: UiModeManager = context.getSystemService(UiModeManager::class.java)

    @Test
    fun `each preference sets the per-app night mode the splash screen is drawn with`() {
        applyAppNightMode(context, ThemePreference.DARK)
        assertEquals(UiModeManager.MODE_NIGHT_YES, shadowOf(uiModeManager).applicationNightMode)
        applyAppNightMode(context, ThemePreference.LIGHT)
        assertEquals(UiModeManager.MODE_NIGHT_NO, shadowOf(uiModeManager).applicationNightMode)
        applyAppNightMode(context, ThemePreference.SYSTEM)
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, shadowOf(uiModeManager).applicationNightMode)
    }

    @Test
    fun `system preference follows the system night mode`() {
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, ThemePreference.SYSTEM.toApplicationNightMode())
    }

    @Test
    fun `dark preference forces night mode`() {
        assertEquals(UiModeManager.MODE_NIGHT_YES, ThemePreference.DARK.toApplicationNightMode())
    }

    @Test
    fun `light preference forces day mode`() {
        assertEquals(UiModeManager.MODE_NIGHT_NO, ThemePreference.LIGHT.toApplicationNightMode())
    }

    @Test
    fun `stored preference names round-trip`() {
        ThemePreference.entries.forEach { preference ->
            assertEquals(preference, ThemePreference.fromName(preference.name))
        }
    }

    @Test
    fun `unknown stored preference names fall back to system`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromName("BOGUS"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromName(""))
    }
}
