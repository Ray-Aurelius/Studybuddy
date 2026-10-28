package com.rayaurelius.sophinity.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rayaurelius.sophinity.domain.MediaProgress
import com.rayaurelius.sophinity.domain.MediaTime
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.ui.theme.Brand

/** "2h 14m of 9h 30m · 23%" with a meter, for library and continue-learning cards. */
@Composable
fun MediaMeter(p: MediaProgress, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier) {
        LinearMeter(p.fraction, Modifier.fillMaxWidth(), color = if (p.isComplete) Brand.Laurel else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(
            "${StudyMath.formatDuration(p.completedSec, true)} of ${StudyMath.formatDuration(p.totalSec, true)} · ${p.percent}%",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        )
        if (!compact && !p.isComplete) {
            Text(
                "${StudyMath.formatDuration(p.remainingAtSpeedSec, true)} left" +
                    if (p.speed != 1f) " at ${MediaTime.formatSpeed(p.speed)}" else "",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hours + minutes entry for a duration stored in seconds. */
@Composable
fun DurationFields(
    hoursLabel: String,
    seconds: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val h = seconds?.let { (it / 3600).toInt() }
    val m = seconds?.let { ((it % 3600) / 60).toInt() }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(hoursLabel, h, { nh ->
            onChange(if (nh == null && m == null) null else ((nh ?: 0) * 3600L + (m ?: 0) * 60L))
        }, Modifier.weight(1f), suffix = "h")
        NumberField("Minutes", m, { nm ->
            onChange(if (nm == null && h == null) null else ((h ?: 0) * 3600L + (nm ?: 0).coerceAtMost(59) * 60L))
        }, Modifier.weight(1f), suffix = "m")
    }
}
