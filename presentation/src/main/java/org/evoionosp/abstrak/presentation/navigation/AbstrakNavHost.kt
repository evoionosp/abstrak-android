package org.evoionosp.abstrak.presentation.navigation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.evoionosp.abstrak.presentation.auth.AuthRoute
import org.evoionosp.abstrak.presentation.bookdetails.BookDetailsRoute
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference
import org.evoionosp.abstrak.presentation.downloads.DownloadsRoute
import org.evoionosp.abstrak.presentation.home.HomeRoute
import org.evoionosp.abstrak.presentation.library.LibraryRoute
import org.evoionosp.abstrak.presentation.search.SearchRoute
import org.evoionosp.abstrak.presentation.settings.AppearanceSettingsScreen
import org.evoionosp.abstrak.presentation.settings.PreferencesRoute
import org.evoionosp.abstrak.presentation.settings.ServerConnectionRoute
import org.evoionosp.abstrak.presentation.settings.SettingsUiState
import org.evoionosp.abstrak.presentation.settings.StorageDownloadsRoute
import org.evoionosp.abstrak.presentation.splash.CatalogBootstrapErrorScreen
import org.evoionosp.abstrak.presentation.splash.SplashUiState
import org.evoionosp.abstrak.presentation.splash.StartupDestination
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
internal fun AbstrakNavHost(
    navController: NavHostController,
    startDestination: String,
    splashState: SplashUiState,
    settingsState: SettingsUiState,
    contentPadding: PaddingValues,
    miniTrailPadding: Dp,
    onRetryCatalogBootstrap: () -> Unit,
    onLogout: () -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCoverThemeChange: (Boolean) -> Unit,
    onMoonlessNightChange: (Boolean) -> Unit,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val slideDistance =
        with(LocalDensity.current) { SharedAxisSlideDistance.roundToPx() } *
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { navEnterTransition(isPop = false, slideDistance = slideDistance) },
        exitTransition = { navExitTransition(isPop = false, slideDistance = slideDistance) },
        popEnterTransition = { navEnterTransition(isPop = true, slideDistance = slideDistance) },
        popExitTransition = { navExitTransition(isPop = true, slideDistance = slideDistance) },
    ) {
        composable(route = AppRoute.Auth.route) {
            AuthRoute(
                modifier = Modifier,
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) },
                onOpenServerConnection = { navController.navigate(AppRoute.ServerConnection.route) },
            )
        }
        composable(route = AppRoute.Home.route) {
            val startupDestination =
                splashState.startupDestination as? StartupDestination.Home
                    ?: return@composable
            HomeRoute(
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) },
                onSessionExpired = { navController.navigateToAuthRoot() },
                bottomBarPadding = contentPadding.calculateBottomPadding(),
                onOpenAudiobook = onOpenAudiobook,
                onOpenPlayer = onOpenPlayer,
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.Library.route) {
            val startupDestination =
                splashState.startupDestination as? StartupDestination.Home
                    ?: return@composable
            LibraryRoute(
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) },
                onSessionExpired = { navController.navigateToAuthRoot() },
                bottomBarPadding = contentPadding.calculateBottomPadding(),
                onOpenAudiobook = onOpenAudiobook,
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.Downloads.route) {
            if (splashState.startupDestination !is StartupDestination.Home) return@composable
            DownloadsRoute(
                onOpenStorageSettings = { navController.navigate(AppRoute.StorageDownloads.route) },
                onOpenAudiobook = onOpenAudiobook,
                bottomBarPadding = contentPadding.calculateBottomPadding(),
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.Search.route) {
            if (splashState.startupDestination !is StartupDestination.Home) return@composable
            SearchRoute(
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) },
                onOpenAudiobook = onOpenAudiobook,
                bottomBarPadding = contentPadding.calculateBottomPadding(),
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.CatalogError.route) {
            val startupDestination =
                splashState.startupDestination as? StartupDestination.CatalogLoadError
                    ?: return@composable
            CatalogBootstrapErrorScreen(
                error = startupDestination.error,
                onRetry = onRetryCatalogBootstrap,
                onLogout = onLogout,
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.Preferences.route) {
            PreferencesRoute(
                onBackClick = { navController.popBackStack() },
                onOpenAppearance = { navController.navigate(AppRoute.Appearance.route) },
                onOpenServerConnection = { navController.navigate(AppRoute.ServerConnection.route) },
                onOpenStorageDownloads = { navController.navigate(AppRoute.StorageDownloads.route) },
                onLoggedOut = { navController.navigateToAuthRoot() },
                showLogout = splashState.startupDestination != StartupDestination.Auth,
                miniTrailPadding = miniTrailPadding,
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.Appearance.route) {
            AppearanceSettingsScreen(
                settingsState = settingsState,
                onBackClick = { navController.popBackStack() },
                onThemePreferenceChange = onThemePreferenceChange,
                onDynamicColorChange = onDynamicColorChange,
                onCoverThemeChange = onCoverThemeChange,
                onMoonlessNightChange = onMoonlessNightChange,
                miniTrailPadding = miniTrailPadding,
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.ServerConnection.route) {
            ServerConnectionRoute(
                onBackClick = { navController.popBackStack() },
                miniTrailPadding = miniTrailPadding,
                modifier = Modifier,
            )
        }
        composable(route = AppRoute.StorageDownloads.route) {
            StorageDownloadsRoute(
                onBackClick = { navController.popBackStack() },
                miniTrailPadding = miniTrailPadding,
                modifier = Modifier,
            )
        }
        composable(
            route = AppRoute.BookDetails.route,
            arguments =
                listOf(
                    navArgument(BOOK_DETAILS_ARG_LIBRARY_ID) { type = NavType.StringType },
                    navArgument(BOOK_DETAILS_ARG_AUDIOBOOK_ID) { type = NavType.StringType },
                ),
        ) {
            BookDetailsRoute(
                onBackClick = { navController.popBackStack() },
                onOpenPlayer = onOpenPlayer,
                miniTrailPadding = miniTrailPadding,
            )
        }
    }
}

private fun NavHostController.navigateToAuthRoot() {
    navigate(AppRoute.Auth.route) {
        popUpTo(0)
    }
}
