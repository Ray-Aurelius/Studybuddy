package com.rayaurelius.sophinity.ui.i18n

import android.content.Context
import android.content.res.Configuration
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.LocaleList
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import com.rayaurelius.sophinity.domain.StudyMath
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The app's language: follows the phone unless the person picks one in Settings. */
object AppLocale {
    /** Languages Sophinity ships translations for (BCP-47 tags), in picker order after English. */
    val supported = listOf(
        "en", "es", "pt-BR", "fr", "de", "it", "nl", "pl", "ru", "uk", "el", "tr",
        "ar", "hi", "id", "vi", "th", "ja", "ko", "zh-CN", "zh-TW",
    )

    /** The chosen tag, or null when following the phone. */
    fun chosenTag(): String? = AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.toLanguageTags()

    fun set(tag: String?) {
        AppCompatDelegate.setApplicationLocales(
            if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    fun current(): Locale =
        AppCompatDelegate.getApplicationLocales().get(0) ?: LocaleListCompat.getAdjustedDefault().get(0) ?: Locale.getDefault()

    /** A language's own name for itself, e.g. "Español", "日本語". */
    fun nativeName(tag: String): String {
        val l = Locale.forLanguageTag(tag)
        return l.getDisplayName(l).replaceFirstChar { it.titlecase(l) }
    }

    /**
     * A context whose resources use the app language. Activities get this automatically; services,
     * notifications and the widget use this so they speak the same language on Android 12 and below.
     */
    fun localized(context: Context): Context {
        val chosen = AppCompatDelegate.getApplicationLocales()
        if (chosen.isEmpty) return context
        val cfg = Configuration(context.resources.configuration)
        cfg.setLocales(LocaleList.forLanguageTags(chosen.toLanguageTags()))
        return context.createConfigurationContext(cfg)
    }
}

object Formats {
    /** Routes StudyMath.formatDuration through ICU so units read naturally in every language. */
    fun install() {
        StudyMath.durationFormatter = { seconds, short -> duration(seconds, short, AppLocale.current()) }
    }

    fun duration(seconds: Long, short: Boolean, locale: Locale): String {
        val fmt = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NARROW)
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 && m == 0L -> fmt.format(Measure(h, MeasureUnit.HOUR))
            h > 0 -> fmt.formatMeasures(Measure(h, MeasureUnit.HOUR), Measure(m, MeasureUnit.MINUTE))
            m > 0 -> fmt.format(Measure(m, MeasureUnit.MINUTE))
            seconds > 0 -> "<" + fmt.format(Measure(1, MeasureUnit.MINUTE))
            else -> fmt.format(Measure(0, MeasureUnit.MINUTE))
        }
    }

    /** A date formatter from a skeleton like "EEEEMMMMd", ordered the way the language expects. */
    fun date(skeleton: String, locale: Locale): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
fun rememberDateFormat(skeleton: String): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, skeleton) { Formats.date(skeleton, locale) }
}
