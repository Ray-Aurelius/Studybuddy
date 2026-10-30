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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
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
        tab("Notes")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("note")).fetchSemanticsNodes().isNotEmpty() }
        shot("22_notes")
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

    @Test fun openingALinkStartsTheTimer() {
        tab("Library")
        compose.onNode(hasContentDescription("Open link on audiobooks.example.com")).performClick()
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        // The timer is saved off the main thread, so wait for it rather than for Compose.
        compose.waitUntil(5_000) { runBlocking { app.container.timer.current() }.isActive }
        val t = runBlocking { app.container.timer.current() }
        org.junit.Assert.assertEquals(com.rayaurelius.sophinity.timer.TimerStatus.RUNNING, t.status)
        org.junit.Assert.assertEquals(audiobookId, t.itemId)
        tab("Today")
        compose.onNode(hasText("Stop & save")).assertExists()
        shot("26_timer_from_link")
    }

    @Test fun linkTimerCanBeTurnedOff() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        runBlocking { app.container.settingsStore.update { it.copy(linkStartsTimer = false) } }
        tab("Library")
        compose.onNode(hasContentDescription("Open link on audiobooks.example.com")).performClick()
        compose.waitForIdle()
        Thread.sleep(500)
        org.junit.Assert.assertFalse(runBlocking { app.container.timer.current() }.isActive)
    }

    @Test fun notesLinkToLibraryItems() {
        tab("Notes")
        // Notes load from the database off the main thread; wait for the list.
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("note")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Questions to try on myself"), useUnmergedTree = true).assertExists()
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
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("item_note")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Questions to try on myself"), useUnmergedTree = true).assertExists()
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

    @Test fun swipingAwayAHalfFilledItemAsksFirst() {
        compose.onNode(hasText("Add to library")).performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction() and hasText("Title")).performTextInput("Meditations")
        // A fast drag down from the top of the form is what used to close the sheet and lose everything.
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Title"))).performTouchInput {
            swipeDown(startY = top + 20f, endY = bottom, durationMillis = 120)
        }
        compose.waitForIdle()
        compose.onNode(hasText("Discard your changes?")).assertExists()
        shot("27_discard_changes")
        compose.onNode(hasText("Keep editing")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Discard your changes?")).assertDoesNotExist()
        compose.onNode(hasText("Meditations")).assertExists()
    }

    @Test fun pastingALinkFillsInTheDetails() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        app.container.linkFetcher = object : com.rayaurelius.sophinity.data.LinkFetcher({ null }) {
            override suspend fun lookup(url: String) = com.rayaurelius.sophinity.domain.LinkDetails(
                title = "How Memory Works", creator = "Brain Lab", type = com.rayaurelius.sophinity.data.db.ItemType.VIDEO, durationSec = 3_750,
            )
        }
        compose.onNode(hasText("Add to library")).performClick()
        compose.waitForIdle()
        compose.onNode(hasTestTag("link_field")).performTextInput("youtube.com/watch?v=abc")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Details filled in from youtube.com")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("How Memory Works")).assertExists()
        compose.onNode(hasText("Brain Lab")).assertExists()
        compose.onNode(hasText("Video")).assertExists()
        shot("28_link_autofill")
        compose.onNode(hasText("Undo")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("How Memory Works")).assertDoesNotExist()
        compose.onNode(hasText("Book")).assertExists()
    }

    @Test fun sharingALinkWithAStartTimeMovesYourPlace() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        // As YouTube's Share → "Start at" would send it: 6 hours in.
        app.container.pendingShare.value = "https://audiobooks.example.com/habits-of-curious-people?t=21600"
        compose.waitUntil(5_000) { runBlocking { app.container.timer.current() }.isActive }
        val t = runBlocking { app.container.timer.current() }
        org.junit.Assert.assertEquals(audiobookId, t.itemId)
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("6:00:0", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        // Logged study time is unchanged: only the earlier part moved.
        val item = runBlocking { app.container.repo.snapshot.first().itemsById[audiobookId]!! }
        org.junit.Assert.assertEquals(21600L - 3 * 40 * 60 * 3 / 2, item.priorSec)
        shot("29_shared_start_time")
    }

    @Test fun imAtSetsThePlaceWhileTiming() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        runBlocking { app.container.timer.start(com.rayaurelius.sophinity.timer.TimerMode.STOPWATCH, null, audiobookId) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNode(hasText("I'm at…")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Where are you in it?")).assertExists()
        compose.onNode(hasSetTextAction() and hasText("Hours")).performTextReplacement("7")
        compose.onNode(hasSetTextAction() and hasText("Minutes")).performTextReplacement("15")
        compose.onNode(hasSetTextAction() and hasText("Seconds")).performTextReplacement("16")
        compose.onNode(hasText("7:15:16 of 9:30:00")).assertExists()
        shot("30_im_at_sheet")
        compose.onNode(hasText("OK")).performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("7:15:1", substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun sharingANewLinkOffersToAddIt() {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        app.container.linkFetcher = com.rayaurelius.sophinity.data.LinkFetcher { null }
        app.container.pendingShare.value = "https://vimeo.com/987654#t=90"
        // The share is handled off the main thread; give it a moment, then the editor is open.
        Thread.sleep(1_500); compose.waitForIdle()
        compose.onNode(hasTestTag("link_field")).assertTextContains("vimeo.com/987654", substring = true)
        shot("31_share_new_link")
        compose.onNode(hasSetTextAction() and hasText("Title")).performTextInput("A talk")
        compose.onNode(hasText("Save")).performScrollTo().performClick()
        Thread.sleep(1_000); compose.waitForIdle()
        org.junit.Assert.assertTrue(runBlocking { app.container.timer.current() }.isActive)
        val item = runBlocking { app.container.repo.snapshot.first().items.first { it.title == "A talk" } }
        org.junit.Assert.assertEquals("https://vimeo.com/987654", item.url)
        // The shared "#t=90" became where they are in the video.
        org.junit.Assert.assertEquals(90L, item.priorSec)
        org.junit.Assert.assertEquals(item.id, runBlocking { app.container.timer.current() }.itemId)
    }

    @Test fun anUntouchedEditorStillSwipesAway() {
        compose.onNode(hasText("Add to library")).performClick()
        compose.waitForIdle()
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Title"))).performTouchInput {
            swipeDown(startY = top + 20f, endY = bottom, durationMillis = 120)
        }
        compose.waitForIdle()
        compose.onNode(hasText("Discard your changes?")).assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("Title")).assertDoesNotExist()
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
