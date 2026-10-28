package com.rayaurelius.sophinity

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.rayaurelius.sophinity.data.AppSettings
import com.rayaurelius.sophinity.data.NavPosition
import com.rayaurelius.sophinity.data.TabId
import com.rayaurelius.sophinity.data.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots of color themes, backgrounds, toolbar layouts and accessibility settings. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PersonalizationScreenshotTest {
    init { runBlocking { DemoData.seed() } }

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun settings(t: (AppSettings) -> AppSettings) = runBlocking {
        ApplicationProvider.getApplicationContext<SophinityApp>().container.settingsStore.update(t)
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/personalize/$name.png")
    }

    private fun tab(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    ).performClick()

    @Test fun oliveDark() { settings { it.copy(colorTheme = "olive", themeMode = ThemeMode.DARK) }; shot("theme_olive_dark") }

    @Test fun terracottaMarble() { settings { it.copy(colorTheme = "terracotta", background = "marble", backgroundDim = 0.25f) }; shot("theme_terracotta_marble") }

    @Test fun tyrianNightLeftRail() {
        settings { it.copy(colorTheme = "tyrian", themeMode = ThemeMode.DARK, background = "night", backgroundDim = 0.2f, navPosition = NavPosition.LEFT) }
        shot("tyrian_night_left_rail")
    }

    @Test fun delphiAegeanRightRailCompact() {
        settings {
            it.copy(colorTheme = "delphi", background = "aegean", backgroundDim = 0.45f, navPosition = NavPosition.RIGHT,
                navLabels = false, hiddenTabs = setOf(TabId.GOALS))
        }
        shot("delphi_aegean_right_rail")
    }

    @Test fun accessibilityLarge() {
        settings { it.copy(textScale = 1.3f, readableFont = true, boldText = true, highContrast = true) }
        shot("a11y_large_readable_hc")
    }

    @Test fun appearancePage() {
        settings { it.copy(colorTheme = "agora", background = "keys") }
        compose.onNode(hasContentDescription("Settings")).performClick()
        compose.onNode(hasText("Appearance")).performClick()
        shot("appearance_page")
    }

    @Test fun customColor() {
        settings { it.copy(colorTheme = "custom", customSeed = 0xFF00897B.toInt()) }
        tab("Laurels")
        shot("custom_teal_laurels")
    }
}
