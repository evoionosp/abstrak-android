package org.evoionosp.abstrak.domain.audiobook.model

data class AudiobookChapter(
    val title: String,
    val startInSeconds: Long,
    val endInSeconds: Long?,
)
