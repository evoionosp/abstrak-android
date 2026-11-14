package org.evoionosp.abstrak.domain.search
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryTest {
    @Test
    fun `parse lowercases, strips accents and splits on whitespace`() {
        assertEquals(listOf("les", "miserables"), SearchQuery.parse("  Les   Misérables ").terms)
    }

    @Test
    fun `blank text is a blank query`() {
        assertTrue(SearchQuery.parse("").isBlank)
        assertTrue(SearchQuery.parse(" \t\n ").isBlank)
        assertFalse(SearchQuery.parse("a").isBlank)
    }

    @Test
    fun `every term must appear in the title or the author`() {
        val title = normalizeForSearch("Dune")
        val author = normalizeForSearch("Frank Herbert")
        assertTrue(SearchQuery.parse("dune").matches(title, author))
        assertTrue(SearchQuery.parse("HERBERT").matches(title, author))
        assertTrue(SearchQuery.parse("dune herbert").matches(title, author))
        assertTrue(SearchQuery.parse("her").matches(title, author))
        assertFalse(SearchQuery.parse("dune asimov").matches(title, author))
    }

    @Test
    fun `matchesTitle ignores the author`() {
        val title = normalizeForSearch("Children of Dune")
        assertTrue(SearchQuery.parse("dune children").matchesTitle(title))
        assertFalse(SearchQuery.parse("herbert").matchesTitle(title))
    }

    @Test
    fun `normalizeForSearch folds case and drops combining marks`() {
        assertEquals("plain ascii", normalizeForSearch("Plain ASCII"))
        assertEquals("rene garcia marquez", normalizeForSearch("René García Márquez"))
        assertEquals("munchen", normalizeForSearch("München"))
        assertTrue(SearchQuery.parse("rené").matches(normalizeForSearch("Rene"), ""))
    }

    @Test
    fun `folds letters that decomposition leaves whole`() {
        assertEquals("strasse", normalizeForSearch("STRAẞE"))
        assertEquals("thor", normalizeForSearch("Þór"))
        assertTrue(SearchQuery.parse("lodz").matches(normalizeForSearch("Łódź"), ""))
        assertTrue(SearchQuery.parse("oyvind").matches(normalizeForSearch("Øyvind"), ""))
        assertTrue(SearchQuery.parse("strasse").matches(normalizeForSearch("Straße"), ""))
        assertTrue(SearchQuery.parse("strasse").matches(normalizeForSearch("STRAẞE"), ""))
        assertTrue(SearchQuery.parse("aesop").matches(normalizeForSearch("Æsop"), ""))
        assertTrue(SearchQuery.parse("coeur").matches(normalizeForSearch("Cœur"), ""))
        assertTrue(SearchQuery.parse("dorde").matches(normalizeForSearch("Đorđe"), ""))
        assertTrue(SearchQuery.parse("brodur").matches(normalizeForSearch("Bróður"), ""))
        assertTrue(SearchQuery.parse("thor").matches(normalizeForSearch("Þór"), ""))
        assertTrue(SearchQuery.parse("ispanak").matches(normalizeForSearch("ıspanak"), ""))
    }

    @Test
    fun `curly quotes match their straight forms`() {
        assertTrue(SearchQuery.parse("ender’s").matches(normalizeForSearch("ender's"), ""))
        assertTrue(SearchQuery.parse("ender's").matches(normalizeForSearch("ender’s"), ""))
        assertTrue(SearchQuery.parse("“dune”").matches(normalizeForSearch("“Dune”"), ""))
        assertTrue(SearchQuery.parse("dune").matches(normalizeForSearch("“Dune”"), ""))
    }

    @Test
    fun `strips accents after Greek and Cyrillic letters`() {
        assertTrue(SearchQuery.parse("ελενη").matches(normalizeForSearch("Ελένη"), ""))
        assertTrue(SearchQuery.parse("Ελένη").matches(normalizeForSearch("ελενη"), ""))
        assertTrue(SearchQuery.parse("ёлка").matches(normalizeForSearch("елка"), ""))
        assertTrue(SearchQuery.parse("елка").matches(normalizeForSearch("ёлка"), ""))
        assertTrue(SearchQuery.parse("rene").matches(normalizeForSearch("René"), ""))
    }

    @Test
    fun `strips optional marks after Arabic and Hebrew letters`() {
        assertTrue(SearchQuery.parse("كتاب").matches(normalizeForSearch("كِتَاب"), ""))
        assertTrue(SearchQuery.parse("كِتَاب").matches(normalizeForSearch("كتاب"), ""))
        assertTrue(SearchQuery.parse("اسلام").matches(normalizeForSearch("إسلام"), ""))
        assertTrue(SearchQuery.parse("إسلام").matches(normalizeForSearch("اسلام"), ""))
        assertTrue(SearchQuery.parse("שלום").matches(normalizeForSearch("שָׁלוֹם"), ""))
        assertTrue(SearchQuery.parse("שָׁלוֹם").matches(normalizeForSearch("שלום"), ""))
    }

    @Test
    fun `keeps marks after letters in other scripts`() {
        assertFalse(SearchQuery.parse("कल").matches(normalizeForSearch("कुल"), ""))
        assertFalse(SearchQuery.parse("कुल").matches(normalizeForSearch("कल"), ""))
        assertTrue(SearchQuery.parse("कुल").matches(normalizeForSearch("कुल"), ""))
        assertFalse(SearchQuery.parse("かっこう").matches(normalizeForSearch("がっこう"), ""))
        assertFalse(SearchQuery.parse("がっこう").matches(normalizeForSearch("かっこう"), ""))
        assertTrue(SearchQuery.parse("がっこう").matches(normalizeForSearch("がっこう"), ""))
    }

    @Test
    fun `characters outside the BMP survive normalization`() {
        assertEquals("a\uD83D\uDE00b", normalizeForSearch("A\uD83D\uDE00B"))
        assertTrue(SearchQuery.parse("a\uD83D\uDE00b").matches(normalizeForSearch("A\uD83D\uDE00B"), ""))
    }
}
