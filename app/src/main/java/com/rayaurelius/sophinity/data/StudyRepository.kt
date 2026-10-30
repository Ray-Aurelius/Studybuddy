package com.rayaurelius.sophinity.data

import androidx.room.withTransaction
import com.rayaurelius.sophinity.data.db.AppDatabase
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.Note
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
    val notes: Flow<List<Note>> = db.notes().observeAll()
    fun notesFor(itemId: Long): Flow<List<Note>> = db.notes().observeFor(itemId)
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

    /**
     * Puts the learner at [positionSec] in a piece of timed media ("I'm at 15:16"), by adjusting how
     * much was done before tracking. Logged study time is untouched. [liveSec] is time on a session
     * for this item still running on the timer.
     */
    suspend fun setMediaPosition(itemId: Long, positionSec: Long, liveSec: Long = 0): LearningItem? {
        val item = db.items().get(itemId) ?: return null
        val logged = db.sessions().secondsForItem(itemId)
        val updated = item.copy(priorSec = com.rayaurelius.sophinity.domain.MediaTime.priorForPosition(item, positionSec, logged, liveSec))
        if (updated != item) db.items().update(updated)
        return updated
    }

    suspend fun getItem(id: Long): LearningItem? = db.items().get(id)
    suspend fun secondsForItem(itemId: Long): Long = db.sessions().secondsForItem(itemId)

    /** The item moved to Finished (keeping any earlier dates). */
    fun finished(item: LearningItem, now: Long = System.currentTimeMillis()) = item.copy(
        status = ItemStatus.DONE, completedAt = item.completedAt ?: now, startedAt = item.startedAt ?: now,
        progressUnits = item.totalUnits ?: item.progressUnits,
    )

    /**
     * "Mark as completed" from the library: logs the rest of the item's length as a session ending
     * now, so it counts in full, and moves it to Finished. Returns the seconds credited.
     */
    suspend fun completeItem(itemId: Long, now: Long = System.currentTimeMillis()): Long = db.withTransaction {
        val item = db.items().get(itemId) ?: return@withTransaction 0L
        val credit = com.rayaurelius.sophinity.domain.MediaTime.creditToCompleteSec(item, db.sessions().secondsForItem(itemId)) ?: 0L
        if (credit > 0) db.sessions().insert(
            StudySession(subjectId = item.subjectId, itemId = itemId, startedAt = now - credit * 1000, durationSec = credit,
                source = com.rayaurelius.sophinity.data.db.SessionSource.MANUAL)
        )
        db.items().update(finished(item, now))
        credit
    }

    /**
     * "Mark as completed" on a logged session: that session is stretched to cover the rest of its
     * item's length, and the item moves to Finished. Returns the seconds credited.
     */
    suspend fun completeFromSession(sessionId: Long, now: Long = System.currentTimeMillis()): Long = db.withTransaction {
        val session = db.sessions().get(sessionId) ?: return@withTransaction 0L
        val item = session.itemId?.let { db.items().get(it) } ?: return@withTransaction 0L
        val credit = com.rayaurelius.sophinity.domain.MediaTime.creditToCompleteSec(item, db.sessions().secondsForItem(item.id)) ?: 0L
        if (credit > 0) db.sessions().update(session.copy(durationSec = session.durationSec + credit))
        db.items().update(finished(item, now))
        credit
    }

    /** The library item a shared link is for (same video, episode or page), if any. */
    suspend fun itemForLink(url: String): LearningItem? {
        val key = com.rayaurelius.sophinity.domain.Links.mediaKey(url)
        return db.items().getAll().filter { it.url.isNotBlank() && com.rayaurelius.sophinity.domain.Links.mediaKey(it.url) == key }
            .minByOrNull { if (it.status == ItemStatus.ACTIVE) 0 else if (it.status == ItemStatus.WANT) 1 else 2 }
    }

    suspend fun deleteItem(item: LearningItem) = db.withTransaction {
        db.sessions().detachItem(item.id)
        db.notes().detachItem(item.id)
        db.items().delete(item)
    }

    // --- Notes
    /** Saves a note (stamping when it changed); an empty note is not kept. Returns its id, or null. */
    suspend fun saveNote(note: Note, now: Long = System.currentTimeMillis()): Long? {
        val n = note.copy(title = note.title.trim(), body = note.body.trimEnd())
        if (n.isEmpty) { if (n.id != 0L) db.notes().delete(n); return null }
        return if (n.id == 0L) db.notes().insert(n.copy(createdAt = now, updatedAt = now))
        else { db.notes().update(n.copy(updatedAt = now)); n.id }
    }
    suspend fun deleteNote(note: Note) = db.notes().delete(note)

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
            notes = db.notes().getAll(),
            streakMinutes = s.streakMinutes,
            restDaysPerWeek = s.restDaysPerWeek,
        )
    }

    suspend fun importData(backup: BackupData) {
        val data = backup.withItemNotesMoved()
        db.withTransaction {
            db.notes().clear()
            db.sessions().clear(); db.items().clear(); db.goals().clear()
            db.subjects().clear(); db.milestones().clear(); db.categories().clear()
            db.subjects().insertAll(data.subjects)
            db.categories().insertAll(data.categories)
            db.items().insertAll(data.items)
            db.sessions().insertAll(data.sessions)
            db.goals().insertAll(data.goals)
            db.milestones().insertAll(data.milestones.map { it.copy(seen = true) })
            db.notes().insertAll(data.notes)
        }
        settingsStore.update {
            it.copy(onboarded = true, streakMinutes = data.streakMinutes, restDaysPerWeek = data.restDaysPerWeek)
        }
    }
}
