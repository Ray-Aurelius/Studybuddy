package com.rayaurelius.sophinity.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.rayaurelius.sophinity.domain.StudyMath
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class Bar(val label: String, val seconds: Long, val detail: String, val highlight: Boolean = false)

/** Bar chart of study time. Tap a bar to see its exact value. */
@Composable
fun StudyBarChart(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    goalLineSeconds: Long? = null,
    labelEvery: Int = 1,
) {
    if (bars.isEmpty()) return
    var selected by remember(bars) { mutableIntStateOf(bars.indexOfLast { it.highlight }.takeIf { it >= 0 } ?: -1) }
    val grow = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) { grow.animateTo(1f, tween(600)) }
    val max = maxOf(bars.maxOf { it.seconds }, goalLineSeconds ?: 0L, 60L)
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.primaryContainer
    val goalColor = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier) {
        val sel = bars.getOrNull(selected)
        Text(
            if (sel != null) "${sel.detail} · ${StudyMath.formatDuration(sel.seconds)}" else "Tap a bar for details",
            style = MaterialTheme.typography.labelLarge,
            color = if (sel != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier.fillMaxWidth().height(height)
                .semantics { contentDescription = bars.joinToString { "${it.detail}: ${StudyMath.formatDuration(it.seconds)}" } }
                .pointerInput(bars) {
                    detectTapGestures { pos ->
                        val slot = size.width / bars.size
                        selected = (pos.x / slot).toInt().coerceIn(0, bars.lastIndex)
                    }
                },
        ) {
            val slot = size.width / bars.size
            val barW = (slot * 0.62f).coerceAtMost(36.dp.toPx())
            val r = CornerRadius(barW / 2.5f)
            bars.forEachIndexed { i, b ->
                val x = i * slot + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), r, alpha = 0.35f)
                val h = (b.seconds.toFloat() / max) * size.height * grow.value
                if (h > 0f) {
                    drawRoundRect(
                        color = if (i == selected || b.highlight) primary else muted,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h.coerceAtLeast(barW / 2)),
                        cornerRadius = r,
                    )
                }
            }
            goalLineSeconds?.let { g ->
                val y = size.height - (g.toFloat() / max) * size.height
                drawLine(goalColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            bars.forEachIndexed { i, b ->
                Text(
                    if (i % labelEvery == 0) b.label else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (b.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** GitHub-style consistency map: each square a day, darker = more study. */
@Composable
fun StudyHeatmap(
    daily: Map<LocalDate, Long>,
    today: LocalDate,
    weekStart: DayOfWeek,
    weeks: Int = 18,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val start = today.with(TemporalAdjusters.previousOrSame(weekStart)).minusWeeks((weeks - 1).toLong())
    fun color(sec: Long): Color = when {
        sec <= 0 -> empty
        sec < 15 * 60 -> base.copy(alpha = 0.28f)
        sec < 30 * 60 -> base.copy(alpha = 0.5f)
        sec < 60 * 60 -> base.copy(alpha = 0.75f)
        else -> base
    }
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
            for (w in 0 until weeks) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    for (d in 0 until 7) {
                        val date = start.plusDays((w * 7 + d).toLong())
                        val future = date.isAfter(today)
                        Box(
                            Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(3.dp))
                                .background(if (future) Color.Transparent else color(daily[date] ?: 0)),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(0L, 600L, 1200L, 2400L, 4000L).forEach {
                Spacer(Modifier.width(4.dp))
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color(it)))
            }
            Spacer(Modifier.width(4.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit = {},
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val s = stroke.toPx()
            val d = size.minDimension - s
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(s))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, tl, Size(d, d), style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun LinearMeter(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), tween(500)) }
    Box(modifier.height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Box(Modifier.fillMaxWidth(anim.value).height(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
    }
}
