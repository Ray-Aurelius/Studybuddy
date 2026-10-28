package com.rayaurelius.sophinity.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboarded: Boolean = false,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    /** Minutes a day needs for it to count toward the streak. */
    val streakMinutes: Int = 10,
    /** Rest days per week that don't break a streak (self-compassion > guilt). */
    val restDaysPerWeek: Int = 1,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    /** Implementation intention: where the person plans to study. */
    val reminderPlace: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val lastSubjectId: Long? = null,
    /** Highest title rank already celebrated, so each new title is celebrated once. */
    val seenTitleRank: Int = 1,
)

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val weekStart = intPreferencesKey("week_start")
        val streakMinutes = intPreferencesKey("streak_minutes")
        val restDays = intPreferencesKey("rest_days")
        val focus = intPreferencesKey("focus_minutes")
        val brk = intPreferencesKey("break_minutes")
        val remOn = booleanPreferencesKey("reminder_on")
        val remH = intPreferencesKey("reminder_hour")
        val remM = intPreferencesKey("reminder_minute")
        val remPlace = stringPreferencesKey("reminder_place")
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val lastSubject = longPreferencesKey("last_subject")
        val seenTitle = intPreferencesKey("seen_title_rank")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[K.onboarded] = s.onboarded
            prefs[K.weekStart] = s.weekStart.value
            prefs[K.streakMinutes] = s.streakMinutes
            prefs[K.restDays] = s.restDaysPerWeek
            prefs[K.focus] = s.focusMinutes
            prefs[K.brk] = s.breakMinutes
            prefs[K.remOn] = s.reminderEnabled
            prefs[K.remH] = s.reminderHour
            prefs[K.remM] = s.reminderMinute
            prefs[K.remPlace] = s.reminderPlace
            prefs[K.theme] = s.themeMode.name
            prefs[K.dynamic] = s.dynamicColor
            prefs[K.seenTitle] = s.seenTitleRank
            if (s.lastSubjectId != null) prefs[K.lastSubject] = s.lastSubjectId else prefs.remove(K.lastSubject)
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboarded = this[K.onboarded] ?: d.onboarded,
            weekStart = this[K.weekStart]?.let { DayOfWeek.of(it) } ?: d.weekStart,
            streakMinutes = this[K.streakMinutes] ?: d.streakMinutes,
            restDaysPerWeek = this[K.restDays] ?: d.restDaysPerWeek,
            focusMinutes = this[K.focus] ?: d.focusMinutes,
            breakMinutes = this[K.brk] ?: d.breakMinutes,
            reminderEnabled = this[K.remOn] ?: d.reminderEnabled,
            reminderHour = this[K.remH] ?: d.reminderHour,
            reminderMinute = this[K.remM] ?: d.reminderMinute,
            reminderPlace = this[K.remPlace] ?: d.reminderPlace,
            themeMode = this[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            dynamicColor = this[K.dynamic] ?: d.dynamicColor,
            lastSubjectId = this[K.lastSubject],
            seenTitleRank = this[K.seenTitle] ?: d.seenTitleRank,
        )
    }
}
