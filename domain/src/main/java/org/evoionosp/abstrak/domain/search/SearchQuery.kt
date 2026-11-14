package org.evoionosp.abstrak.domain.search
import java.text.Normalizer
import java.util.Locale

class SearchQuery private constructor(
    val terms: List<String>,
) {
    val isBlank: Boolean
        get() = terms.isEmpty()

    fun matches(
        title: String,
        author: String,
    ): Boolean = terms.all { term -> term in title || term in author }

    fun matchesTitle(title: String): Boolean = terms.all { term -> term in title }

    companion object {
        private val whitespace = Regex("\\s+")

        fun parse(raw: String): SearchQuery =
            SearchQuery(
                terms = normalizeForSearch(raw).split(whitespace).filter { it.isNotEmpty() },
            )
    }
}

fun normalizeForSearch(text: String): String {
    val lowercase = text.lowercase(Locale.ROOT)
    if (lowercase.all { it.code < ASCII_LIMIT }) return lowercase
    val decomposed = Normalizer.normalize(lowercase, Normalizer.Form.NFD)
    return buildString(decomposed.length) {
        var baseScript: Character.UnicodeScript? = null
        var index = 0
        while (index < decomposed.length) {
            val codePoint = decomposed.codePointAt(index)
            index += Character.charCount(codePoint)
            val folded = foldForSearch(codePoint)
            if (folded != null) {
                append(folded)
                baseScript = Character.UnicodeScript.of(folded.codePointAt(0))
                continue
            }
            val isMark = Character.getType(codePoint) == Character.NON_SPACING_MARK.toInt()
            if (isMark && baseScript in STRIP_MARK_AFTER_SCRIPTS) continue
            appendCodePoint(codePoint)
            if (!isMark) {
                baseScript = Character.UnicodeScript.of(codePoint)
            }
        }
    }
}

private fun foldForSearch(codePoint: Int): String? =
    when (codePoint) {
        'ł'.code -> "l"
        'ø'.code -> "o"
        'ß'.code -> "ss"
        'æ'.code -> "ae"
        'œ'.code -> "oe"
        'đ'.code -> "d"
        'ð'.code -> "d"
        'þ'.code -> "th"
        'ı'.code -> "i"
        '\u2018'.code, '\u2019'.code, '\u02BC'.code -> "'"
        '\u201C'.code, '\u201D'.code -> "\""
        else -> null
    }

private val STRIP_MARK_AFTER_SCRIPTS =
    setOf(
        Character.UnicodeScript.LATIN,
        Character.UnicodeScript.GREEK,
        Character.UnicodeScript.CYRILLIC,
        Character.UnicodeScript.ARABIC,
        Character.UnicodeScript.HEBREW,
    )
private const val ASCII_LIMIT = 128
