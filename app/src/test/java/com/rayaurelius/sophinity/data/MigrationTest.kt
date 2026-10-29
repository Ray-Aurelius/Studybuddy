package com.rayaurelius.sophinity.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rayaurelius.sophinity.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Guards learners' data: everything saved in an older version must survive the upgrade. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun v1ToV2KeepsLibraryGoalsAndSessions() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO subjects (id, name, color, emoji, archived, createdAt) VALUES (1, 'Spanish', 0, '🗣️', 0, 0)")
            execSQL("INSERT INTO items (id, type, title, creator, status, progressUnits, takeaway, notes, addedAt) VALUES (1, 'BOOK', 'Old Book', 'A', 'DONE', 0, '', '', 0)")
            execSQL("INSERT INTO goals (id, title, kind, period, target, itemType, createdAt, archived) VALUES (1, '2 books', 'ITEMS', 'MONTHLY', 2, 'BOOK', 0, 0)")
            execSQL("INSERT INTO sessions (id, subjectId, startedAt, durationSec, note, source) VALUES (1, 1, 1000, 3600, 'hola', 'TIMER')")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true)

        db.query("SELECT title, type, categoryId FROM items").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Old Book", c.getString(0))
            assertEquals("BOOK", c.getString(1))
            assertTrue(c.isNull(2))
        }
        db.query("SELECT itemType, categoryId FROM goals").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("BOOK", c.getString(0))
            assertTrue(c.isNull(1))
        }
        db.query("SELECT COUNT(*) FROM sessions").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        // New table exists and accepts rows.
        db.execSQL("INSERT INTO categories (name, emoji, unitLabel, createdAt) VALUES ('Workshop', '🛠️', 'sessions', 0)")
        db.query("SELECT name FROM categories").use { c -> c.moveToFirst(); assertEquals("Workshop", c.getString(0)) }
        db.close()
    }

    @Test
    fun v2ToV3AddsMediaRuntimeWithSafeDefaults() {
        helper.createDatabase(dbName, 2).apply {
            execSQL("INSERT INTO items (id, type, title, creator, status, progressUnits, takeaway, notes, addedAt) VALUES (1, 'AUDIOBOOK', 'Old Audiobook', '', 'ACTIVE', 3, '', '', 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 3, true)
        db.query("SELECT title, progressUnits, runtimeMin, priorSec, playbackSpeed FROM items").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Old Audiobook", c.getString(0))
            assertEquals(3, c.getInt(1))
            assertTrue(c.isNull(2))
            assertEquals(0L, c.getLong(3))
            assertEquals(1.0f, c.getFloat(4), 0f)
        }
        db.close()
    }

    @Test
    fun v3ToV4AddsAnEmptyLink() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("INSERT INTO items (id, type, title, creator, status, progressUnits, priorSec, playbackSpeed, takeaway, notes, addedAt) VALUES (1, 'SEMINAR', 'Old Seminar', '', 'ACTIVE', 0, 0, 1.0, '', '', 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 4, true)
        db.query("SELECT title, url FROM items").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Old Seminar", c.getString(0))
            assertEquals("", c.getString(1))
        }
        db.close()
    }

    @Test
    fun v1AllTheWayToCurrent() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO items (id, type, title, creator, status, progressUnits, takeaway, notes, addedAt) VALUES (1, 'BOOK', 'B', '', 'DONE', 0, '', '', 0)")
            close()
        }
        helper.runMigrationsAndValidate(dbName, 4, true).use { db ->
            db.query("SELECT COUNT(*) FROM items").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        }
    }
}
