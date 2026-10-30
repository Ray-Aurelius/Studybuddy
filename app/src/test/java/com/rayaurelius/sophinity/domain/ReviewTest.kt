package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.db.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTest {
    private val today = 20_000L

    @Test fun gapsGrowWhenRememberedAndResetWhenMissed() {
        var n = Review.start(Note(title = "Stoics", body = "Control what you can"), today)
        assertEquals(today + 1, n.reviewDueDay)
        var day = today + 1
        val gaps = mutableListOf<Long>()
        repeat(8) { n = Review.remembered(n, day); gaps += n.reviewDueDay!! - day; day = n.reviewDueDay!! }
        assertEquals(listOf(3L, 7, 14, 30, 90, 180, 180, 180), gaps)
        assertEquals(8, n.reviewCount)
        n = Review.again(n, day)
        assertEquals(0, n.reviewStage)
        assertEquals(day + 1, n.reviewDueDay)
        assertEquals(9, n.reviewCount)
        assertFalse(Review.stop(n).isReviewing)
    }

    @Test fun dueListAndNextDay() {
        val a = Note(id = 1, reviewDueDay = today - 2)
        val b = Note(id = 2, reviewDueDay = today)
        val c = Note(id = 3, reviewDueDay = today + 4)
        val d = Note(id = 4)
        assertEquals(listOf(1L, 2L), Review.due(listOf(c, b, d, a), today).map { it.id })
        assertEquals(today + 4, Review.nextDueDay(listOf(a, b, c, d), today))
        assertNull(Review.nextDueDay(listOf(a, d), today))
        assertTrue(a.isDue(today)); assertFalse(c.isDue(today)); assertFalse(d.isDue(today))
    }

    @Test fun cueAndAnswer() {
        assertEquals("Stoics" to "Control what you can", Review.cueAndAnswer(Note(title = "Stoics", body = " Control what you can ")))
        assertEquals("Spacing" to "1, 3, 7 days\nthen a month", Review.cueAndAnswer(Note(body = "Spacing\n1, 3, 7 days\nthen a month")))
        assertEquals(null to "Just one line", Review.cueAndAnswer(Note(body = "Just one line")))
    }
}
