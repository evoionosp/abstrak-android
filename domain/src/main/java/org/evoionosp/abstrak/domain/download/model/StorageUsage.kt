package org.evoionosp.abstrak.domain.download.model

data class StorageUsage(
    val downloadsBytes: Long,
    val cacheBytes: Long,
    val deviceFreeBytes: Long,
) {
    val abstrakBytes: Long
        get() = downloadsBytes + cacheBytes
}
