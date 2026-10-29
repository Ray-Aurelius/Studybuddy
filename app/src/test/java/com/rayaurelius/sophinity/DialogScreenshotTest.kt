package com.rayaurelius.sophinity

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.rayaurelius.sophinity.ui.theme.SophinityTheme
import com.rayaurelius.sophinity.ui.components.LibraryItemPicker
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Dialogs rendered on their own, with demo data. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class DialogScreenshotTest {
    @get:Rule val compose = createComposeRule()

    /*
     * The "Log a past session" dialog itself can't be captured here: text fields inside an
     * AlertDialog never report idle under Robolectric. Its library link is this shared picker,
     * so the picker is checked on its own, opened, with demo items.
     */
    @Test fun libraryItemPickerListsTheLibrary() {
        val snap = runBlocking {
            DemoData.seed()
            ApplicationProvider.getApplicationContext<SophinityApp>().container.repo.snapshot.first()
        }
        compose.setContent {
            SophinityTheme {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp)) {
                    LibraryItemPicker(snap, null) {}
                }
            }
        }
        compose.onNode(hasText("Not linked to the library")).performClick()
        compose.onNode(hasText("Habits of Curious People")).assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/16_library_item_picker.png")
    }
}
