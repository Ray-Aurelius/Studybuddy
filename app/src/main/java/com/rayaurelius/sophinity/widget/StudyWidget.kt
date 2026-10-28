package com.rayaurelius.sophinity.widget

import com.rayaurelius.sophinity.ui.i18n.AppLocale
import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rayaurelius.sophinity.MainActivity
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.SophinityApp
import com.rayaurelius.sophinity.data.db.GoalKind
import com.rayaurelius.sophinity.data.db.GoalPeriod
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.timer.TimerMode
import com.rayaurelius.sophinity.timer.TimerState
import com.rayaurelius.sophinity.timer.TimerStatus
import kotlinx.coroutines.flow.first

private data class WidgetData(
    val todaySec: Long,
    val streak: Int,
    val weekLabel: String,
    val weekFraction: Float?,
    val timer: TimerState,
    val today: String, val paused: String, val focusBlock: String, val learning: String,
    val pause: String, val resume: String, val start: String,
)

class StudyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = (context.applicationContext as SophinityApp).container
        val res = AppLocale.localized(context).resources
        val snap = c.repo.snapshot.first()
        val weekly = snap.goalProgress.firstOrNull { it.goal.kind == GoalKind.TIME && it.goal.period == GoalPeriod.WEEKLY }
        val data = WidgetData(
            todaySec = snap.todaySeconds,
            streak = snap.streak.current,
            weekLabel = weekly?.let { res.getString(R.string.widget_goal_week, StudyMath.formatDuration(it.current * 60L, true), StudyMath.formatDuration(it.target * 60L, true)) }
                ?: res.getString(R.string.widget_this_week, StudyMath.formatDuration(snap.weekSeconds, true)),
            weekFraction = weekly?.fraction,
            timer = c.timer.current(),
            today = res.getString(R.string.common_today_lower), paused = res.getString(R.string.common_paused),
            focusBlock = res.getString(R.string.common_focus_block), learning = res.getString(R.string.common_learning),
            pause = res.getString(R.string.common_pause), resume = res.getString(R.string.common_resume), start = res.getString(R.string.common_start),
        )
        provideContent { GlanceTheme { Content(context, data) } }
    }

    @Composable
    private fun Content(context: Context, d: WidgetData) {
        val colors = GlanceTheme.colors
        Column(
            modifier = GlanceModifier.fillMaxSize().background(colors.widgetBackground).cornerRadius(20.dp)
                .padding(14.dp).clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text("Sophinity", style = TextStyle(color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.defaultWeight())
                Text("🔥 ${d.streak}", style = TextStyle(color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium))
            }
            Spacer(GlanceModifier.height(6.dp))
            if (d.timer.isActive) {
                AndroidRemoteViews(remoteViews = chronometer(context, d.timer))
                Text(
                    if (d.timer.status == TimerStatus.PAUSED) d.paused else if (d.timer.mode == TimerMode.FOCUS) d.focusBlock else d.learning,
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                )
            } else {
                Text(
                    StudyMath.formatDuration(d.todaySec),
                    style = TextStyle(color = colors.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold),
                )
                Text(d.today, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp))
            }
            Spacer(GlanceModifier.defaultWeight())
            if (d.weekFraction != null) {
                LinearProgressIndicator(
                    progress = d.weekFraction, modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = colors.primary, backgroundColor = colors.secondaryContainer,
                )
                Spacer(GlanceModifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text(d.weekLabel, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp), modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(6.dp))
                Button(
                    text = when (d.timer.status) {
                        TimerStatus.RUNNING -> d.pause
                        TimerStatus.PAUSED -> d.resume
                        else -> d.start
                    },
                    onClick = actionRunCallback<ToggleTimerAction>(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, contentColor = colors.onPrimary),
                )
            }
        }
    }

    private fun chronometer(context: Context, t: TimerState): RemoteViews {
        val now = System.currentTimeMillis()
        val running = t.status == TimerStatus.RUNNING
        val countDown = t.mode == TimerMode.FOCUS
        val base = if (countDown) SystemClock.elapsedRealtime() + t.remainingMs(now)
        else SystemClock.elapsedRealtime() - t.elapsedMs(now)
        return RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
            setChronometerCountDown(R.id.chrono, countDown)
            setChronometer(R.id.chrono, base, null, running)
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { StudyWidget().updateAll(context) }
        }
    }
}

class ToggleTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        (context.applicationContext as SophinityApp).container.timer.toggleFromWidget()
        StudyWidget.refresh(context)
    }
}

class StudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyWidget()
}
