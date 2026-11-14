package org.evoionosp.abstrak.presentation.home

sealed interface HomeUiEvent {
    data object SessionExpired : HomeUiEvent
}
