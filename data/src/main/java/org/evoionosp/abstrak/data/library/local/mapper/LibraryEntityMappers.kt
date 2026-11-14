package org.evoionosp.abstrak.data.library.local.mapper
import org.evoionosp.abstrak.data.library.local.entity.LibraryEntity
import org.evoionosp.abstrak.domain.library.model.AudiobookLibrary

internal fun LibraryEntity.toDomain(): AudiobookLibrary =
    AudiobookLibrary(
        id = id,
        name = name,
        isSelected = isSelected,
    )
