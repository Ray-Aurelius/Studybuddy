package com.rayaurelius.studybuddy.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayaurelius.studybuddy.AppContainer
import com.rayaurelius.studybuddy.data.Snapshot
import com.rayaurelius.studybuddy.data.db.SessionSource
import com.rayaurelius.studybuddy.data.db.StudySession
import com.rayaurelius.studybuddy.timer.TimerMode
import com.rayaurelius.studybuddy.timer.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot: StateFlow<Snapshot?> = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val timer: StateFlow<TimerState?> = c.timer.state

    private val _reflect = MutableStateFlow<StudySession?>(null)
    /** A just-finished session waiting for the person's quick reflection. */
    val reflect = _reflect.asStateFlow()

    init {
        viewModelScope.launch {
            c.pendingReflection.filterNotNull().collect { id ->
                c.pendingReflection.value = null
                _reflect.value = c.repo.getSession(id)
            }
        }
    }

    fun start(mode: TimerMode, subjectId: Long?, itemId: Long?) = viewModelScope.launch { c.timer.start(mode, subjectId, itemId) }
    fun pause() = viewModelScope.launch { c.timer.pause() }
    fun resume() = viewModelScope.launch { c.timer.resume() }
    fun discard() = viewModelScope.launch { c.timer.discard() }
    fun startBreak() = viewModelScope.launch { _reflect.value = null; c.timer.startBreak() }
    fun skipBreak() = viewModelScope.launch { c.timer.endBreak(alert = false) }
    fun setSubject(subjectId: Long?, itemId: Long?) = viewModelScope.launch { c.timer.setSubject(subjectId, itemId) }

    fun finish() = viewModelScope.launch {
        c.timer.finish()?.let { _reflect.value = c.repo.getSession(it) }
    }

    /** On-screen ticker noticed the focus block ended while the app was open. */
    fun completeIfDue() = viewModelScope.launch {
        c.timer.completeFocusIfDue()?.let { _reflect.value = c.repo.getSession(it) }
    }

    fun saveReflection(session: StudySession, note: String, focus: Int?) = viewModelScope.launch {
        c.repo.updateSession(session.copy(note = note.trim(), focus = focus))
        _reflect.value = null
    }

    fun dismissReflection() { _reflect.value = null }

    fun addManual(subjectId: Long?, itemId: Long?, startedAt: Long, minutes: Int, note: String) = viewModelScope.launch {
        if (minutes <= 0) return@launch
        c.repo.addSession(
            StudySession(
                subjectId = subjectId, itemId = itemId, startedAt = startedAt,
                durationSec = minutes * 60L, note = note.trim(), source = SessionSource.MANUAL,
            )
        )
    }

    fun markSeen(keys: List<String>) = viewModelScope.launch { c.repo.markMilestonesSeen(keys) }
}
