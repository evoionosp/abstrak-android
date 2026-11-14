package org.evoionosp.abstrak.presentation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakTheme
import org.evoionosp.abstrak.presentation.navigation.AbstrakApp
import org.evoionosp.abstrak.presentation.settings.SettingsViewModel
import org.evoionosp.abstrak.presentation.splash.SplashViewModel
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var snackbarController: SnackbarController

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            splashViewModel.uiState.value.isLoading
        }
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()
            AbstrakTheme(
                themePreference = settingsState.themePreference,
                dynamicColor = settingsState.useDynamicColor,
                moonlessNight = settingsState.useMoonlessNight,
            ) {
                AbstrakApp(
                    splashState = splashState,
                    settingsState = settingsState,
                    onRetryCatalogBootstrap = splashViewModel::retryCatalogBootstrap,
                    onLogout = splashViewModel::logout,
                    onThemePreferenceChange = settingsViewModel::onThemePreferenceChange,
                    onDynamicColorChange = settingsViewModel::onDynamicColorChange,
                    onCoverThemeChange = settingsViewModel::onCoverThemeChange,
                    onMoonlessNightChange = settingsViewModel::onMoonlessNightChange,
                    snackbarController = snackbarController,
                )
            }
        }
    }
}
