package org.evoionosp.abstrak.domain.session

fun interface DownloadStore {
    suspend fun deleteAll()
}
