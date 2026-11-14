package org.evoionosp.abstrak.presentation.bookdetails.components
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.bookdetails.BookDetailsAction
import org.evoionosp.abstrak.presentation.ui.download.DownloadUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookActionsRowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private var hasProgress by mutableStateOf(false)

    @Test
    fun `the button reads start listening until the book has a position to continue from`() {
        val actions = mutableListOf<BookDetailsAction>()
        composeRule.setContent {
            BookActionsRow(hasProgress = hasProgress, download = DownloadUiState(), onAction = { actions += it })
        }
        val start = composeRule.activity.getString(R.string.start_listening)
        val resume = composeRule.activity.getString(R.string.continue_listening)
        composeRule.onNodeWithText(resume).assertDoesNotExist()
        composeRule.onNodeWithText(start).performClick()
        assertEquals(listOf(BookDetailsAction.Play), actions)
        hasProgress = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText(start).assertDoesNotExist()
        composeRule.onNodeWithText(resume).performClick()
        assertEquals(listOf(BookDetailsAction.Play, BookDetailsAction.Play), actions)
    }
}
