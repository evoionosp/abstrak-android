package org.evoionosp.abstrak.domain.session.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore

class GetCurrentSessionUseCase
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) {
        suspend operator fun invoke(): LoginSession? = sessionStore.session.first()
    }
