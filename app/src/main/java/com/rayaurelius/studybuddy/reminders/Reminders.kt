package com.rayaurelius.studybuddy.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rayaurelius.studybuddy.MainActivity
import com.rayaurelius.studybuddy.R
import com.rayaurelius.studybuddy.StudyBuddyApp
import com.rayaurelius.studybuddy.data.AppSettings
import com.rayaurelius.studybuddy.timer.Channels
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK = "daily_reminder"

    fun apply(context: Context, s: AppSettings) {
        val wm = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        if (!s.reminderEnabled) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val now = LocalDateTime.now()
        var next = now.with(LocalTime.of(s.reminderHour, s.reminderMinute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as StudyBuddyApp
        val repo = app.container.repo
        val settings = repo.settingsStore.current()
        if (!settings.reminderEnabled || !Channels.canPost(applicationContext)) return Result.success()

        // Already hit today's minimum? Don't nag — respect autonomy.
        if (repo.secondsToday() >= settings.streakMinutes * 60L) return Result.success()

        val snap = repo.snapshot.first()
        val streak = snap.streak.current
        val place = settings.reminderPlace.trim()
        val title = when {
            streak >= 2 -> "Keep your $streak-day streak going 🔥"
            else -> "Time to learn something ✨"
        }
        val body = buildString {
            append("You planned to study now")
            if (place.isNotEmpty()) append(" at $place")
            append(". Even ${settings.streakMinutes} minutes counts.")
        }
        val pi = PendingIntent.getActivity(
            applicationContext, 7, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(applicationContext, Channels.REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(applicationContext).notify(2001, n) } catch (_: SecurityException) { }
        return Result.success()
    }
}
