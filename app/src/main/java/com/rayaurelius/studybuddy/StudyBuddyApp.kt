package com.rayaurelius.studybuddy

import android.app.Application
import android.content.Context
import com.rayaurelius.studybuddy.data.SettingsStore
import com.rayaurelius.studybuddy.data.StudyRepository
import com.rayaurelius.studybuddy.data.db.AppDatabase
import com.rayaurelius.studybuddy.domain.Milestones
import com.rayaurelius.studybuddy.reminders.ReminderScheduler
import com.rayaurelius.studybuddy.timer.Channels
import com.rayaurelius.studybuddy.timer.TimerController
import com.rayaurelius.studybuddy.widget.StudyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Hand-rolled DI: small app, few singletons, no annotation processing to debug. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = AppDatabase.build(context)
    val settingsStore = SettingsStore(context)
    val repo = StudyRepository(db, settingsStore)
    val timer = TimerController(context, repo, appScope)

    /** Session id waiting for a reflection (set from a notification tap). */
    val pendingReflection = MutableStateFlow<Long?>(null)
}

class StudyBuddyApp : Application() {
    lateinit var container: AppContainer
        private set

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Channels.create(this)
        val c = container

        // Unlock milestones whenever the underlying data changes.
        c.appScope.launch {
            c.repo.snapshot.collect { snap ->
                val have = snap.unlocked.map { it.key }.toSet()
                val newly = Milestones.all.filter { it.key !in have && it.achieved(snap.milestoneContext) }.map { it.key }
                c.repo.unlock(newly)
            }
        }

        // Keep the home-screen widget in sync with data + timer.
        c.appScope.launch {
            combine(
                c.repo.snapshot.map { Triple(it.todaySeconds, it.streak.current, it.goalProgress.map { g -> g.current }) },
                c.timer.state.filterNotNull().map { Triple(it.status, it.mode, it.accumulatedMs) },
            ) { a, b -> a to b }
                .distinctUntilChanged()
                .debounce(300)
                .collect { StudyWidget.refresh(this@StudyBuddyApp) }
        }

        c.appScope.launch {
            ReminderScheduler.apply(this@StudyBuddyApp, c.settingsStore.current())
            c.timer.restore()
        }
    }
}
