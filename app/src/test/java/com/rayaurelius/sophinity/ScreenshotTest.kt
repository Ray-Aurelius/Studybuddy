package com.rayaurelius.sophinity

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
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
        tab("Sessions"); shot("8_sessions")
        tab("Notes"); shot("22_notes")
    }

    @Test fun pausedTimerFitsWithLargeText() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        runBlocking {
            app.container.settingsStore.update { it.copy(textScale = 1.5f) }
            app.container.timer.start(com.rayaurelius.sophinity.timer.TimerMode.STOPWATCH, null, audiobookId)
            app.container.timer.pause()
        }
        compose.mainClock.advanceTimeBy(1_000)
        // Resume is an icon button now, so its label can never be clipped.
        compose.onNode(hasContentDescription("Resume")).assertExists()
        compose.onNode(hasText("Stop & save")).assertExists()
        shot("23_paused_timer_large_text")
    }

    @Test fun notesLinkToLibraryItems() {
        tab("Notes")
        compose.onNode(hasText("Questions to try on myself")).assertExists()
        compose.onAllNodes(hasTestTag("note"))[0].performClick()
        compose.waitForIdle()
        compose.onNode(hasTestTag("note_body")).assertExists()
        shot("24_note_editor")
    }

    @Test fun libraryItemShowsItsNotes() {
        tab("Library")
        compose.onNode(hasText("Habits of Curious People"), useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Run time"))).performScrollToNode(hasText("Write a note"))
        compose.onNode(hasText("Questions to try on myself")).assertExists()
        shot("25_item_notes")
    }

    @Test fun deleteAsksFirst() {
        tab("Sessions")
        compose.onAllNodes(hasContentDescription("Delete session"))[0].performClick()
        shot("9_sessions_delete_confirm")
        // Cancelling keeps the session; the dialog's wording names the session being deleted.
        compose.onNode(hasText("Cancel")).performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasContentDescription("Delete session"))[0].assertExists()
    }

    @Test fun sessionEditorLinksToLibrary() {
        tab("Sessions")
        compose.onAllNodes(hasTestTag("session"))[0].performClick()
        compose.onNode(hasText("Library item")).assertExists()
        shot("11_session_editor")
    }

    @Test fun timerButtonsWithLargeText() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        runBlocking {
            app.container.settingsStore.update { it.copy(textScale = 1.3f) }
            app.container.timer.start(com.rayaurelius.sophinity.timer.TimerMode.STOPWATCH, null, null)
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNode(hasText("Stop & save")).assertExists()
        shot("12_timer_large_text")
    }

    @Test fun audiobookEditorHasNoDoneCount() {
        tab("Library")
        compose.onNode(hasText("Habits of Curious People"), useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Run time"))).performScrollToNode(hasText("Total chapters"))
        shot("13_audiobook_editor")
    }

    @Config(qualifiers = "w360dp-h780dp-night-xxhdpi")
    @Test fun narrowPhoneToolbar() {
        tab("Sessions")
        shot("14_narrow_toolbar")
    }

    @Test fun settingsFromAnyPageAndBackups() {
        tab("Goals")
        compose.onNode(hasContentDescription("Settings")).performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Automatic daily backups"))
        shot("15_backups")
    }

    @Test fun addMediaFromTimer() {
        compose.onNode(hasText("Add to library")).performClick()
        compose.onNode(hasText("Title")).assertExists()
        shot("17_add_media_from_timer")
    }

    @Test fun libraryFilterIsADropDown() {
        tab("Library")
        compose.onNode(hasText("Show")).performClick()
        compose.onNode(hasText("Audiobooks")).assertExists()
        shot("18_library_dropdown_open")
    }

    @Test fun bubblesCanComeBack() {
        runBlocking {
            ApplicationProvider.getApplicationContext<SophinityApp>().container.settingsStore
                .update { it.copy(choiceStyle = com.rayaurelius.sophinity.data.ChoiceStyle.BUBBLES) }
        }
        tab("Library")
        compose.onNode(hasText("Habits of Curious People"), useUnmergedTree = true).performClick()
        compose.waitForIdle()
        shot("19_editor_bubbles")
    }

    @Test fun startStopwatchFromAGivenTime() {
        compose.onNode(hasText("Started earlier? Set a start time")).performClick()
        compose.onNode(hasText("Start the stopwatch at")).assertExists()
        shot("20_head_start_sheet")
        compose.onNode(hasText("45m")).performClick()
        compose.onNode(hasText("OK")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("counts up from 45m")).assertExists()
        shot("21_head_start_set")
    }

    @Test fun laurelMedallions() {
        tab("Laurels")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Consistency"))
        shot("10_laurel_medallions")
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
