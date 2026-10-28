package com.rayaurelius.studybuddy.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rayaurelius.studybuddy.MainActivity
import com.rayaurelius.studybuddy.R
import com.rayaurelius.studybuddy.domain.StudyMath

object Channels {
    const val TIMER = "timer"
    const val ALERTS = "alerts"
    const val REMINDERS = "reminders"

    fun create(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(TIMER, "Study timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows your running study session"
                    setShowBadge(false)
                },
                NotificationChannel(ALERTS, "Focus & break alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Tells you when a focus block or break ends"
                },
                NotificationChannel(REMINDERS, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Your daily nudge to study"
                },
            )
        )
    }

    fun canPost(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class TimerNotifier(private val context: Context) {
    private val nm = NotificationManagerCompat.from(context)

    companion object {
        const val ID_TIMER = 1001
        const val ID_ALERT = 1002
        const val EXTRA_REFLECT_SESSION = "reflect_session_id"
    }

    private fun openApp(sessionId: Long? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            sessionId?.let { putExtra(EXTRA_REFLECT_SESSION, it) }
        }
        return PendingIntent.getActivity(
            context, sessionId?.toInt() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun action(action: String, req: Int): PendingIntent = PendingIntent.getBroadcast(
        context, req, Intent(context, TimerActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun show(s: TimerState, subjectName: String?, mediaLine: String? = null) {
        if (!Channels.canPost(context)) return
        if (s.status == TimerStatus.IDLE) { nm.cancel(ID_TIMER); return }
        val now = System.currentTimeMillis()
        val b = NotificationCompat.Builder(context, Channels.TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openApp())
            .setColor(0xFF5B4BDB.toInt())
        when (s.status) {
            TimerStatus.RUNNING -> {
                val focus = s.mode == TimerMode.FOCUS
                b.setContentTitle(if (focus) "Focus block · ${subjectName ?: "Studying"}" else "Studying ${subjectName ?: ""}".trim())
                    .setUsesChronometer(true)
                    .setShowWhen(true)
                if (focus) {
                    b.setWhen(now + s.remainingMs(now)).setChronometerCountDown(true).setContentText(mediaLine ?: "until your break")
                } else {
                    b.setWhen(now - s.elapsedMs(now)).setContentText(mediaLine ?: "Every minute counts")
                }
                b.addAction(0, "Pause", action(TimerActionReceiver.PAUSE, 1))
                b.addAction(0, "Finish", action(TimerActionReceiver.FINISH, 2))
            }
            TimerStatus.PAUSED -> {
                b.setContentTitle("Paused · ${StudyMath.formatClock(s.elapsedMs(now))}")
                    .setContentText(mediaLine ?: subjectName ?: "Tap resume when you're ready")
                    .setShowWhen(false)
                b.addAction(0, "Resume", action(TimerActionReceiver.RESUME, 3))
                b.addAction(0, "Finish", action(TimerActionReceiver.FINISH, 2))
            }
            TimerStatus.BREAK -> {
                b.setContentTitle("Break time ☕")
                    .setContentText("Stretch, hydrate, look far away")
                    .setUsesChronometer(true).setChronometerCountDown(true).setWhen(s.breakEndsAt).setShowWhen(true)
                b.addAction(0, "Skip break", action(TimerActionReceiver.SKIP_BREAK, 4))
            }
            TimerStatus.IDLE -> Unit
        }
        try { nm.notify(ID_TIMER, b.build()) } catch (_: SecurityException) { }
    }

    fun alertFocusDone(sessionId: Long, minutes: Long) {
        if (!Channels.canPost(context)) return
        val n = NotificationCompat.Builder(context, Channels.ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Focus block complete 🎉")
            .setContentText("$minutes min logged. Tap to note one thing you learned.")
            .setContentIntent(openApp(sessionId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Start break", action(TimerActionReceiver.START_BREAK, 5))
            .build()
        try { nm.notify(ID_ALERT, n) } catch (_: SecurityException) { }
    }

    fun alertBreakDone() {
        if (!Channels.canPost(context)) return
        val n = NotificationCompat.Builder(context, Channels.ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Break's over")
            .setContentText("Ready for another focus block?")
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { nm.notify(ID_ALERT, n) } catch (_: SecurityException) { }
    }

    fun savedNotice(sessionId: Long, minutes: Long) {
        if (!Channels.canPost(context)) return
        val n = NotificationCompat.Builder(context, Channels.ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("$minutes min saved")
            .setContentText("Tap to add a quick reflection")
            .setContentIntent(openApp(sessionId))
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        try { nm.notify(ID_ALERT, n) } catch (_: SecurityException) { }
    }
}
