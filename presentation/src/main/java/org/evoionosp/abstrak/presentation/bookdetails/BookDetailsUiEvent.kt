package org.evoionosp.abstrak.presentation.bookdetails

sealed interface BookDetailsUiEvent {
    data object OpenPlayer : BookDetailsUiEvent
}
