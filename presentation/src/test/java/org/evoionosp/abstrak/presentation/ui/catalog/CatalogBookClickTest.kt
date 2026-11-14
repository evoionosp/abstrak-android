package org.evoionosp.abstrak.presentation.ui.catalog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.AbstrakSnackbar
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.AbstrakSnackbarVisuals
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.evoionosp.abstrak.presentation.ui.message.LocalSnackbarController
import org.evoionosp.abstrak.presentation.ui.message.ShowSnackbarMessages
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogBookClickTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val hostState = SnackbarHostState()
    private val controller = SnackbarController()
    private val opened = mutableListOf<String>()

    @Before
    fun setUp() {
        composeRule.setContent {
            ShowSnackbarMessages(controller = controller, hostState = hostState)
            CompositionLocalProvider(LocalSnackbarController provides controller) {
                val onClick = rememberCatalogBookClick { opened += it.id }
                Column {
                    TextButton(onClick = { onClick(book("audio", hasAudio = true)) }) { Text("Tap audiobook") }
                    TextButton(onClick = { onClick(book("ebook", hasAudio = false)) }) { Text("Tap ebook") }
                    SnackbarHost(hostState) { AbstrakSnackbar(it) }
                }
            }
        }
    }

    @Test
    fun `an audiobook tap opens the book`() {
        composeRule.onNodeWithText("Tap audiobook").performClick()
        assertEquals(listOf("audio"), opened)
        assertEquals(null, hostState.currentSnackbarData)
    }

    @Test
    fun `an ebook tap explains in a short warning snackbar instead of opening the book`() {
        composeRule.onNodeWithText("Tap ebook").performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.catalog_ebook_not_supported))
            .assertIsDisplayed()
        val visuals = hostState.currentSnackbarData?.visuals as AbstrakSnackbarVisuals
        assertEquals(SnackbarDuration.Short, visuals.duration)
        assertEquals(SnackbarType.WARNING, visuals.type)
        assertTrue(opened.isEmpty())
    }

    private fun book(
        id: String,
        hasAudio: Boolean,
    ) = AudiobookUiModel(
        id = id,
        libraryId = "lib-1",
        title = id,
        author = "Author",
        authorNames = listOf("Author"),
        coverUrl = "",
        durationInSeconds = null,
        durationLabel = "",
        progressSeconds = null,
        timeLeftLabel = null,
        hasAudio = hasAudio,
    )
}
