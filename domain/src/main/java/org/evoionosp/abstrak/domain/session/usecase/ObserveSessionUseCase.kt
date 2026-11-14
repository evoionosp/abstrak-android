package org.evoionosp.abstrak.domain.session.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore

class ObserveSessionUseCase
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) {
        operator fun invoke(): Flow<LoginSession?> = sessionStore.session
    }
