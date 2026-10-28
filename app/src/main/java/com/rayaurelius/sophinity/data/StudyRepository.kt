package com.rayaurelius.sophinity.data

import androidx.room.withTransaction
import com.rayaurelius.sophinity.data.db.AppDatabase
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.data.db.Subject
import com.rayaurelius.sophinity.data.db.UnlockedMilestone
import com.rayaurelius.sophinity.domain.CategoryInfo
import com.rayaurelius.sophinity.domain.Categories
import com.rayaurelius.sophinity.domain.GoalMath
import com.rayaurelius.sophinity.domain.GoalProgress
import com.rayaurelius.sophinity.domain.LevelInfo
import com.rayaurelius.sophinity.domain.Levels
import com.rayaurelius.sophinity.domain.MilestoneContext
import com.rayaurelius.sophinity.domain.StreakInfo
import com.rayaurelius.sophinity.domain.StudyMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

/** One consistent, computed view of the learner's data that every screen reads from. */
data class Snapshot(
    val today: LocalDate,
    val zone: ZoneId,
    val settings: AppSettings,
    val subjects: List<Subject>,
    val customCategories: List<CustomCategory>,
    val sessions: List<StudySession>,
    val items: List<LearningItem>,
    val goals: List<Goal>,
    val unlocked: List<UnlockedMilestone>,
    val daily: Map<LocalDate, Long>,
    val streak: StreakInfo,
    val level: LevelInfo,
    val goalProgress: List<GoalProgress>,
    val milestoneContext: MilestoneContext,
) {
    /** Earned title (Neophyte → Philosopher) and what the next one needs. */
    val title: com.rayaurelius.sophinity.domain.TitleStatus =
        com.rayaurelius.sophinity.domain.Titles.status(unlocked.associate { it.key to it.unlockedAt })

    val subjectsById: Map<Long, Subject> = subjects.associateBy { it.id }
    val itemsById: Map<Long, LearningItem> = items.associateBy { it.id }
    val customById: Map<Long, CustomCategory> = customCategories.associateBy { it.id }
    /** Built-in library categories followed by the learner's own. */
    val categories: List<CategoryInfo> = Categories.all(customCategories)
    fun categoryOf(item: LearningItem): CategoryInfo = Categories.of(item, customById)
    fun categoryOf(goal: Goal): CategoryInfo? = Categories.forGoal(goal, customById)
    val todaySeconds: Long get() = daily[today] ?: 0L
    val weekSeconds: Long get() = StudyMath.secondsIn(StudyMath.weekRange(today, settings.weekStart), daily)
    val totalSeconds: Long get() = milestoneContext.totalSeconds
    val activeSubjects: List<Subject> get() = subjects.filter { !it.archived }
    val activeItems: List<LearningItem> get() = items.filter { it.status == ItemStatus.ACTIVE }
    fun secondsForItem(itemId: Long) = sessions.filter { it.itemId == itemId }.sumOf { it.durationSec }
    /** Progress through timed media, or null if the item has no run time. */
    fun mediaProgress(item: LearningItem, liveSec: Long = 0) =
        com.rayaurelius.sophinity.domain.MediaTime.progress(item, secondsForItem(item.id), liveSec)
}

class StudyRepository(private val db: AppDatabase, val settingsStore: SettingsStore) {

    /** Bumped at midnight / on resume so "today" stays correct while the app is open. */
    private val clock = MutableStateFlow(LocalDate.now())
    fun refreshDate() { clock.value = LocalDate.now() }

    val subjects = db.subjects().observeAll()
    val categories = db.categories().observeAll()
    val sessions = db.sessions().observeAll()
    val items = db.items().observeAll()
    val goals = db.goals().observeActive()
    val milestones = db.milestones().observeAll()
    val settings = settingsStore.settings

    private data class Raw(
        val subjects: List<Subject>,
        val sessions: List<StudySession>,
        val items: List<LearningItem>,
        val goals: List<Goal>,
        val allGoals: List<Goal>,
    )

    val snapshot: Flow<Snapshot> = combine(
        combine(subjects, sessions, items, goals, db.goals().observeAll(), ::Raw),
        milestones,
        settings,
        clock,
        categories,
    ) { raw, unlocked, s, today, cats -> compute(raw, unlocked, s, today, cats) }

