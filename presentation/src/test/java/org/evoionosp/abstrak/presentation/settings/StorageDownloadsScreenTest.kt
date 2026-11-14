package org.evoionosp.abstrak.presentation.settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import org.evoionosp.abstrak.domain.download.model.StorageUsage
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.ui.format.formatStorageSize
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageDownloadsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val storage = StorageUsage(downloadsBytes = 500, cacheBytes = 40, deviceFreeBytes = 10_000)

    @Test
    fun `not loaded, neither the summary nor the switches exist`() {
        showScreen(isLoaded = false)
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.storage_space_title))
            .assertDoesNotExist()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.storage_wifi_only))
            .assertDoesNotExist()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.storage_auto_delete))
            .assertDoesNotExist()
    }

    @Test
    fun `loaded, the summary and the switches exist`() {
        showScreen(isLoaded = true)
        scrollToManageGroup()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.storage_space_title))
            .assertExists()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.storage_wifi_only))
            .assertExists()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.storage_auto_delete))
            .assertExists()
    }

    @Test
    fun `loaded, the downloads group comes first, then manage storage with the summary and clear all`() {
        showScreen(isLoaded = true)
        scrollToManageGroup()
        val downloadsTitle =
            composeRule.onAllNodesWithText(
                composeRule.activity.getString(R.string.storage_downloads_group),
                useUnmergedTree = true,
            )[0]
        val manageTitle =
            composeRule.onNodeWithText(
                composeRule.activity.getString(R.string.storage_manage_group),
            )
        val summary = composeRule.onNodeWithText(summaryText())
        val clearAll =
            composeRule.onNodeWithText(
                composeRule.activity.getString(R.string.storage_clear_all),
            )
        val downloadsTitleTop = downloadsTitle.getUnclippedBoundsInRoot().top
        val manageTitleTop = manageTitle.getUnclippedBoundsInRoot().top
        val summaryTop = summary.getUnclippedBoundsInRoot().top
        val clearAllTop = clearAll.getUnclippedBoundsInRoot().top
        assertTrue("Downloads group above Manage storage", downloadsTitleTop < manageTitleTop)
        assertTrue("Manage storage above the summary", manageTitleTop < summaryTop)
        assertTrue("Summary above clear-all", summaryTop < clearAllTop)
    }

    @Test
    fun `the page title reads Downloads and Storage`() {
        showScreen(isLoaded = true)
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.preferences_storage_downloads))
            .assertIsDisplayed()
    }

    private fun showScreen(isLoaded: Boolean) {
        composeRule.setContent {
            StorageDownloadsScreen(
                state =
                    StorageDownloadsUiState(
                        isLoaded = isLoaded,
                        downloadWifiOnly = true,
                        autoDeleteFinishedDownloads = true,
                        downloadedBookTitles = listOf("Dune"),
                        storage = storage,
                    ),
                onBackClick = {},
                onDownloadWifiOnlyChange = {},
                onAutoDeleteFinishedDownloadsChange = {},
                onClearAllClick = {},
            )
        }
        composeRule.waitForIdle()
    }

    private fun scrollToManageGroup() {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        composeRule.waitForIdle()
    }

    private fun summaryText(): String =
        composeRule.activity.getString(
            R.string.storage_space_summary,
            formatStorageSize(storage.abstrakBytes),
            formatStorageSize(storage.deviceFreeBytes),
        )
}
