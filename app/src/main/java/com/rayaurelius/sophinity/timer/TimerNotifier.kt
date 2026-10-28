package com.rayaurelius.sophinity.timer

import com.rayaurelius.sophinity.ui.i18n.AppLocale
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
import com.rayaurelius.sophinity.MainActivity
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.domain.StudyMath

object Channels {
    const val TIMER = "timer"
    const val ALERTS = "alerts"
    const val REMINDERS = "reminders"

    fun create(base: Context) {
        val context = AppLocale.localized(base)
        val nm = base.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(TIMER, context.getString(R.string.ch_timer), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.ch_timer_desc)
                    setShowBadge(false)
                },
                NotificationChannel(ALERTS, context.getString(R.string.ch_alerts), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.ch_alerts_desc)
                },
                NotificationChannel(REMINDERS, context.getString(R.string.ch_reminders), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.ch_reminders_desc)
                },
            )
        )
    }

    fun canPost(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class TimerNotifier(private val base: Context) {
    /** Resources in the app language (matters on Android 12 and below). */
    private val context: Context get() = AppLocale.localized(base)
    private val nm = NotificationManagerCompat.from(base)

    companion object {
        const val ID_TIMER = 1001
        const val ID_ALERT = 1002
        const val EXTRA_REFLECT_SESSION = "reflect_session_id"
    }

    private fun openApp(sessionId: Long? = null): PendingIntent {
        val intent = Intent(base, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            sessionId?.let { putExtra(EXTRA_REFLECT_SESSION, it) }
        }
        return PendingIntent.getActivity(
            base, sessionId?.toInt() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun action(action: String, req: Int): PendingIntent = PendingIntent.getBroadcast(
        base, req, Intent(base, TimerActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun show(s: TimerState, subjectName: String?, mediaLine: String? = null) {
        if (!Channels.canPost(base)) return
        if (s.status == TimerStatus.IDLE) { nm.cancel(ID_TIMER); return }
        val now = System.currentTimeMillis()
        val b = NotificationCompat.Builder(base, Channels.TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openApp())
            .setColor(0xFF0A6FD8.toInt())
        when (s.status) {
            TimerStatus.RUNNING -> {
                val focus = s.mode == TimerMode.FOCUS
                b.setContentTitle(if (focus) context.getString(R.string.notif_focus_title, subjectName ?: context.getString(R.string.common_learning)) else context.getString(R.string.notif_learning_title, subjectName ?: "").trim())
                    .setUsesChronometer(true)
                    .setShowWhen(true)
                if (focus) {
                    b.setWhen(now + s.remainingMs(now)).setChronometerCountDown(true).setContentText(mediaLine ?: context.getString(R.string.notif_until_break))
                } else {
                    b.setWhen(now - s.elapsedMs(now)).setContentText(mediaLine ?: context.getString(R.string.notif_every_minute))
                }
                b.addAction(0, context.getString(R.string.common_pause), action(TimerActionReceiver.PAUSE, 1))
                b.addAction(0, context.getString(R.string.common_finish), action(TimerActionReceiver.FINISH, 2))
            }
            TimerStatus.PAUSED -> {
                b.setContentTitle(context.getString(R.string.notif_paused_title, StudyMath.formatClock(s.elapsedMs(now))))
                    .setContentText(mediaLine ?: subjectName ?: context.getString(R.string.notif_tap_resume))
                    .setShowWhen(false)
                b.addAction(0, context.getString(R.string.common_resume), action(TimerActionReceiver.RESUME, 3))
                b.addAction(0, context.getString(R.string.common_finish), action(TimerActionReceiver.FINISH, 2))
            }
            TimerStatus.BREAK -> {
                b.setContentTitle(context.getString(R.string.notif_break_title))
                    .setContentText(context.getString(R.string.notif_break_text))
                    .setUsesChronometer(true).setChronometerCountDown(true).setWhen(s.breakEndsAt).setShowWhen(true)
                b.addAction(0, context.getString(R.string.notif_skip_break), action(TimerActionReceiver.SKIP_BREAK, 4))
            }
            TimerStatus.IDLE -> Unit
        }
        try { nm.notify(ID_TIMER, b.build()) } catch (_: SecurityException) { }
    }

    fun alertFocusDone(sessionId: Long, minutes: Long) {
        if (!Channels.canPost(base)) return
        val n = NotificationCompat.Builder(base, Channels.ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notif_focus_done_title))
            .setContentText(context.getString(R.string.notif_focus_done_text, StudyMath.formatDuration(minutes * 60)))
            .setContentIntent(openApp(sessionId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.notif_start_break), action(TimerActionReceiver.START_BREAK, 5))
            .build()
        try { nm.notify(ID_ALERT, n) } catch (_: SecurityException) { }
    }

    fun alertBreakDone() {
        if (!Channels.canPost(base)) return
        val n = NotificationCompat.Builder(base, Channels.ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notif_break_over))
            .setContentText(context.getString(R.string.notif_break_over_text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { nm.notify(ID_ALERT, n) } catch (_: SecurityException) { }
    }

    fun savedNotice(sessionId: Long, minutes: Long) {
        if (!Channels.canPost(base)) return
        val n = NotificationCompat.Builder(base, Channels.ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notif_saved_title, StudyMath.formatDuration(minutes * 60)))
            .setContentText(context.getString(R.string.notif_saved_text))
            .setContentIntent(openApp(sessionId))
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        try { nm.notify(ID_ALERT, n) } catch (_: SecurityException) { }
    }
}
