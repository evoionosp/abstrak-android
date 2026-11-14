package org.evoionosp.abstrak.presentation.ui.message
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.junit.Assert.assertEquals
import org.junit.Test

class SnackbarControllerTest {
    @Test
    fun `messages arrive in order with their types, even before anyone collects`() =
        runTest {
            val controller = SnackbarController()
            controller.info(R.string.catalog_offline)
            controller.success(UiText.Plain("Saved"))
            controller.warning(R.string.catalog_ebook_not_supported)
            controller.error(UiText.Plural(R.plurals.downloads_delete_failed, 2))
            assertEquals(
                listOf(
                    SnackbarMessage(UiText.Resource(R.string.catalog_offline), SnackbarType.INFO),
                    SnackbarMessage(UiText.Plain("Saved"), SnackbarType.SUCCESS),
                    SnackbarMessage(UiText.Resource(R.string.catalog_ebook_not_supported), SnackbarType.WARNING),
                    SnackbarMessage(UiText.Plural(R.plurals.downloads_delete_failed, 2), SnackbarType.ERROR),
                ),
                controller.messages.take(4).toList(),
            )
        }
}
