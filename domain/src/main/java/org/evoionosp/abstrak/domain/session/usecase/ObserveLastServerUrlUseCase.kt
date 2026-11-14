package org.evoionosp.abstrak.domain.session.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.session.SessionStore

class ObserveLastServerUrlUseCase
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) {
        operator fun invoke(): Flow<String?> = sessionStore.lastServerUrl
    }
