package com.rayaurelius.sophinity.data.db

import com.rayaurelius.sophinity.R
import androidx.annotation.StringRes
import androidx.annotation.PluralsRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Kinds of learning material a person can log outside formal schooling. */
@Serializable
enum class ItemType(
    /** Glyph name (see ui/glyphs). */
    val glyph: String,
    @StringRes val labelRes: Int,
    @StringRes val pluralRes: Int,
    @PluralsRes val countRes: Int,
    @StringRes val unitRes: Int,
) {
    BOOK("scroll", R.string.type_book, R.string.type_books, R.plurals.count_book, R.string.unit_pages),
    AUDIOBOOK("lyre", R.string.type_audiobook, R.string.type_audiobooks, R.plurals.count_audiobook, R.string.unit_chapters),
    PODCAST("masks", R.string.type_podcast, R.string.type_podcasts, R.plurals.count_podcast, R.string.unit_episodes),
    SEMINAR("stoa", R.string.type_seminar, R.string.type_seminars, R.plurals.count_seminar, R.string.unit_sessions),
    COURSE("tablet", R.string.type_course, R.string.type_courses, R.plurals.count_course, R.string.unit_modules),
    VIDEO("theatre", R.string.type_video, R.string.type_videos, R.plurals.count_video, R.string.unit_videos),
    ARTICLE("papyrus", R.string.type_article, R.string.type_articles, R.plurals.count_article, R.string.unit_parts),
    /** A user-defined category; see [LearningItem.categoryId] and [CustomCategory]. */
    CUSTOM("meander", R.string.type_other, R.string.type_other, R.plurals.count_item, R.string.unit_parts),
}

/** A library category the learner defined themselves, e.g. "Workshop" or "Documentary". */
@Serializable
@Entity(tableName = "categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Singular name, e.g. "Workshop". */
    val name: String,
    /** Glyph name; stored in the old "emoji" column so existing databases and backups keep working. */
    @ColumnInfo(name = "emoji") @SerialName("emoji") val icon: String = "meander",
    /** What progress is counted in, e.g. "sessions" or "lessons". */
    val unitLabel: String = "parts",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
enum class ItemStatus(@StringRes val labelRes: Int) { WANT(R.string.status_want), ACTIVE(R.string.status_active), DONE(R.string.status_done) }

@Serializable
enum class SessionSource { TIMER, MANUAL }

@Serializable
enum class GoalKind { TIME, ITEMS }

@Serializable
enum class GoalPeriod(@StringRes val labelRes: Int) {
    DAILY(R.string.period_daily), WEEKLY(R.string.period_weekly), MONTHLY(R.string.period_monthly), BY_DATE(R.string.period_by_date)
}

@Serializable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    /** Glyph name; stored in the old "emoji" column so existing databases and backups keep working. */
    @ColumnInfo(name = "emoji") @SerialName("emoji") val icon: String = "scroll",
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
    /** Set when [type] is CUSTOM. */
    val categoryId: Long? = null,
    val creator: String = "",
    val status: ItemStatus = ItemStatus.ACTIVE,
    val subjectId: Long? = null,
    val totalUnits: Int? = null,
    val progressUnits: Int = 0,
    /** Total run time of the media in minutes (audiobook, podcast series, video course, seminar…). */
    val runtimeMin: Int? = null,
    /** Media time already completed before it was tracked in the app. */
    @ColumnInfo(defaultValue = "0") val priorSec: Long = 0,
    /** Listening/watching speed; 1 hour logged at 1.5× covers 1.5 hours of media. */
    @ColumnInfo(defaultValue = "1.0") val playbackSpeed: Float = 1f,
    val rating: Int? = null,
    val takeaway: String = "",
    /** Superseded by [Note] in v5: migrated into a linked note, then left empty. */
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    /** Web link to the media: an online seminar, an audiobook or podcast page, a course, an article. Blank if none. */
    @ColumnInfo(defaultValue = "") val url: String = "",
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
    /** Set when [itemType] is CUSTOM. */
    val categoryId: Long? = null,
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

/** A free-form note, optionally linked to something in the library. Added in DB version 5. */
@Serializable
@Entity(tableName = "notes", indices = [Index("itemId"), Index("updatedAt")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val itemId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    /** The title, or the note's first line when it has none. */
    val heading: String get() = title.ifBlank { body.trim().lineSequence().firstOrNull().orEmpty() }
    val isEmpty: Boolean get() = title.isBlank() && body.isBlank()
}
