package com.rayaurelius.sophinity.reminders

import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.ui.i18n.AppLocale
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
import com.rayaurelius.sophinity.MainActivity
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.SophinityApp
import com.rayaurelius.sophinity.data.AppSettings
import com.rayaurelius.sophinity.timer.Channels
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
        val app = applicationContext as SophinityApp
        val repo = app.container.repo
        val settings = repo.settingsStore.current()
        if (!settings.reminderEnabled || !Channels.canPost(applicationContext)) return Result.success()

        // Already hit today's minimum? Don't nag — respect autonomy.
        if (repo.secondsToday() >= settings.streakMinutes * 60L) return Result.success()

        val snap = repo.snapshot.first()
        val streak = snap.streak.current
        val place = settings.reminderPlace.trim()
        val ctx = AppLocale.localized(applicationContext)
        val title = when {
            streak >= 2 -> ctx.resources.getQuantityString(R.plurals.reminder_title_streak, streak, streak)
            else -> ctx.getString(R.string.reminder_title)
        }
        val minimum = StudyMath.formatDuration(settings.streakMinutes * 60L)
        val due = repo.dueNoteCount(java.time.LocalDate.now().toEpochDay())
        val body = (if (place.isNotEmpty()) ctx.getString(R.string.reminder_body_place, place, minimum)
            else ctx.getString(R.string.reminder_body, minimum)) +
            (if (due > 0) " " + ctx.resources.getQuantityString(R.plurals.reminder_review_extra, due, due) else "")
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
