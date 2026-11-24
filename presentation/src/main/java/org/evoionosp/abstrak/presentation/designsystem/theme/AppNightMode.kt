package org.evoionosp.abstrak.presentation.designsystem.theme
import android.app.UiModeManager
import android.content.Context
import android.os.Build

/**
 * Sets the system's per-app night mode to match [preference], so the splash screen follows the
 * app's theme rather than only the system's.
 *
 * The Compose theme can't reach the splash screen: the system draws that starting window from
 * the activity theme before any app code runs, resolving night-qualified resources such as the
 * splash background against the configuration it gives the app. Since API 31 that configuration
 * includes a per-app night mode, which the system persists, so from the next cold start on the
 * splash is drawn in the chosen theme. Changing it is a uiMode configuration change, which
 * recreates the activity. Below API 31 there is no per-app night mode and the splash keeps
 * following the system theme.
 */
fun applyAppNightMode(
    context: Context,
    preference: ThemePreference,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(preference.toApplicationNightMode())
}

/** The [UiModeManager] per-app night mode for this preference; following the system is MODE_NIGHT_AUTO. */
fun ThemePreference.toApplicationNightMode(): Int =
    when (this) {
        ThemePreference.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        ThemePreference.DARK -> UiModeManager.MODE_NIGHT_YES
        ThemePreference.LIGHT -> UiModeManager.MODE_NIGHT_NO
    }
