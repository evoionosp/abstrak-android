package org.evoionosp.abstrak.presentation.ui.message
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.evoionosp.abstrak.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UiTextTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `plain text is itself`() {
        assertEquals("Saved", UiText.Plain("Saved").resolve(resources))
    }

    @Test
    fun `a resource resolves with its arguments`() {
        assertEquals(
            resources.getString(R.string.catalog_offline),
            UiText.Resource(R.string.catalog_offline).resolve(resources),
        )
    }

    @Test
    fun `a plural picks the form for its count and formats the count by default`() {
        assertEquals("Deleted 1 download", UiText.Plural(R.plurals.downloads_deleted, 1).resolve(resources))
        assertEquals("Deleted 3 downloads", UiText.Plural(R.plurals.downloads_deleted, 3).resolve(resources))
    }
}
