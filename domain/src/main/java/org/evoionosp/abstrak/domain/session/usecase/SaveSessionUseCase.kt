package org.evoionosp.abstrak.domain.session.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore

class SaveSessionUseCase
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) {
        suspend operator fun invoke(session: LoginSession): LoginSession = sessionStore.saveSession(session)
    }
