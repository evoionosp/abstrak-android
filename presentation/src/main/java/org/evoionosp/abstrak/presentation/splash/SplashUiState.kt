package org.evoionosp.abstrak.presentation.splash

data class SplashUiState(
    val isLoading: Boolean = true,
    val startupDestination: StartupDestination = StartupDestination.Auth,
)
