package org.evoionosp.abstrak.presentation.splash
import org.evoionosp.abstrak.domain.library.model.CatalogError
import org.evoionosp.abstrak.domain.session.LoginSession

sealed interface StartupDestination {
    data object Auth : StartupDestination

    data class Home(
        val session: LoginSession,
    ) : StartupDestination

    data class CatalogLoadError(
        val session: LoginSession,
        val error: CatalogError,
    ) : StartupDestination
}
