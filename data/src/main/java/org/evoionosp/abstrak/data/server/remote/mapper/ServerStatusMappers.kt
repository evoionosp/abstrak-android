package org.evoionosp.abstrak.data.server.remote.mapper
import org.evoionosp.abstrak.data.server.remote.dto.LoginStatusResponseDto
import org.evoionosp.abstrak.domain.server.model.ServerStatus

internal fun LoginStatusResponseDto.toDomain(): ServerStatus =
    ServerStatus(
        app = app.orEmpty(),
        serverVersion = serverVersion.orEmpty(),
        isInit = isInit ?: false,
        language = language.orEmpty(),
        authMethods = authMethods ?: emptyList(),
        authLoginCustomMessage = authFormData?.authLoginCustomMessage.orEmpty(),
    )
