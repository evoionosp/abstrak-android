package org.evoionosp.abstrak.presentation.library
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.library.model.SyncStatus
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.home.HomeUiState
import org.evoionosp.abstrak.presentation.ui.image.LocalAccessToken
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.evoionosp.abstrak.presentation.ui.model.LibraryUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private var isGridView by mutableStateOf(true)

    @Test
    @Config(qualifiers = "h1000dp")
    fun `the library lists its books without an authors section`() {
        showLibrary(HomeUiState(audiobooks = listOf(book("dune", "Dune", "Frank Herbert"))).withLibrary())
        composeRule.onNodeWithText("Dune").assertExists()
        composeRule.onAllNodesWithText("Frank Herbert").assertCountEquals(1)
        composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.library_stats_authors)).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "h1000dp")
    fun `audiobooks and ebooks show in their own sections`() {
        val opened = mutableListOf<String>()
        showLibrary(
            HomeUiState(
                audiobooks = listOf(book("dune", "Dune", "Frank Herbert")),
                ebooks = listOf(book("manual", "Field Manual", "Anon", hasAudio = false)),
            ).withLibrary(),
            onOpenAudiobook = { opened += it.id },
        )
        val audiobooksHeader = boundsOf(composeRule.activity.getString(R.string.library_audiobooks))
        composeRule.onNodeWithText(plural(R.plurals.library_audiobooks_count, 1)).assertExists()
        val booksHeader = boundsOf(plural(R.plurals.library_books_count, 1))
        assertTrue(boundsOf("Dune").top > audiobooksHeader.bottom)
        assertTrue(booksHeader.top > boundsOf("Dune").bottom)
        assertTrue(boundsOf("Field Manual").top > booksHeader.bottom)
        composeRule.onNodeWithText("Field Manual").performClick()
        assertEquals(listOf("manual"), opened)
    }

    @Test
    fun `a library of only ebooks shows the books section with the layout toggle`() {
        showLibrary(HomeUiState(ebooks = listOf(book("manual", "Field Manual", "Anon", hasAudio = false))).withLibrary())
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.library_audiobooks)).assertDoesNotExist()
        composeRule.onNodeWithText(plural(R.plurals.library_books_count, 1)).assertExists()
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.library_show_list))
            .assertExists()
    }

    @Test
    fun `in the list an ebook reads Ebook in place of a duration`() {
        isGridView = false
        showLibrary(HomeUiState(ebooks = listOf(book("manual", "Field Manual", "Anon", hasAudio = false))).withLibrary())
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.catalog_ebook)).assertExists()
        composeRule.onNodeWithText("0:00").assertDoesNotExist()
    }

    @Test
    fun `switching between tiles and list keeps the books header in place`() {
        showLibrary(HomeUiState(audiobooks = (1..6).map { book("b$it", "Book $it", "Author $it") }).withLibrary())
        val header = composeRule.activity.getString(R.string.library_audiobooks)
        val tilesTop = composeRule.onNodeWithText(header).getUnclippedBoundsInRoot().top
        isGridView = false
        composeRule.waitForIdle()
        val listTop = composeRule.onNodeWithText(header).getUnclippedBoundsInRoot().top
        assertEquals(tilesTop, listTop)
    }

    @Test
    fun `the sync chip sits centered between the header and the books`() {
        showLibrary(syncedHoursAgo(3))
        val chip = composeRule.onNodeWithText(plural(R.plurals.library_sync_hours_ago, 3)).getUnclippedBoundsInRoot()
        assertTrue(chip.top >= settingsButtonBounds().bottom)
        val booksHeader = composeRule.activity.getString(R.string.library_audiobooks)
        assertTrue(chip.bottom <= composeRule.onNodeWithText(booksHeader).getUnclippedBoundsInRoot().top)
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        assertEquals(((root.left + root.right) / 2).value, ((chip.left + chip.right) / 2).value, 1f)
    }

    @Test
    fun `in offline mode the chip says so`() {
        showLibrary(syncedHoursAgo(3).copy(isOfflineMode = true))
        val status = plural(R.plurals.library_sync_hours_ago, 3)
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.library_sync_offline_label, status))
            .assertExists()
    }

    @Test
    fun `tapping the sync chip opens the sync sheet, whose switch turns offline mode on`() {
        val offlineModeChanges = mutableListOf<Boolean>()
        showLibrary(syncedHoursAgo(3), onOfflineModeChange = { offlineModeChanges += it })
        val offlineMode = composeRule.activity.getString(R.string.library_sync_offline_mode)
        composeRule.onNodeWithText(offlineMode).assertDoesNotExist()
        composeRule.onNodeWithText(plural(R.plurals.library_sync_hours_ago, 3)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.library_sync_sheet_title)).assertExists()
        composeRule.onNodeWithText(offlineMode).performClick()
        assertEquals(listOf(true), offlineModeChanges)
    }

    @Test
    @Config(qualifiers = "h1000dp")
    fun `the stats total the library below its books`() {
        showLibrary(
            HomeUiState(
                audiobooks = listOf(book("dune", "Dune", "Frank Herbert")),
                ebooks = listOf(book("manual", "Field Manual", "Anon", hasAudio = false)),
                authorCount = 7,
                durationHours = 12.5,
            ).withLibrary(),
        )
        assertTrue(boundsOf(composeRule.activity.getString(R.string.library_stats)).top >= boundsOf("Field Manual").bottom)
        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("7").assertExists()
        composeRule.onNodeWithText("12.5").assertExists()
    }

    @Test
    fun `an empty library shows no stats`() {
        showLibrary(HomeUiState().withLibrary())
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.library_stats)).assertDoesNotExist()
    }

    @Test
    fun `the library pill is as tall as the settings button`() {
        showLibrary(HomeUiState(audiobooks = listOf(book("dune", "Dune", "Frank Herbert"))).withLibrary())
        val settings = settingsButtonBounds()
        composeRule.onNodeWithText("Shelf").assertHeightIsEqualTo(settings.bottom - settings.top)
    }

    private fun settingsButtonBounds() =
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.settings_icon_desc))
            .getUnclippedBoundsInRoot()

    private fun boundsOf(text: String) = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot()

    private fun plural(
        id: Int,
        count: Int,
    ): String = composeRule.activity.resources.getQuantityString(id, count, count)

    private fun syncedHoursAgo(hours: Int): HomeUiState =
        HomeUiState(
            audiobooks = listOf(book("dune", "Dune", "Frank Herbert")),
            syncStatus = SyncStatus.Success(lastSyncedAtMillis = NOW - hours * HOUR_MILLIS),
        ).withLibrary()

    private fun showLibrary(
        state: HomeUiState,
        onOpenAudiobook: (AudiobookUiModel) -> Unit = {},
        onOfflineModeChange: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAccessToken provides "test-token") {
                LibraryScreen(
                    state = state,
                    isGridView = isGridView,
                    bottomBarPadding = 0.dp,
                    nowMillis = NOW,
                    onRefresh = {},
                    onLibrarySelected = {},
                    onToggleLayout = {},
                    onOfflineModeChange = onOfflineModeChange,
                    onOpenSettings = {},
                    onOpenAudiobook = onOpenAudiobook,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun HomeUiState.withLibrary(): HomeUiState =
        copy(
            libraries = listOf(LibraryUiModel(id = "lib-1", name = "Shelf", isSelected = true)),
            selectedLibraryId = "lib-1",
            selectedLibraryName = "Shelf",
        )

    private fun book(
        id: String,
        title: String,
        author: String,
        hasAudio: Boolean = true,
    ) = AudiobookUiModel(
        id = id,
        libraryId = "lib-1",
        title = title,
        author = author,
        authorNames = listOf(author),
        coverUrl = "",
        durationInSeconds = if (hasAudio) 3_600L else 0L,
        durationLabel = if (hasAudio) "1h" else "0:00",
        progressSeconds = null,
        timeLeftLabel = null,
        hasAudio = hasAudio,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val HOUR_MILLIS = 60 * 60 * 1000L
    }
}
