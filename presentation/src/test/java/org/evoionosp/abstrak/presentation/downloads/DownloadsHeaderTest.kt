package org.evoionosp.abstrak.presentation.downloads
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.downloads.components.DownloadsHeader
import org.evoionosp.abstrak.presentation.ui.format.formatStorageSize
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadsHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the title is a heading and no size text shows`() {
        showHeader(selectedCount = 0)
        val title = composeRule.activity.getString(R.string.downloads_title)
        composeRule.onNodeWithText(title).assert(isHeading())
        composeRule.onNodeWithText(formatStorageSize(TOTAL_BYTES)).assertDoesNotExist()
        composeRule.onNodeWithText(formatStorageSize(0L)).assertDoesNotExist()
    }

    @Test
    fun `the title is vertically centered with the settings button`() {
        showHeader(selectedCount = 0)
        val density = composeRule.density
        val title = composeRule.activity.getString(R.string.downloads_title)
        val titleNode = composeRule.onNodeWithText(title)
        val titleTop = titleNode.getUnclippedBoundsInRoot().top
        val titleTopPx = with(density) { titleTop.toPx() }
        val layoutResult = textLayoutResult(title)
        val glyphCenterPx =
            titleTopPx + (layoutResult.getLineTop(0) + layoutResult.getLineBottom(0)) / 2
        val buttonNode =
            composeRule.onNodeWithContentDescription(
                composeRule.activity.getString(R.string.settings_icon_desc),
            )
        val buttonBounds = buttonNode.getUnclippedBoundsInRoot()
        val buttonCenter = buttonBounds.top + (buttonBounds.bottom - buttonBounds.top) / 2
        val buttonCenterPx = with(density) { buttonCenter.toPx() }
        assertTrue(
            "Title glyphs centered with the settings button",
            abs(glyphCenterPx - buttonCenterPx) <= with(density) { 1.dp.toPx() },
        )
    }

    @Test
    fun `selection mode covers the header`() {
        showHeader(selectedCount = 2)
        val title = composeRule.activity.getString(R.string.downloads_title)
        composeRule.onNodeWithText(title).assertDoesNotExist()
        val count =
            composeRule.activity.resources.getQuantityString(
                R.plurals.downloads_selected_count,
                2,
                2,
            )
        composeRule.onNodeWithText(count).assertIsDisplayed()
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

    private fun showHeader(selectedCount: Int) {
        composeRule.setContent {
            DownloadsHeader(
                selectedCount = selectedCount,
                onOpenSettings = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
            )
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val TOTAL_BYTES = 1_073_741_824L
    }
}
