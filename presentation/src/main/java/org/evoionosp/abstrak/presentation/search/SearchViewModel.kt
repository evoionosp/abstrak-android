package org.evoionosp.abstrak.presentation.search
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.catalog.usecase.CatalogRefreshStage
import org.evoionosp.abstrak.domain.catalog.usecase.RefreshLibraryCatalogsUseCase
import org.evoionosp.abstrak.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.abstrak.domain.search.model.AudiobookSearchResult
import org.evoionosp.abstrak.domain.search.usecase.SearchAudiobooksUseCase
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.evoionosp.abstrak.presentation.ui.model.toUiModel

data class SearchResultUiModel(
    val audiobook: AudiobookUiModel,
    val libraryName: String,
)

data class SearchUiState(
    val results: List<SearchResultUiModel>? = null,
    val searchAllLibraries: Boolean? = null,
    val selectedLibraryName: String? = null,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        searchAudiobooks: SearchAudiobooksUseCase,
        observeSelectedLibrary: ObserveSelectedLibraryUseCase,
        appSettingsStore: AppSettingsStore,
        private val refreshLibraryCatalogs: RefreshLibraryCatalogsUseCase,
        @param:Named("default") private val defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val query = MutableStateFlow("")
        private val isRefreshing = MutableStateFlow(false)
        private val searchAllLibraries =
            appSettingsStore.settings.map { it.searchAllLibraries }.distinctUntilChanged()
        val uiState: StateFlow<SearchUiState> =
            combine(
                searchAudiobooks(query),
                searchAllLibraries,
                observeSelectedLibrary().map { it?.name }.distinctUntilChanged(),
                isRefreshing,
            ) { results, allLibraries, selectedLibraryName, refreshing ->
                SearchUiState(
                    results = results?.map { it.toUiModel() },
                    searchAllLibraries = allLibraries,
                    selectedLibraryName = selectedLibraryName,
                    isRefreshing = refreshing,
                )
            }.flowOn(defaultDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

        init {
            viewModelScope.launch {
                searchAllLibraries.collectLatest { allLibraries ->
                    if (!allLibraries) return@collectLatest
                    try {
                        refreshLibraryCatalogs().collect { stage ->
                            isRefreshing.value = stage == CatalogRefreshStage.SELECTED_LIBRARY
                        }
                    } finally {
                        isRefreshing.value = false
                    }
                }
            }
        }

        fun onQueryChange(query: String) {
            this.query.value = query
        }
    }

private fun AudiobookSearchResult.toUiModel(): SearchResultUiModel =
    SearchResultUiModel(audiobook = audiobook.toUiModel(), libraryName = libraryName)
