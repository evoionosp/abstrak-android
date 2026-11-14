package org.evoionosp.abstrak.domain.download.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.testing.FakeDownloadRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteAllDownloadsUseCaseTest {
    @Test
    fun `deletes everything through the repository`() =
        runTest {
            val downloads = FakeDownloadRepository()
            val useCase = DeleteAllDownloadsUseCase(downloads)
            useCase()
            assertEquals(1, downloads.deleteAllCount)
        }
}
