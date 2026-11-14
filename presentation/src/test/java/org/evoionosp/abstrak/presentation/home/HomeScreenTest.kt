package org.evoionosp.abstrak.presentation.home
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.evoionosp.abstrak.presentation.ui.image.LocalAccessToken
import org.evoionosp.abstrak.presentation.ui.message.LocalSnackbarController
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.evoionosp.abstrak.presentation.ui.model.LibraryUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a long name keeps the greeting on one ellipsized line`() {
        val name = "Maximilian".repeat(6)
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(username = name),
                bottomBarPadding = 0.dp,
                onRefresh = {},
                onOpenSettings = {},
                onOpenAudiobook = {},
                onPlayAudiobook = {},
            )
        }
        val greeting = textLayoutResult("Hi, $name")
        assertEquals(1, greeting.lineCount)
        assertTrue(greeting.isLineEllipsized(0))
    }

    @Test
    fun `a Continue Listening book plays and opens the full player`() {
        val book = continueListeningBook(title = "Dune Messiah")
        val viewModel = mockk<HomeViewModel>(relaxed = true)
        every { viewModel.uiState } returns
            MutableStateFlow(
                HomeUiState(
                    libraries = listOf(LibraryUiModel(id = "lib-1", name = "Shelf", isSelected = true)),
                    continueListening = listOf(book),
                ),
            )
        every { viewModel.events } returns MutableSharedFlow()
        var playerOpened = 0
        composeRule.setContent {
            CompositionLocalProvider(
                LocalSnackbarController provides SnackbarController(),
                LocalAccessToken provides "",
            ) {
                HomeRoute(
                    onOpenSettings = {},
                    onSessionExpired = {},
                    onOpenAudiobook = {},
                    onOpenPlayer = { playerOpened++ },
                    bottomBarPadding = 0.dp,
                    viewModel = viewModel,
                )
            }
        }
        composeRule.onNodeWithText("Dune Messiah").performClick()
        verify { viewModel.playAudiobook(book) }
        assertEquals(1, playerOpened)
    }

    private fun continueListeningBook(title: String) =
        AudiobookUiModel(
            id = "book-1",
            libraryId = "lib-1",
            title = title,
            author = "Frank Herbert",
            authorNames = listOf("Frank Herbert"),
            coverUrl = "",
            durationInSeconds = 3_600L,
            durationLabel = "1h",
            progressSeconds = 600.0,
            timeLeftLabel = "50m left",
        )

    private fun textLayoutResult(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.first()
    }
}
