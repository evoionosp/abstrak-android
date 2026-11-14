package org.evoionosp.abstrak.domain.library.model

sealed interface DomainResult<out T> {
    data class Success<T>(
        val data: T,
    ) : DomainResult<T>

    data class Failure(
        val error: CatalogError,
    ) : DomainResult<Nothing>
}
