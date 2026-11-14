package org.evoionosp.abstrak.presentation.ui.message
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

sealed interface UiText {
    data class Plain(
        val value: String,
    ) : UiText

    data class Resource(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Plural(
        @param:PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = listOf(count),
    ) : UiText
}

fun UiText.resolve(resources: Resources): String =
    when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> resources.getString(id, *args.toTypedArray())
        is UiText.Plural -> resources.getQuantityString(id, count, *args.toTypedArray())
    }
