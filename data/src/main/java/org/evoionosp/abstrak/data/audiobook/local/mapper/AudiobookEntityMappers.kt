package org.evoionosp.abstrak.data.audiobook.local.mapper
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookChapterEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookTrackEntity
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookDetail
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack

internal fun AudiobookEntity.toDomain(): Audiobook =
    Audiobook(
        id = id,
        libraryId = libraryId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        series = series,
        durationInSeconds = durationInSeconds,
        hasAudio = hasAudio,
    )

internal fun AudiobookDetailEntity.toAudiobook(): Audiobook =
    Audiobook(
        id = audiobookId,
        libraryId = libraryId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        series = series,
        durationInSeconds = durationInSeconds,
    )

internal fun AudiobookChapterEntity.toDomain(): AudiobookChapter =
    AudiobookChapter(
        title = title,
        startInSeconds = startInSeconds,
        endInSeconds = endInSeconds,
    )

internal fun AudiobookTrackEntity.toDomain(): AudiobookTrack =
    AudiobookTrack(
        index = trackIndex,
        startOffsetInSeconds = startOffsetInSeconds,
        durationInSeconds = durationInSeconds,
        title = title,
        remoteUrl = remoteUrl,
        mimeType = mimeType,
    )

internal fun AudiobookDetailEntity.toDomain(
    chapters: List<AudiobookChapterEntity>,
    tracks: List<AudiobookTrackEntity>,
): AudiobookDetail =
    AudiobookDetail(
        audiobook = toAudiobook(),
        description = description,
        chapters = chapters.map { it.toDomain() },
        tracks = tracks.map { it.toDomain() },
        refreshedAtMillis = refreshedAtMillis,
    )
