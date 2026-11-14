package org.evoionosp.abstrak.domain.session

fun interface LocalCatalogCleaner {
    suspend fun clear()
}
