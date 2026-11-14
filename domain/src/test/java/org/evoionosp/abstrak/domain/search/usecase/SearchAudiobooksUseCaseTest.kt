package org.evoionosp.abstrak.domain.search.usecase
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.search.model.AudiobookSearchResult
import org.evoionosp.abstrak.domain.settings.AppSettings
import org.evoionosp.abstrak.domain.testing.FakeAppSettingsStore
import org.evoionosp.abstrak.domain.testing.FakeAudiobookRepository
import org.evoionosp.abstrak.domain.testing.FakeLibraryRepository
import org.evoionosp.abstrak.domain.testing.testAudiobook
import org.evoionosp.abstrak.domain.testing.testLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchAudiobooksUseCaseTest {
    private val audiobooksLibrary = testLibrary(id = "lib-1", name = "Audiobooks", isSelected = true)
    private val allLibrary = testLibrary(id = "lib-2", name = "All")
    private val dune = book("dune", "lib-1", "Dune", "Frank Herbert")
    private val hailMary = book("hail-mary", "lib-1", "Project Hail Mary", "Andy Weir")
    private val childrenOfDune = book("children", "lib-2", "Children of Dune", "Frank Herbert")
    private val dracula = book("dracula", "lib-2", "Dracula", "Bram Stoker")

    @Test
    fun `a blank query emits null instead of results`() =
        runTest {
            val graph = graph()
            assertNull(graph.useCase(flowOf("   ")).first())
        }

    @Test
    fun `matches titles and authors across every library with library names`() =
        runTest {
            val graph = graph()
            assertEquals(
                listOf("Children of Dune" to "All", "Dune" to "Audiobooks"),
                graph.useCase(flowOf("DUNE")).first().titlesWithLibraries(),
            )
            assertEquals(
                listOf("Children of Dune" to "All", "Dune" to "Audiobooks"),
                graph.useCase(flowOf("herbert")).first().titlesWithLibraries(),
            )
        }

    @Test
    fun `title matches rank above author-only matches`() =
        runTest {
            val stoker = book("stoker", "lib-1", "A Life", "Bram Stoker")
            val draculaSequel = book("sequel", "lib-1", "Dracula's Guest", "Someone Else")
            val graph = graph(lib1Books = listOf(stoker, draculaSequel), lib2Books = listOf(dracula))
            assertEquals(
                listOf("A Life", "Dracula"),
                graph.useCase(flowOf("stoker")).first().titles(),
            )
            assertEquals(
                listOf("Dracula", "Dracula's Guest"),
                graph.useCase(flowOf("dracula")).first().titles(),
            )
            val authorDracula = book("author-dracula", "lib-1", "Collected Letters", "Dracula Fan")
            graph.audiobooks.setAudiobooks("lib-1", listOf(stoker, draculaSequel, authorDracula))
            assertEquals(
                listOf("Dracula", "Dracula's Guest", "Collected Letters"),
                graph.useCase(flowOf("dracula")).first().titles(),
            )
        }

    @Test
    fun `every word must match, across title and author`() =
        runTest {
            val graph = graph()
            assertEquals(listOf("Children of Dune", "Dune"), graph.useCase(flowOf("dune frank")).first().titles())
            assertEquals(listOf("Project Hail Mary"), graph.useCase(flowOf("mary weir")).first().titles())
            assertEquals(emptyList<String>(), graph.useCase(flowOf("dune weir")).first().titles())
        }

    @Test
    fun `with the preference off only the selected library is searched`() =
        runTest {
            val graph = graph(settings = AppSettings(searchAllLibraries = false))
            assertEquals(
                listOf("Dune" to "Audiobooks"),
                graph.useCase(flowOf("dune")).first().titlesWithLibraries(),
            )
        }

    @Test
    fun `toggling the preference re-scopes an open query`() =
        runTest {
            val graph = graph()
            graph.useCase(flowOf("dune")).test {
                assertEquals(listOf("Children of Dune", "Dune"), awaitItem().titles())
                graph.settings.setSearchAllLibraries(false)
                assertEquals(listOf("Dune"), awaitItem().titles())
                graph.settings.setSearchAllLibraries(true)
                assertEquals(listOf("Children of Dune", "Dune"), awaitItem().titles())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `results follow the query and catalog updates`() =
        runTest {
            val graph = graph(lib2Books = emptyList())
            val query = MutableStateFlow("")
            graph.useCase(query).test {
                assertNull(awaitItem())
                query.value = "dune"
                assertEquals(listOf("Dune"), awaitItem().titles())
                graph.audiobooks.setAudiobooks("lib-2", listOf(childrenOfDune))
                assertEquals(listOf("Children of Dune", "Dune"), awaitItem().titles())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `books of a library missing from the library list are left out`() =
        runTest {
            val orphan = book("orphan", "gone", "Dune Messiah", "Frank Herbert")
            val graph = graph()
            graph.audiobooks.setAudiobooks("gone", listOf(orphan))
            assertEquals(listOf("Children of Dune", "Dune"), graph.useCase(flowOf("dune")).first().titles())
        }

    @Test
    fun `no selected library with the preference off finds nothing`() =
        runTest {
            val graph =
                graph(
                    settings = AppSettings(searchAllLibraries = false),
                    libraries = listOf(audiobooksLibrary.copy(isSelected = false), allLibrary),
                )
            assertEquals(emptyList<String>(), graph.useCase(flowOf("dune")).first().titles())
        }

    private fun TestScope.graph(
        settings: AppSettings = AppSettings(),
        libraries: List<org.evoionosp.abstrak.domain.library.model.AudiobookLibrary> =
            listOf(audiobooksLibrary, allLibrary),
        lib1Books: List<Audiobook> = listOf(dune, hailMary),
        lib2Books: List<Audiobook> = listOf(childrenOfDune, dracula),
    ): Graph {
        val settingsStore = FakeAppSettingsStore(settings)
        val libraryRepository = FakeLibraryRepository(libraries = libraries)
        val audiobookRepository = FakeAudiobookRepository()
        audiobookRepository.setAudiobooks("lib-1", lib1Books)
        audiobookRepository.setAudiobooks("lib-2", lib2Books)
        val useCase =
            SearchAudiobooksUseCase(
                audiobookRepository = audiobookRepository,
                libraryRepository = libraryRepository,
                appSettingsStore = settingsStore,
                defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        return Graph(useCase, settingsStore, audiobookRepository)
    }

    private class Graph(
        val useCase: SearchAudiobooksUseCase,
        val settings: FakeAppSettingsStore,
        val audiobooks: FakeAudiobookRepository,
    )

    private fun book(
        id: String,
        libraryId: String,
        title: String,
        author: String,
    ) = testAudiobook(id = id, libraryId = libraryId).copy(title = title, author = author)

    private fun List<AudiobookSearchResult>?.titles(): List<String> = orEmpty().map { it.audiobook.title }

    private fun List<AudiobookSearchResult>?.titlesWithLibraries(): List<Pair<String, String>> =
        orEmpty().map { it.audiobook.title to it.libraryName }
}
