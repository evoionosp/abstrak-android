package org.evoionosp.abstrak.domain.audiobook.model

data class Audiobook(
    val id: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val series: String?,
    val durationInSeconds: Long?,
    val progressSeconds: Double? = null,
    val hasAudio: Boolean = true,
)
