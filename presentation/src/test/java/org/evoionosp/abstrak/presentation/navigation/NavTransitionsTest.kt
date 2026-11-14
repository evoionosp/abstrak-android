package org.evoionosp.abstrak.presentation.navigation
import org.junit.Assert.assertEquals
import org.junit.Test

class NavTransitionsTest {
    private val home = AppRoute.Home.route
    private val library = AppRoute.Library.route
    private val search = AppRoute.Search.route
    private val downloads = AppRoute.Downloads.route
    private val auth = AppRoute.Auth.route
    private val preferences = AppRoute.Preferences.route
    private val appearance = AppRoute.Appearance.route
    private val bookDetails = AppRoute.BookDetails.route

    @Test
    fun `switching root tabs fades through, either way`() {
        listOf(home to library, library to search, search to downloads, downloads to home).forEach { (from, to) ->
            assertEquals(NavMotion.FADE_THROUGH, navMotion(from, to, isPop = false))
            assertEquals(NavMotion.FADE_THROUGH, navMotion(from, to, isPop = true))
        }
    }

    @Test
    fun `opening a page from any tab goes forward and back pops backward`() {
        listOf(home, library, search, downloads).forEach { tab ->
            assertEquals(NavMotion.SHARED_AXIS_FORWARD, navMotion(tab, preferences, isPop = false))
            assertEquals(NavMotion.SHARED_AXIS_BACKWARD, navMotion(preferences, tab, isPop = true))
            assertEquals(NavMotion.SHARED_AXIS_FORWARD, navMotion(tab, bookDetails, isPop = false))
            assertEquals(NavMotion.SHARED_AXIS_BACKWARD, navMotion(bookDetails, tab, isPop = true))
        }
    }

    @Test
    fun `page to page goes forward and back pops backward`() {
        assertEquals(NavMotion.SHARED_AXIS_FORWARD, navMotion(preferences, appearance, isPop = false))
        assertEquals(NavMotion.SHARED_AXIS_BACKWARD, navMotion(appearance, preferences, isPop = true))
    }

    @Test
    fun `logging out goes back to login though it navigates`() {
        assertEquals(NavMotion.SHARED_AXIS_BACKWARD, navMotion(preferences, auth, isPop = false))
        assertEquals(NavMotion.SHARED_AXIS_BACKWARD, navMotion(home, auth, isPop = false))
    }

    @Test
    fun `pages opened from login go forward and back to it`() {
        assertEquals(NavMotion.SHARED_AXIS_FORWARD, navMotion(auth, preferences, isPop = false))
        assertEquals(NavMotion.SHARED_AXIS_BACKWARD, navMotion(preferences, auth, isPop = true))
    }
}
