package org.evoionosp.abstrak.domain.server.repository
import org.evoionosp.abstrak.domain.server.model.ServerCheckResult
import org.evoionosp.abstrak.domain.server.model.ServerStatus

interface ServerRepository {
    suspend fun ping(baseUrl: String): ServerCheckResult<Boolean>

    suspend fun getStatus(baseUrl: String): ServerCheckResult<ServerStatus>

    suspend fun healthCheck(baseUrl: String): ServerCheckResult<Boolean>
}
