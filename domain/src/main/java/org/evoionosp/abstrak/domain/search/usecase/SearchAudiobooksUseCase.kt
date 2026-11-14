package org.evoionosp.abstrak.domain.search.usecase
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.evoionosp.abstrak.domain.audiobook.model.Audiobook
import org.evoionosp.abstrak.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.abstrak.domain.library.repository.LibraryRepository
import org.evoionosp.abstrak.domain.search.SearchQuery
import org.evoionosp.abstrak.domain.search.model.AudiobookSearchResult
import org.evoionosp.abstrak.domain.search.normalizeForSearch
import org.evoionosp.abstrak.domain.settings.AppSettingsStore

class SearchAudiobooksUseCase
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
        private val libraryRepository: LibraryRepository,
        private val appSettingsStore: AppSettingsStore,
        @param:Named("default") private val defaultDispatcher: CoroutineDispatcher,
    ) {
        operator fun invoke(queries: Flow<String>): Flow<List<AudiobookSearchResult>?> =
            combine(
                searchableBooks(),
                queries.distinctUntilChanged().map(SearchQuery::parse),
            ) { books, query ->
                if (query.isBlank) null else books.search(query)
            }.flowOn(defaultDispatcher)

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun searchableBooks(): Flow<List<SearchableBook>> =
            combine(
                appSettingsStore.settings.map { it.searchAllLibraries },
                libraryRepository.observeLibraries(),
                ::Pair,
            ).distinctUntilChanged()
                .flatMapLatest { (allLibraries, libraries) ->
                    val libraryNames = libraries.associate { it.id to it.name }
                    val selectedLibraryId = libraries.firstOrNull { it.isSelected }?.id
                    val books =
                        when {
                            allLibraries -> audiobookRepository.observeAllAudiobooks()
                            selectedLibraryId != null -> audiobookRepository.observeAudiobooks(selectedLibraryId)
                            else -> flowOf(emptyList())
                        }
                    books.map { catalog ->
                        catalog.mapNotNull { book ->
                            libraryNames[book.libraryId]?.let { name -> SearchableBook(book, name) }
                        }
                    }
                }

        private fun List<SearchableBook>.search(query: SearchQuery): List<AudiobookSearchResult> =
            filter { query.matches(it.title, it.author) }
                .sortedWith(
                    compareBy(
                        { !query.matchesTitle(it.title) },
                        { it.title },
                        { it.libraryName },
                        { it.audiobook.id },
                    ),
                ).map { AudiobookSearchResult(audiobook = it.audiobook, libraryName = it.libraryName) }

        private class SearchableBook(
            val audiobook: Audiobook,
            val libraryName: String,
        ) {
            val title: String = normalizeForSearch(audiobook.title)
            val author: String = normalizeForSearch(audiobook.author)
        }
    }
