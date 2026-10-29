package com.rayaurelius.sophinity.data

import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.Note
import com.rayaurelius.sophinity.ui.notes.NoteFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesTest {
    @Test fun headingFallsBackToFirstLine() {
        assertEquals("Title", Note(title = "Title", body = "x").heading)
        assertEquals("First line", Note(body = "\n  First line\nsecond").heading)
        assertTrue(Note(title = " ", body = "  ").isEmpty)
    }

    @Test fun filterByItemUnlinkedAndSearch() {
        val a = Note(id = 1, title = "Stoics", body = "Marcus", itemId = 10, updatedAt = 3)
        val b = Note(id = 2, body = "Loose idea", updatedAt = 2)
        val c = Note(id = 3, body = "Recursion", itemId = 11, updatedAt = 5)
        val notes = listOf(a, b, c)
        val titles = mapOf(10L to "Meditations", 11L to "Algorithms")
        assertEquals(listOf(3L, 1L, 2L), NoteFilter.apply(notes, null, "") { titles[it] }.map { it.id })
        assertEquals(listOf(1L), NoteFilter.apply(notes, 10L, "") { titles[it] }.map { it.id })
        assertEquals(listOf(2L), NoteFilter.apply(notes, -1L, "") { titles[it] }.map { it.id })
        // Search covers the title, the text and the linked item's title, ignoring case.
        assertEquals(listOf(1L), NoteFilter.apply(notes, null, "marcus") { titles[it] }.map { it.id })
        assertEquals(listOf(3L), NoteFilter.apply(notes, null, " ALGO ") { titles[it] }.map { it.id })
    }

    @Test fun oldBackupsTurnItemNotesIntoLinkedNotes() {
        val old = BackupData(
            version = 4, exportedAt = 0, subjects = emptyList(), sessions = emptyList(), goals = emptyList(), milestones = emptyList(),
            items = listOf(
                LearningItem(id = 5, type = ItemType.BOOK, title = "B", notes = " keep me ", addedAt = 42),
                LearningItem(id = 6, type = ItemType.BOOK, title = "C"),
            ),
        )
        val moved = BackupData.decode(BackupData.encode(old))
        assertEquals(1, moved.notes.size)
        assertEquals("keep me", moved.notes[0].body)
        assertEquals(5L, moved.notes[0].itemId)
        assertEquals(42L, moved.notes[0].createdAt)
        assertTrue(moved.items.all { it.notes.isEmpty() })
        // Running it again doesn't duplicate anything.
        assertEquals(moved, moved.withItemNotesMoved())
    }

    @Test fun notesRoundTripInBackups() {
        val data = BackupData(
            version = BackupData.CURRENT_VERSION, exportedAt = 0, subjects = emptyList(), sessions = emptyList(), goals = emptyList(),
            milestones = emptyList(), items = emptyList(), notes = listOf(Note(id = 1, title = "T", body = "B", itemId = null, createdAt = 1, updatedAt = 2)),
        )
        assertEquals(data, BackupData.decode(BackupData.encode(data)))
    }
}
