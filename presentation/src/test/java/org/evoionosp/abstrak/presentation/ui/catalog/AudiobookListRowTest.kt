package org.evoionosp.abstrak.presentation.ui.catalog
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.ui.image.LocalAccessToken
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudiobookListRowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `at the default font scale the row fits its content exactly`() {
        showRow(fontScale = 1f)
        assertRowFitsContent()
    }

    @Test
    fun `at twice the font scale the row grows and no line is cut off`() {
        showRow(fontScale = 2f)
        assertRowFitsContent()
        composeRule
            .onNode(hasClickAction())
            .assertHeightIsAtLeast(AudiobookListRowDefaults.Height + 16.dp + 1.dp)
    }

    private fun assertRowFitsContent() {
        val texts = listOf("Dune", "Frank Herbert", "1h")
        texts.forEach { text ->
            assertFalse(textLayoutResult(text).didOverflowHeight)
        }
        var textHeight = 0.dp
        texts.forEach { text ->
            val node = composeRule.onNodeWithText(text, useUnmergedTree = true)
            val bounds = node.getUnclippedBoundsInRoot()
            textHeight += bounds.bottom - bounds.top
        }
        composeRule.onNode(hasClickAction()).assertHeightIsEqualTo(
            maxOf(AudiobookListRowDefaults.Height, textHeight) + 16.dp,
        )
    }

    private fun showRow(fontScale: Float) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAccessToken provides "test-token",
                LocalDensity provides Density(LocalDensity.current.density, fontScale = fontScale),
            ) {
                AudiobookListRow(
                    audiobook =
                        AudiobookUiModel(
                            id = "dune",
                            libraryId = "lib-1",
                            title = "Dune",
                            author = "Frank Herbert",
                            authorNames = listOf("Frank Herbert"),
                            coverUrl = "",
                            durationInSeconds = 3_600L,
                            durationLabel = "1h",
                            progressSeconds = null,
                            timeLeftLabel = null,
                        ),
                    detail = "1h",
                    onClick = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun textLayoutResult(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.first()
    }
}
