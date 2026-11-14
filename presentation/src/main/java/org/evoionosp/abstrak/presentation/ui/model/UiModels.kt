package org.evoionosp.abstrak.presentation.ui.model
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary
import org.evoionosp.abstrak.presentation.ui.format.toDurationLabel

data class AudiobookUiModel(
    val id: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val authorNames: List<String>,
    val coverUrl: String,
    val durationInSeconds: Long?,
    val durationLabel: String,
    val progressSeconds: Double?,
    val timeLeftLabel: String?,
    val hasAudio: Boolean = true,
)

data class LibraryUiModel(
    val id: String,
    val name: String,
    val isSelected: Boolean,
)

fun Audiobook.toUiModel(): AudiobookUiModel {
    val names = author.toAuthorNames()
    return AudiobookUiModel(
        id = id,
        libraryId = libraryId,
        title = title,
        author = author,
        authorNames = names,
        coverUrl = coverUrl,
        durationInSeconds = durationInSeconds,
        durationLabel = durationInSeconds?.toDurationLabel() ?: "",
        progressSeconds = progressSeconds,
        timeLeftLabel = timeLeftLabel(durationInSeconds, progressSeconds),
        hasAudio = hasAudio,
    )
}

fun AudiobookUiModel.toDomain(): Audiobook =
    Audiobook(
        id = id,
        libraryId = libraryId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        series = null,
        durationInSeconds = durationInSeconds,
        progressSeconds = progressSeconds,
        hasAudio = hasAudio,
    )

fun AudiobookLibrary.toUiModel(): LibraryUiModel =
    LibraryUiModel(
        id = id,
        name = name,
        isSelected = isSelected,
    )

internal fun String.toAuthorNames(): List<String> =
    split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("Unknown Author") }

internal fun timeLeftLabel(
    durationInSeconds: Long?,
    progressSeconds: Double?,
): String? {
    if (durationInSeconds == null || progressSeconds == null) return null
    val remainingSeconds = durationInSeconds - progressSeconds
    if (remainingSeconds <= 0) return null
    val totalMinutes = Math.round(remainingSeconds / 60.0).coerceAtLeast(1L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "%dh %02dm left".format(hours, minutes)
    } else {
        "%dm left".format(totalMinutes)
    }
}
