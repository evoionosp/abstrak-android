package org.evoionosp.abstrak.presentation.bookdetails.components
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookHeroHeaderTest {
    @Test
    fun `expanded title block sits its inset above the header bottom whatever its height`() {
        assertEquals(1_000 - 40 - 300, top(collapseFraction = 0f, blockHeight = 300))
        assertEquals(1_000 - 40 - 200, top(collapseFraction = 0f, blockHeight = 200))
    }

    @Test
    fun `collapsed title block centers on the back button`() {
        assertEquals(70 - 70, top(collapseFraction = 1f, blockHeight = 140))
        assertEquals(70 - 50, top(collapseFraction = 1f, blockHeight = 100))
    }

    @Test
    fun `from halfway the block shrinks about its middle`() {
        val twoLines = top(collapseFraction = 0.5f, blockHeight = 240)
        val oneLine = top(collapseFraction = 0.5f, blockHeight = 150)
        assertEquals(twoLines + 240 / 2, oneLine + 150 / 2)
    }

    @Test
    fun `book length rounds to the minute and splits hours from minutes`() {
        assertEquals(12 to 5, bookLength(12 * 3_600L + 5 * 60))
        assertEquals(1 to 0, bookLength(3_599L))
        assertEquals(0 to 45, bookLength(45 * 60L + 29))
        assertEquals(0 to 1, bookLength(30L))
    }

    @Test
    fun `book length under half a minute is unknown`() {
        assertNull(bookLength(29L))
        assertNull(bookLength(0L))
        assertNull(bookLength(-120L))
    }

    private fun top(
        collapseFraction: Float,
        blockHeight: Int,
    ): Int =
        heroTitleTop(
            collapseFraction = collapseFraction,
            containerHeight = 1_000,
            blockHeight = blockHeight,
            bottomInset = 40,
            barCenter = 70,
        )
}
