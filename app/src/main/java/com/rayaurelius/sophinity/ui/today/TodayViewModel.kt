package com.rayaurelius.sophinity.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.SessionSource
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.timer.TimerMode
import com.rayaurelius.sophinity.timer.TimerState
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

    fun start(mode: TimerMode, subjectId: Long?, itemId: Long?, headStartMs: Long = 0) = viewModelScope.launch { c.timer.start(mode, subjectId, itemId, headStartMs) }
    fun pause() = viewModelScope.launch { c.timer.pause() }
    fun resume() = viewModelScope.launch { c.timer.resume() }
    fun discard() = viewModelScope.launch { c.timer.discard() }
    fun startBreak() = viewModelScope.launch { _reflect.value = null; c.timer.startBreak() }
    fun skipBreak() = viewModelScope.launch { c.timer.endBreak(alert = false) }
    fun setSubject(subjectId: Long?, itemId: Long?) = viewModelScope.launch { c.timer.setSubject(subjectId, itemId) }

    /** "I'm at 15:16": moves the learner's place in the item; logged study time is unchanged. */
    fun setPosition(itemId: Long, positionSec: Long) = viewModelScope.launch {
        c.repo.setMediaPosition(itemId, positionSec, c.timer.liveSecFor(itemId))
    }

    /** What sharing a link to Sophinity did. */
    sealed interface ShareResult {
        data class Matched(val item: com.rayaurelius.sophinity.data.db.LearningItem, val action: com.rayaurelius.sophinity.timer.LinkTimer.Action, val positionSec: Long?) : ShareResult
        data class NotFound(val url: String) : ShareResult
    }

    /**
     * A link shared from YouTube, Vimeo, Spotify or a browser. If it's for something in the library,
     * time it, and if the link says where playback is ("Start at 15:16"), move the learner's place there.
     */
    suspend fun handleShare(url: String): ShareResult {
        val item = c.repo.itemForLink(url) ?: return ShareResult.NotFound(url)
        val action = c.timer.startForLink(item.id, item.subjectId)
        val pos = com.rayaurelius.sophinity.domain.Links.timestampSec(url)
            ?.takeIf { com.rayaurelius.sophinity.domain.MediaTime.supportsRuntime(item.type) }
        if (pos != null) c.repo.setMediaPosition(item.id, pos, c.timer.liveSecFor(item.id))
        return ShareResult.Matched(item, action, pos)
    }

    fun startOn(itemId: Long, subjectId: Long?) = viewModelScope.launch { c.timer.startForLink(itemId, subjectId) }

    /** Stop the timer and count the rest of the item's length; the reflection follows as usual. */
    fun finishCompleted() = viewModelScope.launch {
        c.timer.finishCompleted()?.let { _reflect.value = c.repo.getSession(it) }
    }

    /** "Mark as completed" from a Continue card: credit the rest of the item and move it to Finished. */
    fun completeItem(itemId: Long, onDone: (Long) -> Unit) = viewModelScope.launch {
        // If it's the one being timed, end that session first so its time is kept.
        val t = c.timer.current()
        if (t.isActive && t.itemId == itemId) { c.timer.finishCompleted()?.let { _reflect.value = c.repo.getSession(it) }; onDone(-1); return@launch }
        onDone(c.repo.completeItem(itemId))
    }

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

    fun markFinished(item: com.rayaurelius.sophinity.data.db.LearningItem) = viewModelScope.launch {
        c.repo.saveItem(
            item.copy(
                status = com.rayaurelius.sophinity.data.db.ItemStatus.DONE,
                completedAt = System.currentTimeMillis(),
                progressUnits = item.totalUnits ?: item.progressUnits,
            )
        )
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
    fun markTitleSeen(rank: Int) = viewModelScope.launch { c.settingsStore.update { it.copy(seenTitleRank = rank) } }
}
