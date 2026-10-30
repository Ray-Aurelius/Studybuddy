package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.db.Note

/**
 * Spaced retrieval practice for notes. A note comes back after growing gaps; each time it's
 * recalled the gap widens, and a miss starts it over from tomorrow. Trying to remember (before
 * seeing the answer) is what strengthens memory, more than re-reading.
 */
object Review {
    /** Days until the next review after 0, 1, 2… successful recalls in a row. */
    val INTERVALS = listOf(1, 3, 7, 14, 30, 90, 180)

    /** Turn reviews on for a note: first comes back tomorrow. */
    fun start(note: Note, today: Long): Note = note.copy(reviewStage = 0, reviewDueDay = today + INTERVALS[0])

    fun stop(note: Note): Note = note.copy(reviewStage = null, reviewDueDay = null)

    /** Remembered: wait longer next time. */
    fun remembered(note: Note, today: Long): Note {
        val next = ((note.reviewStage ?: 0) + 1).coerceAtMost(INTERVALS.lastIndex)
        return note.copy(reviewStage = next, reviewDueDay = today + INTERVALS[next], reviewCount = note.reviewCount + 1)
    }

    /** Not yet: back tomorrow, starting over. */
    fun again(note: Note, today: Long): Note =
        note.copy(reviewStage = 0, reviewDueDay = today + INTERVALS[0], reviewCount = note.reviewCount + 1)

    /** Notes due today (or overdue), oldest due first. */
    fun due(notes: List<Note>, today: Long): List<Note> =
        notes.filter { it.isDue(today) }.sortedWith(compareBy({ it.reviewDueDay }, { it.updatedAt }))

    /** The earliest review still to come, after today. */
    fun nextDueDay(notes: List<Note>, today: Long): Long? = notes.mapNotNull { it.reviewDueDay }.filter { it > today }.minOrNull()

    /**
     * What to show first and what to recall. With a title, the title is the cue and the text is the
     * answer. Without one, the first line is the cue, unless that's all there is; then the cue is
     * null (the screen asks about the linked item or the date) and the whole note is the answer.
     */
    fun cueAndAnswer(note: Note): Pair<String?, String> {
        val body = note.body.trim()
        if (note.title.isNotBlank()) return note.title.trim() to body
        val lines = body.lines()
        val rest = lines.drop(1).joinToString("\n").trim()
        return if (lines.size > 1 && rest.isNotEmpty()) lines.first().trim() to rest else null to body
    }
}
