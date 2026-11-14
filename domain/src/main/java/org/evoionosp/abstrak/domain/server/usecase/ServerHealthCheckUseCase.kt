package org.evoionosp.abstrak.domain.server.usecase
import javax.inject.Inject
import org.evoionosp.abstrak.domain.server.model.ServerCheckResult
import org.evoionosp.abstrak.domain.server.repository.ServerRepository

class ServerHealthCheckUseCase
    @Inject
    constructor(
        private val repository: ServerRepository,
    ) {
        suspend operator fun invoke(baseUrl: String): ServerCheckResult<Boolean> = repository.healthCheck(baseUrl)
    }
