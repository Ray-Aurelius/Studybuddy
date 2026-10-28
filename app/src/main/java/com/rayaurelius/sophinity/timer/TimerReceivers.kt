package com.rayaurelius.sophinity.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rayaurelius.sophinity.SophinityApp
import com.rayaurelius.sophinity.reminders.ReminderScheduler
import kotlinx.coroutines.launch

object TimerAlarms {
    const val ACTION_FOCUS_DONE = "com.rayaurelius.sophinity.FOCUS_DONE"
    const val ACTION_BREAK_DONE = "com.rayaurelius.sophinity.BREAK_DONE"

    const val EXTRA_KIND = "kind"

    // One PendingIntent identity for both alarm kinds, so cancel() always clears whichever is set.
    private fun pi(context: Context, kind: String?) = PendingIntent.getBroadcast(
        context, 42, Intent(context, TimerAlarmReceiver::class.java).apply { if (kind != null) putExtra(EXTRA_KIND, kind) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canBeExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    /** Exact when the user granted "Alarms & reminders", otherwise a close-enough inexact alarm. */
    fun schedule(context: Context, atMillis: Long, action: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context, action)
        if (canBeExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, p)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, p)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pi(context, null))
    }
}

private fun BroadcastReceiver.runAsync(context: Context, block: suspend (SophinityApp) -> Unit) {
    val app = context.applicationContext as SophinityApp
    val pending = goAsync()
    app.container.appScope.launch {
        try { block(app) } finally { pending.finish() }
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync(context) { app ->
        when (intent.getStringExtra(TimerAlarms.EXTRA_KIND)) {
            TimerAlarms.ACTION_FOCUS_DONE -> app.container.timer.completeFocusIfDue()
            TimerAlarms.ACTION_BREAK_DONE -> app.container.timer.endBreak(alert = true)
        }
    }
}

class TimerActionReceiver : BroadcastReceiver() {
    companion object {
        const val PAUSE = "com.rayaurelius.sophinity.PAUSE"
        const val RESUME = "com.rayaurelius.sophinity.RESUME"
        const val FINISH = "com.rayaurelius.sophinity.FINISH"
        const val START_BREAK = "com.rayaurelius.sophinity.START_BREAK"
        const val SKIP_BREAK = "com.rayaurelius.sophinity.SKIP_BREAK"
    }

    override fun onReceive(context: Context, intent: Intent) = runAsync(context) { app ->
        val timer = app.container.timer
        when (intent.action) {
            PAUSE -> timer.pause()
            RESUME -> timer.resume()
            FINISH -> timer.finish()?.let { id ->
                val mins = (app.container.repo.getSession(id)?.durationSec ?: 0) / 60
                TimerNotifier(context).savedNotice(id, mins)
            }
            START_BREAK -> {
                androidx.core.app.NotificationManagerCompat.from(context).cancel(TimerNotifier.ID_ALERT)
                timer.startBreak()
            }
            SKIP_BREAK -> timer.endBreak(alert = false)
        }
    }
}

/** Restores timer notification/alarms and reminder schedule after reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runAsync(context) { app ->
            app.container.timer.restore()
            ReminderScheduler.apply(context, app.container.repo.settingsStore.current())
        }
    }
}
