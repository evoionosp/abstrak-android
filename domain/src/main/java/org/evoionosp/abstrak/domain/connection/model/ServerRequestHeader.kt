package org.evoionosp.abstrak.domain.connection.model
import java.util.UUID

data class ServerRequestHeader(
    val name: String,
    val value: String,
    val id: String = UUID.randomUUID().toString(),
) {
    fun clean(): ServerRequestHeader =
        copy(
            name = name.cleanHeaderName(),
            value = value.cleanHeaderValue(),
        )

    private fun String.cleanHeaderName(): String = replace(HEADER_NAME_INVALID_CHARS, "").trim()

    private fun String.cleanHeaderValue(): String = replace(HEADER_VALUE_INVALID_CHARS, "").trim()

    private companion object {
        val HEADER_NAME_INVALID_CHARS = Regex("[^!#\$%&'*+\\-.^_`|~0-9A-Za-z]")
        val HEADER_VALUE_INVALID_CHARS = Regex("[\\x00-\\x1F\\x7F]")
    }
}

fun List<ServerRequestHeader>.sanitizedForStorage(): List<ServerRequestHeader> =
    map { it.clean() }
        .distinctBy { it.name }
        .filterNot { it.name.isEmpty() }
        .filterNot { it.value.isEmpty() }

fun sanitizeUserAgent(raw: String): String = raw.replace(USER_AGENT_INVALID_CHARS, "").trim()

private val USER_AGENT_INVALID_CHARS = Regex("[\\x00-\\x1F\\x7F]")
