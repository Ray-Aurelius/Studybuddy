package com.rayaurelius.sophinity.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.SophinityApp
import com.rayaurelius.sophinity.data.db.Subject
import com.rayaurelius.sophinity.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as SophinityApp
    return viewModel(factory = viewModelFactory { initializer { create(app.container) } })
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    action?.invoke()
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun StatTile(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = container) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(16.dp)); action() }
    }
}

@Composable
fun SubjectDot(color: Int, modifier: Modifier = Modifier, size: Int = 10) {
    Box(modifier.size(size.dp).clip(CircleShape).background(Color(color)))
}

/** Compact dropdown chip used to pick a subject (or none). */
@Composable
fun SubjectPicker(
    subjects: List<Subject>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String = "No subject",
) {
    var open by remember { mutableStateOf(false) }
    val sel = subjects.firstOrNull { it.id == selectedId }
    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (sel != null) { Text(sel.emoji); Spacer(Modifier.width(6.dp)) }
                Text(sel?.name ?: noneLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelect(null); open = false })
            subjects.filter { !it.archived }.forEach { s ->
                DropdownMenuItem(
                    text = { Text("${s.emoji}  ${s.name}") },
                    leadingIcon = { SubjectDot(s.color) },
                    onClick = { onSelect(s.id); open = false },
                )
            }
        }
    }
}

@Composable
fun StarRating(rating: Int?, onChange: ((Int?) -> Unit)? = null, size: Int = 28) {
    Row {
        for (i in 1..5) {
            val filled = (rating ?: 0) >= i
            val icon = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline
            val tint = if (filled) Brand.Gold else MaterialTheme.colorScheme.outline
            if (onChange != null) {
                IconButton(onClick = { onChange(if (rating == i) null else i) }, modifier = Modifier.size((size + 12).dp)) {
                    Icon(icon, "$i stars", tint = tint, modifier = Modifier.size(size.dp))
                }
            } else {
                Icon(icon, null, tint = tint, modifier = Modifier.size(size.dp))
            }
        }
    }
}

/** 1–5 focus self-rating. Noticing focus quality is a core metacognitive skill. */
@Composable
fun FocusRating(value: Int?, onChange: (Int?) -> Unit) {
    val faces = listOf("😵‍💫" to "Scattered", "😕" to "Distracted", "🙂" to "Okay", "😃" to "Focused", "🤩" to "Deep flow")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        faces.forEachIndexed { i, (emoji, label) ->
            val n = i + 1
            val sel = value == n
            Column(
                Modifier.clip(MaterialTheme.shapes.small)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onChange(if (sel) null else n) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NumberField(label: String, value: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier, suffix: String? = null) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { t -> onChange(t.filter(Char::isDigit).take(6).toIntOrNull()) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

private data class Particle(
    val x: Float, val vx: Float, val vy: Float, val color: Color, val size: Float, val spin: Float, val phase: Float,
)

/** A short celebratory burst. Small, honest celebrations reinforce effort. */
@Composable
fun Confetti(modifier: Modifier = Modifier, durationMs: Long = 2600) {
    val colors = listOf(Brand.Aegean, Brand.Flame, Brand.Laurel, Brand.Gold, Color(0xFFE5487A), Color(0xFF2B9AE8))
    val particles = remember {
        List(90) {
            val angle = Random.nextFloat() * Math.PI.toFloat()
            val speed = 0.6f + Random.nextFloat() * 0.9f
            Particle(
                x = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f,
                vx = cos(angle) * speed * 0.55f,
                vy = -sin(angle) * speed * 1.4f,
                color = colors.random(), size = 6f + Random.nextFloat() * 8f,
                spin = (Random.nextFloat() - 0.5f) * 720f, phase = Random.nextFloat() * 360f,
            )
        }
    }
    var t by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (t < durationMs) { withFrameNanos { now -> t = (now - start) / 1_000_000 } }
    }
    if (t >= durationMs) return
    Canvas(modifier.fillMaxSize()) {
        val s = t / 1000f
        particles.forEach { p ->
            val x = (p.x + p.vx * s) * size.width
            val y = size.height * 0.45f + (p.vy * s + 1.1f * s * s) * size.height * 0.5f
            val alpha = (1f - t.toFloat() / durationMs).coerceIn(0f, 1f)
            rotate(p.phase + p.spin * s, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}

/** A thin Greek-key (meander) band, the classic border of Greek pottery and temples. */
@Composable
fun GreekKeyBand(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, height: Int = 12) {
    Canvas(modifier.fillMaxWidth().height(height.dp)) {
        val h = size.height
        val u = h / 7f // grid step; one key unit is 6 steps wide
        val stroke = u * 0.9f
        val path = androidx.compose.ui.graphics.Path()
        var x0 = 0f
        while (x0 < size.width) {
            fun p(x: Float, y: Float) = Offset(x0 + x * u, y * u)
            val pts = listOf(p(0f, 6f), p(0f, 1f), p(5f, 1f), p(5f, 5f), p(2f, 5f), p(2f, 3f), p(3f, 3f))
            path.moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { path.lineTo(it.x, it.y) }
            x0 += 6 * u
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawLine(color, Offset(0f, 6f * u), Offset(size.width, 6f * u), strokeWidth = stroke)
    }
}
