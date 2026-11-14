package org.evoionosp.abstrak.domain.download.usecase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.domain.download.repository.DownloadRepository

class GetStorageUsageUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
    ) {
        operator fun invoke(): Flow<StorageUsage> = downloadRepository.observeStorageUsage()
    }
