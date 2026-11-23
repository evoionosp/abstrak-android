package org.evoionosp.abstrak.domain.settings

data class AppSettings(
    val themePreference: String = DEFAULT_THEME_PREFERENCE,
    val useDynamicColor: Boolean = true,
    val useCoverTheme: Boolean = true,
    val useMoonlessNight: Boolean = false,
    val sleepTimerMinutes: Int = DEFAULT_SLEEP_TIMER_MINUTES,
    val searchAllLibraries: Boolean = true,
    val downloadWifiOnly: Boolean = false,
    val autoDeleteFinishedDownloads: Boolean = false,
    /** Act as if there were no network, even when there is: nothing is sent to the server. */
    val useOfflineMode: Boolean = false,
) {
    companion object {
        const val DEFAULT_THEME_PREFERENCE = "SYSTEM"
        const val DEFAULT_SLEEP_TIMER_MINUTES = 30
        const val MIN_SLEEP_TIMER_MINUTES = 1
        const val MAX_SLEEP_TIMER_MINUTES = 100
    }
}
