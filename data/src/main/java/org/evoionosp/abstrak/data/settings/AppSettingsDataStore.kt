package org.evoionosp.abstrak.data.settings
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsDataStore(
    private val context: Context,
) : AppSettingsStore {
    private object Keys {
        val themePreference = stringPreferencesKey("theme_preference")
        val useDynamicColor = booleanPreferencesKey("use_dynamic_color")
        val useCoverTheme = booleanPreferencesKey("use_cover_theme")
        val useMoonlessNight = booleanPreferencesKey("use_moonless_night")
        val sleepTimerMinutes = intPreferencesKey("sleep_timer_minutes")
        val searchAllLibraries = booleanPreferencesKey("search_all_libraries")
        val downloadWifiOnly = booleanPreferencesKey("download_wifi_only")
        val autoDeleteFinishedDownloads = booleanPreferencesKey("auto_delete_finished_downloads")
        val useOfflineMode = booleanPreferencesKey("use_offline_mode")
        val appIcon = stringPreferencesKey("app_icon")
    }

    override val settings: Flow<AppSettings> =
        context.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map(::toAppSettings)

    override suspend fun setThemePreference(themePreference: String) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.themePreference] = themePreference
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.useDynamicColor] = enabled
        }
    }

    override suspend fun setCoverTheme(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.useCoverTheme] = enabled
        }
    }

    override suspend fun setMoonlessNight(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.useMoonlessNight] = enabled
        }
    }

    override suspend fun setSleepTimerMinutes(minutes: Int) {
        val coerced =
            minutes.coerceIn(
                AppSettings.MIN_SLEEP_TIMER_MINUTES,
                AppSettings.MAX_SLEEP_TIMER_MINUTES,
            )
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.sleepTimerMinutes] = coerced
        }
    }

    override suspend fun setSearchAllLibraries(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.searchAllLibraries] = enabled
        }
    }

    override suspend fun setDownloadWifiOnly(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.downloadWifiOnly] = enabled
        }
    }

    override suspend fun setAutoDeleteFinishedDownloads(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.autoDeleteFinishedDownloads] = enabled
        }
    }

    override suspend fun setOfflineMode(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.useOfflineMode] = enabled
        }
    }

    override suspend fun setAppIcon(icon: AppIcon) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.appIcon] = icon.name
        }
    }

    private fun toAppSettings(preferences: Preferences): AppSettings =
        AppSettings(
            themePreference =
                preferences[Keys.themePreference]
                    ?: AppSettings.DEFAULT_THEME_PREFERENCE,
            useDynamicColor = preferences[Keys.useDynamicColor] ?: true,
            useCoverTheme = preferences[Keys.useCoverTheme] ?: true,
            useMoonlessNight = preferences[Keys.useMoonlessNight] ?: false,
            sleepTimerMinutes =
                (preferences[Keys.sleepTimerMinutes] ?: AppSettings.DEFAULT_SLEEP_TIMER_MINUTES)
                    .coerceIn(
                        AppSettings.MIN_SLEEP_TIMER_MINUTES,
                        AppSettings.MAX_SLEEP_TIMER_MINUTES,
                    ),
            searchAllLibraries = preferences[Keys.searchAllLibraries] ?: true,
            downloadWifiOnly = preferences[Keys.downloadWifiOnly] ?: false,
            autoDeleteFinishedDownloads = preferences[Keys.autoDeleteFinishedDownloads] ?: false,
            useOfflineMode = preferences[Keys.useOfflineMode] ?: false,
            appIcon = AppIcon.fromName(preferences[Keys.appIcon]),
        )
}
