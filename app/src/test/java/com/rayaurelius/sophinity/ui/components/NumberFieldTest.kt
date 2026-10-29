package com.rayaurelius.sophinity.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Typing minutes one digit at a time must give that number, not a capped 59. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NumberFieldTest {
    @get:Rule val compose = createComposeRule()

    @Test fun typingThreeThenSevenGivesThirtySeven() {
        var seconds: Long? = null
        compose.setContent {
            var s by remember { mutableStateOf<Long?>(null) }
            DurationFields("Hours", s, { s = it; seconds = it })
        }
        val minutes = compose.onNode(hasText("Minutes"))
        minutes.performClick()
        minutes.performTextInput("3")
        minutes.performTextInput("7")
        compose.waitForIdle()
        assertEquals(37 * 60L, seconds)
    }

    @Test fun typingOverAZeroReplacesIt() {
        var value: Int? = 0
        compose.setContent {
            var v by remember { mutableStateOf<Int?>(0) }
            NumberField("Minutes", v, { v = it?.coerceAtMost(59); value = v })
        }
        val field = compose.onNode(hasText("Minutes"))
        field.performTextReplacement("0")
        field.performTextInput("3")
        field.performTextInput("7")
        compose.waitForIdle()
        assertEquals(37, value)
    }

    /** The reported bug: hours typed first, minutes showed "0", and the cursor landed in front of it. */
    @Test fun minutesAfterHoursGiveTheTypedNumber() {
        var seconds: Long? = null
        compose.setContent {
            var s by remember { mutableStateOf<Long?>(null) }
            DurationFields("Hours", s, { s = it; seconds = it })
        }
        compose.onNode(hasText("Hours")).performTextInput("1")
        compose.waitForIdle()
        val minutes = compose.onNode(hasText("Minutes"))
        minutes.performTextInputSelection(TextRange(0))
        minutes.performTextInput("3")
        minutes.performTextInput("7")
        compose.waitForIdle()
        assertEquals(3600L + 37 * 60L, seconds)
    }

    /** The reported bug: "Log a past session" starts at 30 minutes; typing 37 gave 59. */
    @Test fun typingIntoAPrefilledBoxReplacesIt() {
        var value: Int? = 30
        compose.setContent {
            var v by remember { mutableStateOf<Int?>(30) }
            NumberField("Minutes", v, { v = it?.coerceAtMost(59); value = v })
        }
        val field = compose.onNode(hasText("Minutes"))
        field.performClick()
        compose.mainClock.advanceTimeBy(500)
        field.performTextInput("3")
        field.performTextInput("7")
        compose.waitForIdle()
        assertEquals(37, value)
    }

    @Test fun valuesAboveTheLimitAreStillCapped() {
        var value: Int? = null
        compose.setContent {
            var v by remember { mutableStateOf<Int?>(null) }
            NumberField("Minutes", v, { v = it?.coerceAtMost(59); value = v })
        }
        compose.onNode(hasText("Minutes")).performTextInput("75")
        compose.waitForIdle()
        assertEquals(59, value)
    }
}
