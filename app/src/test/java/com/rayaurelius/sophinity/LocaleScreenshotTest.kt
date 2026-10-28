package com.rayaurelius.sophinity

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.rayaurelius.sophinity.data.NavPosition
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Translated screens: a right-to-left language, a long-word language, CJK, and the
 * languages that switch heading fonts. Tabs are found by their translated labels, so
 * a missing translation shows up as a test failure, not only as an English word in a picture.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class LocaleScreenshotTest {
    init { runBlocking { DemoData.seed() } }

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/locales/$name.png")
    }

    private fun tab(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    ).performClick()

    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi")
    fun arabicToday() = shot("ar_today")

    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi")
    fun arabicLaurels() { tab("الأكاليل"); shot("ar_laurels") }

    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi")
    fun arabicLeftRail() {
        runBlocking {
            ApplicationProvider.getApplicationContext<SophinityApp>().container.settingsStore.update { it.copy(navPosition = NavPosition.LEFT) }
        }
        tab("الإحصاءات")
        shot("ar_stats_left_rail")
    }

    @Test @Config(qualifiers = "de-w411dp-h891dp-xxhdpi")
    fun germanToday() = shot("de_today")

    @Test @Config(qualifiers = "de-w411dp-h891dp-xxhdpi")
    fun germanStats() { tab("Statistik"); shot("de_stats") }

    @Test @Config(qualifiers = "ja-w411dp-h891dp-xxhdpi")
    fun japaneseToday() = shot("ja_today")

    @Test @Config(qualifiers = "ja-w411dp-h891dp-xxhdpi")
    fun japaneseLibrary() { tab("ライブラリ"); shot("ja_library") }

    @Test @Config(qualifiers = "el-w411dp-h891dp-xxhdpi")
    fun greekLaurels() { tab("Δάφνες"); shot("el_laurels") }

    @Test @Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
    fun russianGoals() { tab("Цели"); shot("ru_goals") }

    @Test @Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
    fun vietnameseToday() = shot("vi_today")

    @Test @Config(qualifiers = "es-w411dp-h891dp-xxhdpi")
    fun spanishLaurels() { tab("Laureles"); shot("es_laurels") }
}
