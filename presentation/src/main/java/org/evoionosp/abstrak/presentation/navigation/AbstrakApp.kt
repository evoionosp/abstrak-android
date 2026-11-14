package org.evoionosp.abstrak.presentation.navigation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.AbstrakSnackbar
import org.evoionosp.abstrak.presentation.designsystem.theme.ThemePreference
import org.evoionosp.abstrak.presentation.permissions.RequestNotificationPermissionEffect
import org.evoionosp.abstrak.presentation.player.MiniPlayerRoute
import org.evoionosp.abstrak.presentation.player.NowPlayingRoute
import org.evoionosp.abstrak.presentation.player.NowPlayingViewModel
import org.evoionosp.abstrak.presentation.player.gesture.SheetDragState
import org.evoionosp.abstrak.presentation.player.overlay.MINI_BOTTOM_GAP
import org.evoionosp.abstrak.presentation.player.overlay.NowPlayingOverlay
import org.evoionosp.abstrak.presentation.settings.SettingsUiState
import org.evoionosp.abstrak.presentation.splash.SplashUiState
import org.evoionosp.abstrak.presentation.splash.StartupDestination
import org.evoionosp.abstrak.presentation.ui.image.LocalAccessToken
import org.evoionosp.abstrak.presentation.ui.message.LocalSnackbarController
import org.evoionosp.abstrak.presentation.ui.message.ShowSnackbarMessages
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController

@Composable
fun AbstrakApp(
    splashState: SplashUiState,
    settingsState: SettingsUiState,
    onRetryCatalogBootstrap: () -> Unit,
    onLogout: () -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCoverThemeChange: (Boolean) -> Unit,
    onMoonlessNightChange: (Boolean) -> Unit,
    snackbarController: SnackbarController,
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    ShowSnackbarMessages(controller = snackbarController, hostState = snackbarHostState)
    val baseRoute =
        when (splashState.startupDestination) {
            StartupDestination.Auth -> AppRoute.Auth
            is StartupDestination.Home -> AppRoute.Home
            is StartupDestination.CatalogLoadError -> AppRoute.CatalogError
        }
    key(baseRoute) {
        val navController = rememberNavController()
        val playingAudiobook by nowPlayingViewModel.playingAudiobook.collectAsStateWithLifecycle()
        var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
        val sheetScope = rememberCoroutineScope()
        val sheetDrag =
            remember(sheetScope) {
                SheetDragState(
                    scope = sheetScope,
                    onCollapsed = { isNowPlayingExpanded = false },
                    onExpanded = { isNowPlayingExpanded = true },
                )
            }
        LaunchedEffect(isNowPlayingExpanded, sheetDrag.travelKnown) {
            if (!sheetDrag.travelKnown) return@LaunchedEffect
            if (isNowPlayingExpanded) {
                sheetDrag.expand()
            } else {
                sheetDrag.collapse()
            }
        }
        LaunchedEffect(playingAudiobook == null) {
            if (playingAudiobook == null) isNowPlayingExpanded = false
        }
        var bottomInsetDp by remember { mutableStateOf(0.dp) }
        var miniHeightDp by remember { mutableStateOf(0.dp) }
        val layoutDirection = LocalLayoutDirection.current
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val homeDestination = splashState.startupDestination as? StartupDestination.Home
        val accessToken = homeDestination?.session?.accessToken.orEmpty()
        if (homeDestination != null) {
            RequestNotificationPermissionEffect()
        }
        CompositionLocalProvider(
            LocalSnackbarController provides snackbarController,
            LocalAccessToken provides accessToken,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        RootNavigationBottomBar(
                            navController = navController,
                            currentRoute = currentRoute,
                            sheetDrag = sheetDrag,
                        )
                    },
                ) { innerPadding ->
                    bottomInsetDp = innerPadding.calculateBottomPadding()
                    val miniTrailPadding =
                        if (playingAudiobook != null) {
                            miniHeightDp + MINI_BOTTOM_GAP
                        } else {
                            0.dp
                        }
                    AbstrakNavHost(
                        navController = navController,
                        startDestination = baseRoute.route,
                        splashState = splashState,
                        settingsState = settingsState,
                        contentPadding =
                            PaddingValues(
                                start = innerPadding.calculateStartPadding(layoutDirection),
                                top = innerPadding.calculateTopPadding(),
                                end = innerPadding.calculateEndPadding(layoutDirection),
                                bottom =
                                    innerPadding.calculateBottomPadding() +
                                        if (playingAudiobook != null) miniHeightDp else 0.dp,
                            ),
                        miniTrailPadding = miniTrailPadding,
                        onRetryCatalogBootstrap = onRetryCatalogBootstrap,
                        onLogout = onLogout,
                        onThemePreferenceChange = onThemePreferenceChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onCoverThemeChange = onCoverThemeChange,
                        onMoonlessNightChange = onMoonlessNightChange,
                        onOpenAudiobook = { audiobook ->
                            navController.navigate(
                                bookDetailsRoute(audiobook.libraryId, audiobook.id),
                            )
                        },
                        onOpenPlayer = { isNowPlayingExpanded = true },
                    )
                }
                NowPlayingOverlay(
                    targetExpanded = isNowPlayingExpanded,
                    sheetDrag = sheetDrag,
                    playingAudiobook = playingAudiobook,
                    themeFromCover = settingsState.useCoverTheme,
                    bottomInsetDp = bottomInsetDp,
                    onMinimize = { isNowPlayingExpanded = false },
                    onExpandMini = { isNowPlayingExpanded = true },
                    onDismissMini = nowPlayingViewModel::stopPlayback,
                    onMiniHeightKnown = { miniHeightDp = it },
                    miniPlayer = { audiobook ->
                        MiniPlayerRoute(audiobook = audiobook, viewModel = nowPlayingViewModel)
                    },
                    fullPlayer = { audiobook ->
                        NowPlayingRoute(
                            audiobook = audiobook,
                            onMinimize = { isNowPlayingExpanded = false },
                            onOpenBookDetails = { book ->
                                isNowPlayingExpanded = false
                                val entry = navController.currentBackStackEntry
                                val alreadyThere =
                                    entry?.destination?.route == AppRoute.BookDetails.route &&
                                        entry.arguments?.getString(BOOK_DETAILS_ARG_AUDIOBOOK_ID) == book.id
                                if (!alreadyThere) {
                                    navController.navigate(
                                        bookDetailsRoute(book.libraryId, book.id),
                                    )
                                }
                            },
                            viewModel = nowPlayingViewModel,
                        )
                    },
                )
                val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                bottom =
                                    when {
                                        playingAudiobook == null -> bottomInsetDp
                                        isNowPlayingExpanded -> navigationBarHeight
                                        else -> bottomInsetDp + miniHeightDp + MINI_BOTTOM_GAP
                                    },
                            ),
                    snackbar = { AbstrakSnackbar(it) },
                )
            }
        }
    }
}
