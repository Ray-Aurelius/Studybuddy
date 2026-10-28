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
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.GoalKind
import com.rayaurelius.sophinity.data.db.GoalPeriod
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.data.db.Subject
import com.rayaurelius.sophinity.domain.Milestones
import com.rayaurelius.sophinity.ui.theme.Brand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Renders real app screens with seeded demo data to app/build/screenshots/.
 * Run: ./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    private var audiobookId = 0L

    init { seed() }

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun seed() = runBlocking { audiobookId = DemoData.seed() }

    private fun tab(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    ).performClick()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun screens() {
        shot("1_today")
        tab("Stats"); shot("2_stats")
        tab("Library"); shot("3_library")
        tab("Goals"); shot("4_goals")
        tab("Laurels"); shot("5_laurels")
    }

    @Test fun timerOnAudiobook() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        runBlocking { app.container.timer.start(com.rayaurelius.sophinity.timer.TimerMode.STOPWATCH, null, audiobookId) }
        compose.mainClock.advanceTimeBy(1_000)
        shot("7_timer_audiobook")
    }

    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    @Test fun darkToday() {
        shot("6_today_dark")
    }
}
