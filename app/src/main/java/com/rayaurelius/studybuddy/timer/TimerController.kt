package com.rayaurelius.studybuddy.timer

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rayaurelius.studybuddy.data.StudyRepository
import com.rayaurelius.studybuddy.data.db.SessionSource
import com.rayaurelius.studybuddy.data.db.StudySession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TimerStatus { IDLE, RUNNING, PAUSED, BREAK }
enum class TimerMode { STOPWATCH, FOCUS }

data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val mode: TimerMode = TimerMode.STOPWATCH,
    /** Wall-clock time the session first started; becomes the session's startedAt. */
    val startedAt: Long = 0,
    /** Wall-clock start of the current running segment. */
    val segmentStart: Long = 0,
    val accumulatedMs: Long = 0,
    val targetMs: Long = 0,
    val subjectId: Long? = null,
    val itemId: Long? = null,
    val breakEndsAt: Long = 0,
) {
    fun elapsedMs(now: Long = System.currentTimeMillis()): Long {
        val raw = accumulatedMs + if (status == TimerStatus.RUNNING) (now - segmentStart).coerceAtLeast(0) else 0
        return if (mode == TimerMode.FOCUS && targetMs > 0) raw.coerceAtMost(targetMs) else raw
    }
    fun remainingMs(now: Long = System.currentTimeMillis()) = (targetMs - elapsedMs(now)).coerceAtLeast(0)
    val isActive get() = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED
    fun focusDue(now: Long = System.currentTimeMillis()) =
        status == TimerStatus.RUNNING && mode == TimerMode.FOCUS && targetMs > 0 && elapsedMs(now) >= targetMs
}

private val Context.timerDataStore by preferencesDataStore(name = "timer")

/**
 * Timestamp-based timer. Nothing "ticks" in the background: state is persisted, elapsed time is
 * derived from the clock, and an alarm fires only when a focus block or break ends.
 */
