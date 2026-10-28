package com.rayaurelius.studybuddy.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Kinds of learning material a person can log outside formal schooling. */
@Serializable
enum class ItemType(val label: String, val plural: String, val emoji: String, val unitLabel: String) {
    BOOK("Book", "Books", "📚", "pages"),
    PODCAST("Podcast", "Podcasts", "🎧", "episodes"),
    SEMINAR("Seminar", "Seminars", "🎤", "sessions"),
    COURSE("Course", "Courses", "🎓", "modules"),
    VIDEO("Video", "Videos", "🎬", "videos"),
    ARTICLE("Article", "Articles", "📰", "parts"),
}

@Serializable
enum class ItemStatus(val label: String) { WANT("Want to"), ACTIVE("In progress"), DONE("Finished") }

@Serializable
enum class SessionSource { TIMER, MANUAL }

@Serializable
enum class GoalKind { TIME, ITEMS }

@Serializable
enum class GoalPeriod(val label: String) {
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), BY_DATE("By a date")
}

@Serializable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val emoji: String = "📘",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "sessions",
    indices = [Index("startedAt"), Index("subjectId"), Index("itemId")],
)
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val itemId: Long? = null,
    /** Wall-clock epoch millis the session began. */
    val startedAt: Long,
    val durationSec: Long,
    /** Short free-recall reflection: "one thing I learned". */
    val note: String = "",
    /** Self-rated focus, 1 (scattered) to 5 (deep flow). Null if skipped. */
    val focus: Int? = null,
    val source: SessionSource = SessionSource.TIMER,
)

@Serializable
@Entity(tableName = "items", indices = [Index("status"), Index("type")])
data class LearningItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ItemType,
    val title: String,
    val creator: String = "",
    val status: ItemStatus = ItemStatus.ACTIVE,
    val subjectId: Long? = null,
    val totalUnits: Int? = null,
    val progressUnits: Int = 0,
    val rating: Int? = null,
    val takeaway: String = "",
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
)

@Serializable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val kind: GoalKind,
    val period: GoalPeriod,
    /** Minutes for TIME goals, item count for ITEMS goals. */
    val target: Int,
    val itemType: ItemType? = null,
    val subjectId: Long? = null,
    /** Epoch day (LocalDate.toEpochDay) the goal is due, for BY_DATE goals. */
    val deadlineEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

@Serializable
@Entity(tableName = "milestones")
data class UnlockedMilestone(
    @PrimaryKey val key: String,
    val unlockedAt: Long,
    val seen: Boolean = false,
)
