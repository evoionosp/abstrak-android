package org.evoionosp.abstrak.presentation.search
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.image.LocalAccessToken
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private var state by mutableStateOf(SearchUiState())
    private val queryState = TextFieldState(initialText = "query")

    @Test
    fun `new results inserted above the first row are shown while at the top`() {
        state = SearchUiState(results = results(30))
        showSearch()
        state = SearchUiState(results = listOf(result("b", "Book B")) + results(30))
        composeRule.waitForIdle()
        assertStartsInList("Book B")
        composeRule.onNodeWithText("Book 0").assertIsDisplayed()
    }

    @Test
    fun `results changes leave a scrolled list where it is`() {
        state = SearchUiState(results = results(30))
        showSearch()
        scrollTo(10)
        composeRule.onNodeWithText("Book 0").assertDoesNotExist()
        state = SearchUiState(results = listOf(result("x", "Book X")) + results(30))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Book 10").assertIsDisplayed()
        composeRule.onNodeWithText("Book X").assertDoesNotExist()
        composeRule.onNodeWithText("Book 0").assertDoesNotExist()
    }

    @Test
    fun `a new query starts the list at the top and keeps the field focused`() {
        state = SearchUiState(results = results(30))
        showSearch()
        scrollTo(10)
        val field = composeRule.onNode(hasSetTextAction())
        field.performClick()
        field.assertIsFocused()
        queryState.edit { replace(0, length, "other") }
        composeRule.waitForIdle()
        state = SearchUiState(results = newResults(30))
        composeRule.waitForIdle()
        assertStartsInList("New Book 0")
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun `a saved scroll position survives a restore`() {
        state = SearchUiState(results = results(30))
        showSearch()
        scrollTo(10)
        composeRule.onNodeWithText("Book 0").assertDoesNotExist()
        composeRule.activityRule.scenario.recreate()
        showSearch()
        composeRule.onNodeWithText("Book 10").assertIsDisplayed()
        composeRule.onNodeWithText("Book 0").assertDoesNotExist()
    }

    @Test
    fun `with an unknown search setting the placeholder stays neutral even for a known library`() {
        state =
            SearchUiState(
                results = null,
                searchAllLibraries = null,
                selectedLibraryName = "Audiobooks",
            )
        composeRule.setContent {
            CompositionLocalProvider(LocalAccessToken provides "test-token") {
                SearchScreen(
                    state = state,
                    queryState = TextFieldState(initialText = ""),
                    bottomBarPadding = 0.dp,
                    onOpenSettings = {},
                    onOpenAudiobook = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.root_search)).assertIsDisplayed()
        val libraryPlaceholder =
            composeRule.onNodeWithText(
                composeRule.activity.getString(R.string.search_placeholder_library, "Audiobooks"),
            )
        libraryPlaceholder.assertDoesNotExist()
    }

    @Test
    fun `the field takes focus only on the first composition`() {
        state = SearchUiState(results = results(30))
        val emptyQueryState = TextFieldState(initialText = "")
        showSearchWith(emptyQueryState)
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        composeRule.activityRule.scenario.recreate()
        showSearchWith(emptyQueryState)
        composeRule.onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun `the search pill is as tall as the settings button`() {
        showSearch()
        val settings =
            composeRule
                .onNodeWithContentDescription(composeRule.activity.getString(R.string.settings_icon_desc))
                .getUnclippedBoundsInRoot()
        composeRule.onNode(hasSetTextAction()).assertHeightIsEqualTo(settings.bottom - settings.top)
    }

    private fun showSearch() {
        showSearchWith(queryState)
    }

    private fun showSearchWith(queryState: TextFieldState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAccessToken provides "test-token") {
                SearchScreen(
                    state = state,
                    queryState = queryState,
                    bottomBarPadding = 0.dp,
                    onOpenSettings = {},
                    onOpenAudiobook = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun scrollTo(index: Int) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
        composeRule.waitForIdle()
    }

    private fun assertStartsInList(text: String) {
        val listTop = composeRule.onNode(hasScrollToIndexAction()).getUnclippedBoundsInRoot().top
        val itemTop = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot().top
        assertTrue("$text starts inside the list", itemTop >= listTop)
    }

    private fun results(count: Int): List<SearchResultUiModel> = (0 until count).map { result("$it", "Book $it") }

    private fun newResults(count: Int): List<SearchResultUiModel> = (0 until count).map { result("new-$it", "New Book $it") }

    private fun result(
        id: String,
        title: String,
    ) = SearchResultUiModel(
        audiobook =
            AudiobookUiModel(
                id = id,
                libraryId = "lib-1",
                title = title,
                author = "Author $id",
                authorNames = listOf("Author $id"),
                coverUrl = "",
                durationInSeconds = 3_600L,
                durationLabel = "1h",
                progressSeconds = null,
                timeLeftLabel = null,
            ),
        libraryName = "Library",
    )
}
