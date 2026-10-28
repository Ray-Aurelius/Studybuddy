package com.rayaurelius.sophinity.domain

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Text that domain code can describe without a Context: a translatable resource (with optional
 * plural count and arguments) or raw text the learner typed. The UI resolves it in the current
 * language. Arguments may themselves be Txt and are resolved first.
 */
sealed interface Txt {
    data class Raw(val text: String) : Txt
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : Txt
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : Txt
}

fun Txt.resolve(res: Resources): String {
    fun arg(a: Any): Any = when (a) {
        is Txt -> a.resolve(res)
        is LocalDate -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(res.configuration.locales[0]).format(a)
        else -> a
    }
    return when (this) {
        is Txt.Raw -> text
        is Txt.Res -> if (args.isEmpty()) res.getString(id) else res.getString(id, *args.map(::arg).toTypedArray())
        is Txt.Plural -> res.getQuantityString(id, count, *args.map(::arg).toTypedArray())
    }
}

/** Resolves in composition and recomposes when the language changes. */
@Composable
fun Txt.str(): String {
    LocalConfiguration.current
    return resolve(LocalContext.current.resources)
}
