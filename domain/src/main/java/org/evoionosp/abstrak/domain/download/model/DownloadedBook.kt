package org.evoionosp.abstrak.domain.download.model
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook

data class DownloadedBook(
    val audiobook: Audiobook,
    val summary: BookDownloadSummary,
)
