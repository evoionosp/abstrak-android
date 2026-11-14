package org.evoionosp.abstrak.domain.download.usecase
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookTrack
import org.evoionosp.abstrak.domain.testing.FakeDownloadRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveLocalTracksUseCaseTest {
    private val repository = FakeDownloadRepository()
    private val useCase = ResolveLocalTracksUseCase(repository)

    @Test
    fun `only the tracks on the device resolve, by track index`() =
        runTest {
            repository.localFiles["book-1" to 0] = "/files/downloads/book-1/0.mp3"
            repository.localFiles["book-1" to 2] = "/files/downloads/book-1/2.mp3"
            repository.localFiles["book-2" to 1] = "/files/downloads/book-2/1.mp3"
            val local = useCase("book-1", listOf(track(0), track(1), track(2)))
            assertEquals(mapOf(0 to "/files/downloads/book-1/0.mp3", 2 to "/files/downloads/book-1/2.mp3"), local)
        }

    @Test
    fun `a book with nothing downloaded streams every track`() =
        runTest {
            assertEquals(emptyMap<Int, String>(), useCase("book-1", listOf(track(0), track(1))))
        }

    private fun track(index: Int) =
        AudiobookTrack(
            index = index,
            startOffsetInSeconds = index * 100L,
            durationInSeconds = 100L,
            title = "Track $index",
            remoteUrl = "https://example.com/$index.mp3",
            mimeType = "audio/mpeg",
        )
}
