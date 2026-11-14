package org.evoionosp.abstrak.domain.settings
import kotlinx.coroutines.flow.Flow

interface AppSettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setThemePreference(themePreference: String)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setCoverTheme(enabled: Boolean)

    suspend fun setMoonlessNight(enabled: Boolean)

    suspend fun setSleepTimerMinutes(minutes: Int)

    suspend fun setSearchAllLibraries(enabled: Boolean)

    suspend fun setDownloadWifiOnly(enabled: Boolean)

    suspend fun setAutoDeleteFinishedDownloads(enabled: Boolean)
}