    private fun compute(raw: Raw, unlocked: List<UnlockedMilestone>, s: AppSettings, today: LocalDate, cats: List<CustomCategory>): Snapshot {
        val zone = ZoneId.systemDefault()
        val daily = StudyMath.dailySeconds(raw.sessions, zone)
        val streak = StudyMath.streak(daily, today, s.streakMinutes * 60L, s.restDaysPerWeek, s.weekStart)
        val progress = raw.goals.map { GoalMath.progress(it, raw.sessions, raw.items, today, s.weekStart, zone) }
        val wins = GoalMath.totalWins(raw.allGoals, raw.sessions, raw.items, today, s.weekStart, zone)
        val ctx = MilestoneContext.from(raw.sessions, raw.items, streak.best, wins, zone)
        return Snapshot(
            today = today, zone = zone, settings = s,
            subjects = raw.subjects, customCategories = cats, sessions = raw.sessions, items = raw.items, goals = raw.goals,
            unlocked = unlocked, daily = daily, streak = streak,
            level = Levels.forSeconds(ctx.totalSeconds), goalProgress = progress, milestoneContext = ctx,
        )
    }

    // --- Sessions
    suspend fun addSession(s: StudySession): Long = db.sessions().insert(s)
    suspend fun updateSession(s: StudySession) = db.sessions().update(s)
    suspend fun deleteSession(s: StudySession) = db.sessions().delete(s)
    suspend fun getSession(id: Long) = db.sessions().get(id)
    suspend fun secondsToday(): Long {
        val zone = ZoneId.systemDefault()
        val r = StudyMath.dayRange(LocalDate.now())
        return db.sessions().secondsBetween(r.startMillis(zone), r.endMillis(zone))
    }

    // --- Subjects
    suspend fun addSubject(s: Subject): Long = db.subjects().insert(s)
    suspend fun updateSubject(s: Subject) = db.subjects().update(s)

    // --- Custom categories
    suspend fun saveCategory(c: CustomCategory): Long =
        if (c.id == 0L) db.categories().insert(c) else { db.categories().update(c); c.id }

    suspend fun deleteCategory(c: CustomCategory) = db.withTransaction {
        db.categories().detachItems(c.id)
        db.categories().detachGoals(c.id)
        db.categories().delete(c)
    }

    // --- Library
    suspend fun saveItem(item: LearningItem): Long =
        if (item.id == 0L) db.items().insert(item) else { db.items().update(item); item.id }

    suspend fun deleteItem(item: LearningItem) = db.withTransaction {
        db.sessions().detachItem(item.id)
        db.items().delete(item)
    }

    // --- Goals
    suspend fun saveGoal(goal: Goal): Long =
        if (goal.id == 0L) db.goals().insert(goal) else { db.goals().update(goal); goal.id }
    suspend fun deleteGoal(goal: Goal) = db.goals().delete(goal)

    // --- Milestones
    suspend fun unlock(keys: List<String>) {
        if (keys.isEmpty()) return
        val now = System.currentTimeMillis()
        db.milestones().insertIgnore(keys.map { UnlockedMilestone(it, now) })
    }
    suspend fun markMilestonesSeen(keys: List<String>) = db.milestones().markSeen(keys)

    // --- Backup
    suspend fun exportData(): BackupData {
        val s = settingsStore.current()
        return BackupData(
            version = BackupData.CURRENT_VERSION,
            exportedAt = System.currentTimeMillis(),
            subjects = db.subjects().getAll(),
            categories = db.categories().getAll(),
            sessions = db.sessions().getAll(),
            items = db.items().getAll(),
            goals = db.goals().getAll(),
            milestones = db.milestones().getAll(),
            streakMinutes = s.streakMinutes,
            restDaysPerWeek = s.restDaysPerWeek,
        )
    }

    suspend fun importData(data: BackupData) {
        db.withTransaction {
            db.sessions().clear(); db.items().clear(); db.goals().clear()
            db.subjects().clear(); db.milestones().clear(); db.categories().clear()
            db.subjects().insertAll(data.subjects)
            db.categories().insertAll(data.categories)
            db.items().insertAll(data.items)
            db.sessions().insertAll(data.sessions)
            db.goals().insertAll(data.goals)
            db.milestones().insertAll(data.milestones.map { it.copy(seen = true) })
        }
        settingsStore.update {
            it.copy(onboarded = true, streakMinutes = data.streakMinutes, restDaysPerWeek = data.restDaysPerWeek)
        }
    }
}