class TimerController(
    private val context: Context,
    private val repo: StudyRepository,
    scope: CoroutineScope,
) {
    private val store = context.timerDataStore
    private val mutex = Mutex()
    private val notifier = TimerNotifier(context)

    private object K {
        val status = stringPreferencesKey("status")
        val mode = stringPreferencesKey("mode")
        val startedAt = longPreferencesKey("started_at")
        val segment = longPreferencesKey("segment_start")
        val acc = longPreferencesKey("accumulated")
        val target = longPreferencesKey("target")
        val subject = longPreferencesKey("subject")
        val item = longPreferencesKey("item")
        val breakEnds = longPreferencesKey("break_ends")
    }

    val state: StateFlow<TimerState?> = store.data.map { it.toState() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun current(): TimerState = store.data.first().toState()

    private fun Preferences.toState() = TimerState(
        status = this[K.status]?.let { runCatching { TimerStatus.valueOf(it) }.getOrNull() } ?: TimerStatus.IDLE,
        mode = this[K.mode]?.let { runCatching { TimerMode.valueOf(it) }.getOrNull() } ?: TimerMode.STOPWATCH,
        startedAt = this[K.startedAt] ?: 0,
        segmentStart = this[K.segment] ?: 0,
        accumulatedMs = this[K.acc] ?: 0,
        targetMs = this[K.target] ?: 0,
        subjectId = this[K.subject]?.takeIf { it > 0 },
        itemId = this[K.item]?.takeIf { it > 0 },
        breakEndsAt = this[K.breakEnds] ?: 0,
    )

    private suspend fun save(s: TimerState) {
        store.edit {
            it[K.status] = s.status.name
            it[K.mode] = s.mode.name
            it[K.startedAt] = s.startedAt
            it[K.segment] = s.segmentStart
            it[K.acc] = s.accumulatedMs
            it[K.target] = s.targetMs
            it[K.subject] = s.subjectId ?: -1
            it[K.item] = s.itemId ?: -1
            it[K.breakEnds] = s.breakEndsAt
        }
        notifier.show(s, subjectName(s.subjectId))
        when {
            s.status == TimerStatus.RUNNING && s.mode == TimerMode.FOCUS ->
                TimerAlarms.schedule(context, System.currentTimeMillis() + s.remainingMs(), TimerAlarms.ACTION_FOCUS_DONE)
            s.status == TimerStatus.BREAK -> TimerAlarms.schedule(context, s.breakEndsAt, TimerAlarms.ACTION_BREAK_DONE)
            else -> TimerAlarms.cancel(context)
        }
    }

    private suspend fun subjectName(id: Long?): String? =
        id?.let { sid -> repo.subjects.first().firstOrNull { it.id == sid }?.name }

    suspend fun start(mode: TimerMode, subjectId: Long?, itemId: Long?) = mutex.withLock {
        val now = System.currentTimeMillis()
        val focus = repo.settingsStore.current().focusMinutes
        save(
            TimerState(
                status = TimerStatus.RUNNING, mode = mode, startedAt = now, segmentStart = now,
                targetMs = if (mode == TimerMode.FOCUS) focus * 60_000L else 0,
                subjectId = subjectId, itemId = itemId,
            )
        )
        repo.settingsStore.update { it.copy(lastSubjectId = subjectId) }
    }

    suspend fun pause() = mutex.withLock {
        val s = current()
        if (s.status != TimerStatus.RUNNING) return@withLock
        val now = System.currentTimeMillis()
        save(s.copy(status = TimerStatus.PAUSED, accumulatedMs = s.elapsedMs(now)))
    }

    suspend fun resume() = mutex.withLock {
        val s = current()
        if (s.status != TimerStatus.PAUSED) return@withLock
        save(s.copy(status = TimerStatus.RUNNING, segmentStart = System.currentTimeMillis()))
    }

    suspend fun setSubject(subjectId: Long?, itemId: Long?) = mutex.withLock {
        val s = current()
        if (s.isActive) save(s.copy(subjectId = subjectId, itemId = itemId))
    }

    /** Ends the session and saves it immediately (so nothing is lost). Returns the new session id. */
    suspend fun finish(): Long? = mutex.withLock { finishLocked() }

    private suspend fun finishLocked(): Long? {
        val s = current()
        if (!s.isActive) return null
        val elapsed = s.elapsedMs()
        save(TimerState())
        if (elapsed < 60_000) return null // under a minute isn't worth a record
        return repo.addSession(
            StudySession(
                subjectId = s.subjectId, itemId = s.itemId, startedAt = s.startedAt,
                durationSec = elapsed / 1000, source = SessionSource.TIMER,
            )
        )
    }

    /** Called by the alarm and the on-screen ticker; safe to call repeatedly. */
    suspend fun completeFocusIfDue(): Long? = mutex.withLock {
        if (!current().focusDue()) return@withLock null
        val id = finishLocked()
        if (id != null) {
            val mins = repo.getSession(id)?.durationSec?.div(60) ?: 0
            notifier.alertFocusDone(id, mins)
        }
        id
    }

    suspend fun startBreak() = mutex.withLock {
        val mins = repo.settingsStore.current().breakMinutes
        save(TimerState(status = TimerStatus.BREAK, breakEndsAt = System.currentTimeMillis() + mins * 60_000L))
    }

    suspend fun endBreak(alert: Boolean) = mutex.withLock {
        if (current().status != TimerStatus.BREAK) return@withLock
        save(TimerState())
        if (alert) notifier.alertBreakDone()
    }

    suspend fun discard() = mutex.withLock { save(TimerState()) }

    /** Home-screen widget button: start, pause, or resume. */
    suspend fun toggleFromWidget() {
        val s = current()
        when (s.status) {
            TimerStatus.IDLE, TimerStatus.BREAK ->
                start(TimerMode.STOPWATCH, repo.settingsStore.current().lastSubjectId, null)
            TimerStatus.RUNNING -> pause()
            TimerStatus.PAUSED -> resume()
        }
    }

    /** Re-post notification / alarms after reboot or app update. */
    suspend fun restore() = mutex.withLock {
        val s = current()
        if (s.focusDue()) {
            finishLocked()?.let { id -> notifier.alertFocusDone(id, (repo.getSession(id)?.durationSec ?: 0) / 60) }
        } else if (s.status == TimerStatus.BREAK && s.breakEndsAt <= System.currentTimeMillis()) {
            save(TimerState())
        } else {
            save(s)
        }
    }
}
