package org.evoionosp.abstrak.data.download.work

interface DownloadWorkScheduler {
    fun scheduleBook(
        audiobookId: String,
        trackIndexes: List<Int>,
        wifiOnly: Boolean,
    )

    suspend fun allowAnyNetwork(
        audiobookId: String,
        trackIndexes: List<Int>,
    )

    fun cancelBook(audiobookId: String)

    fun cancelAll()
}
