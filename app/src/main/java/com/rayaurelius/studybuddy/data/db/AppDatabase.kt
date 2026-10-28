package com.rayaurelius.studybuddy.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY archived, name COLLATE NOCASE")
    fun observeAll(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects")
    suspend fun getAll(): List<Subject>

    @Insert suspend fun insert(subject: Subject): Long
    @Update suspend fun update(subject: Subject)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Subject>)
    @Query("DELETE FROM subjects") suspend fun clear()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<StudySession>>

    @Query("SELECT * FROM sessions")
    suspend fun getAll(): List<StudySession>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): StudySession?

    @Query("SELECT COALESCE(SUM(durationSec), 0) FROM sessions WHERE startedAt >= :from AND startedAt < :to")
    suspend fun secondsBetween(from: Long, to: Long): Long

    @Insert suspend fun insert(session: StudySession): Long
    @Update suspend fun update(session: StudySession)
    @Delete suspend fun delete(session: StudySession)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<StudySession>)
    @Query("DELETE FROM sessions") suspend fun clear()
    @Query("UPDATE sessions SET itemId = NULL WHERE itemId = :itemId") suspend fun detachItem(itemId: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CustomCategory>

    @Insert suspend fun insert(c: CustomCategory): Long
    @Update suspend fun update(c: CustomCategory)
    @Delete suspend fun delete(c: CustomCategory)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<CustomCategory>)
    @Query("DELETE FROM categories") suspend fun clear()

    // When a category is deleted its items and goals fall back to the generic "Other" bucket.
    @Query("UPDATE items SET categoryId = NULL WHERE categoryId = :id") suspend fun detachItems(id: Long)
    @Query("UPDATE goals SET categoryId = NULL WHERE categoryId = :id") suspend fun detachGoals(id: Long)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'WANT' THEN 1 ELSE 2 END, COALESCE(completedAt, addedAt) DESC")
    fun observeAll(): Flow<List<LearningItem>>

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<LearningItem>

    @Insert suspend fun insert(item: LearningItem): Long
    @Update suspend fun update(item: LearningItem)
    @Delete suspend fun delete(item: LearningItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<LearningItem>)
    @Query("DELETE FROM items") suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY createdAt")
    fun observeActive(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<Goal>

    @Insert suspend fun insert(goal: Goal): Long
    @Update suspend fun update(goal: Goal)
    @Delete suspend fun delete(goal: Goal)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Goal>)
    @Query("DELETE FROM goals") suspend fun clear()
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones")
    fun observeAll(): Flow<List<UnlockedMilestone>>

    @Query("SELECT * FROM milestones")
    suspend fun getAll(): List<UnlockedMilestone>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(list: List<UnlockedMilestone>)

    @Query("UPDATE milestones SET seen = 1 WHERE `key` IN (:keys)")
    suspend fun markSeen(keys: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<UnlockedMilestone>)
    @Query("DELETE FROM milestones") suspend fun clear()
}

@Database(
    entities = [
        Subject::class, StudySession::class, LearningItem::class, Goal::class, UnlockedMilestone::class,
        CustomCategory::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: custom library categories (new table + categoryId on items and goals).
        AutoMigration(from = 1, to = 2),
        // v3: media run time, prior progress and playback speed on library items.
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjects(): SubjectDao
    abstract fun categories(): CategoryDao
    abstract fun sessions(): SessionDao
    abstract fun items(): ItemDao
    abstract fun goals(): GoalDao
    abstract fun milestones(): MilestoneDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "studybuddy.db").build()
    }
}
