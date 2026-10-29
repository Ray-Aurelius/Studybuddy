package com.rayaurelius.sophinity.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where the main toolbar lives. Left/right show a vertical rail. */
enum class NavPosition { BOTTOM, LEFT, RIGHT }

/** Tabs that can be hidden from the toolbar. Today is always shown. */
enum class TabId { STATS, SESSIONS, LIBRARY, GOALS, LAURELS }

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
    /** Color theme id from ColorThemes ("aegean", "olive", …, "wallpaper", "custom"). */
    val colorTheme: String = "aegean",
    /** Seed color for the "custom" theme. */
    val customSeed: Int = 0xFF2F9BFF.toInt(),
    // --- Background
    /** "none", a built-in background id, or "photo" for the person's own image. */
    val background: String = "none",
    /** 0 = background at full strength, 0.9 = mostly covered (keeps text readable). */
    val backgroundDim: Float = 0.35f,
    /** Blur radius in dp for photo backgrounds (Android 12+). */
    val backgroundBlur: Float = 0f,
    // --- Accessibility
    /** Multiplies the system font size: 0.85 … 1.6. */
    val textScale: Float = 1f,
    val boldText: Boolean = false,
    val highContrast: Boolean = false,
    /** null = follow the system "remove animations" setting. */
    val reduceMotion: Boolean? = null,
    /** Atkinson Hyperlegible everywhere instead of the decorative serif. */
    val readableFont: Boolean = false,
    // --- Toolbar
    val navPosition: NavPosition = NavPosition.BOTTOM,
    val hiddenTabs: Set<TabId> = emptySet(),
    val navLabels: Boolean = true,
    val lastSubjectId: Long? = null,
    /** Highest title rank already celebrated, so each new title is celebrated once. */
    val seenTitleRank: Int = 1,
    // --- Automatic backups
    val autoBackup: Boolean = false,
    /** Storage Access Framework tree URI of the folder the user picked for automatic backups. */
    val backupFolder: String? = null,
    val lastBackupAt: Long? = null,
    /** True when the most recent automatic backup could not be written (e.g. the folder was removed). */
    val lastBackupFailed: Boolean = false,
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
        val dynamic = booleanPreferencesKey("dynamic_color") // legacy: became colorTheme "wallpaper"
        val colorTheme = stringPreferencesKey("color_theme")
        val customSeed = intPreferencesKey("custom_seed")
        val background = stringPreferencesKey("background")
        val bgDim = floatPreferencesKey("background_dim")
        val bgBlur = floatPreferencesKey("background_blur")
        val textScale = floatPreferencesKey("text_scale")
        val bold = booleanPreferencesKey("bold_text")
        val contrast = booleanPreferencesKey("high_contrast")
        val motion = stringPreferencesKey("reduce_motion")
        val readable = booleanPreferencesKey("readable_font")
        val navPos = stringPreferencesKey("nav_position")
        val hidden = stringSetPreferencesKey("hidden_tabs")
        val navLabels = booleanPreferencesKey("nav_labels")
        val lastSubject = longPreferencesKey("last_subject")
        val seenTitle = intPreferencesKey("seen_title_rank")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val backupFolder = stringPreferencesKey("backup_folder")
        val lastBackup = longPreferencesKey("last_backup_at")
        val backupFailed = booleanPreferencesKey("last_backup_failed")
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
            prefs.remove(K.dynamic)
            prefs[K.colorTheme] = s.colorTheme
            prefs[K.customSeed] = s.customSeed
            prefs[K.background] = s.background
            prefs[K.bgDim] = s.backgroundDim
            prefs[K.bgBlur] = s.backgroundBlur
            prefs[K.textScale] = s.textScale
            prefs[K.bold] = s.boldText
            prefs[K.contrast] = s.highContrast
            prefs[K.motion] = when (s.reduceMotion) { null -> "system"; true -> "on"; false -> "off" }
            prefs[K.readable] = s.readableFont
            prefs[K.navPos] = s.navPosition.name
            prefs[K.hidden] = s.hiddenTabs.map { it.name }.toSet()
            prefs[K.navLabels] = s.navLabels
            prefs[K.seenTitle] = s.seenTitleRank
            if (s.lastSubjectId != null) prefs[K.lastSubject] = s.lastSubjectId else prefs.remove(K.lastSubject)
            prefs[K.autoBackup] = s.autoBackup
            if (s.backupFolder != null) prefs[K.backupFolder] = s.backupFolder else prefs.remove(K.backupFolder)
            if (s.lastBackupAt != null) prefs[K.lastBackup] = s.lastBackupAt else prefs.remove(K.lastBackup)
            prefs[K.backupFailed] = s.lastBackupFailed
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
            colorTheme = this[K.colorTheme] ?: if (this[K.dynamic] == true) "wallpaper" else d.colorTheme,
            customSeed = this[K.customSeed] ?: d.customSeed,
            background = this[K.background] ?: d.background,
            backgroundDim = this[K.bgDim] ?: d.backgroundDim,
            backgroundBlur = this[K.bgBlur] ?: d.backgroundBlur,
            textScale = this[K.textScale] ?: d.textScale,
            boldText = this[K.bold] ?: d.boldText,
            highContrast = this[K.contrast] ?: d.highContrast,
            reduceMotion = when (this[K.motion]) { "on" -> true; "off" -> false; else -> null },
            readableFont = this[K.readable] ?: d.readableFont,
            navPosition = this[K.navPos]?.let { runCatching { NavPosition.valueOf(it) }.getOrNull() } ?: d.navPosition,
            hiddenTabs = this[K.hidden]?.mapNotNull { runCatching { TabId.valueOf(it) }.getOrNull() }?.toSet() ?: d.hiddenTabs,
            navLabels = this[K.navLabels] ?: d.navLabels,
            lastSubjectId = this[K.lastSubject],
            seenTitleRank = this[K.seenTitle] ?: d.seenTitleRank,
            autoBackup = this[K.autoBackup] ?: d.autoBackup,
            backupFolder = this[K.backupFolder],
            lastBackupAt = this[K.lastBackup],
            lastBackupFailed = this[K.backupFailed] ?: d.lastBackupFailed,
        )
    }
}
