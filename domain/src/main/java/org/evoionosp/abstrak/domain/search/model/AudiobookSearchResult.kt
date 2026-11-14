package org.evoionosp.abstrak.domain.search.model
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook

data class AudiobookSearchResult(
    val audiobook: Audiobook,
    val libraryName: String,
)
