package com.rayaurelius.sophinity.ui.components

import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import com.rayaurelius.sophinity.R
import androidx.compose.ui.res.stringResource
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
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.GlyphTile
import androidx.compose.ui.unit.sp
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
    glyph: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = container) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Glyph(glyph, size = 26.dp, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            // Shrinks to fit rather than truncating (narrow layouts, large text, long translations).
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = MaterialTheme.typography.titleLarge.fontSize))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
fun EmptyState(glyph: String, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GlyphTile(glyph, size = 72.dp)
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
    noneLabel: String = stringResource(R.string.subject_none),
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
                if (sel != null) { Glyph(sel.icon, size = 18.dp, tint = Color(sel.color)); Spacer(Modifier.width(6.dp)) }
                Text(sel?.name ?: noneLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelect(null); open = false })
            subjects.filter { !it.archived }.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.name) },
                    leadingIcon = { Glyph(s.icon, size = 20.dp, tint = Color(s.color)) },
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
                    Icon(icon, stringResource(R.string.stars, i), tint = tint, modifier = Modifier.size(size.dp))
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
    val labels = listOf(R.string.focus_1, R.string.focus_2, R.string.focus_3, R.string.focus_4, R.string.focus_5)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { i, label ->
            val n = i + 1
            val sel = value == n
            Column(
                Modifier.clip(MaterialTheme.shapes.small)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onChange(if (sel) null else n) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FocusColumns(n, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** [level] of five rising columns filled in: a small Greek bar chart for focus quality. */
@Composable
fun FocusColumns(level: Int, color: Color, modifier: Modifier = Modifier) {
    val empty = MaterialTheme.colorScheme.outlineVariant
    androidx.compose.foundation.Canvas(modifier.size(width = 34.dp, height = 22.dp)) {
        val w = size.width / 5f
        for (i in 0 until 5) {
            val h = size.height * (0.32f + 0.17f * i)
            drawRoundRect(
                if (i < level) color else empty,
                topLeft = androidx.compose.ui.geometry.Offset(i * w + w * 0.18f, size.height - h),
                size = androidx.compose.ui.geometry.Size(w * 0.64f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.2f),
            )
        }
    }
}

@Composable
fun NumberField(label: String, value: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier, suffix: String? = null) {
    // The field keeps its own text and cursor. Rebuilding the text from [value] on every keystroke
    // moved the cursor, so the next digit could land in front ("3" then "7" became 73, capped to 59).
    // Zero shows as an empty box with a "0" hint: a real "0" let the cursor sit in front of it,
    // so typing 3 then 7 made "370" (capped to 59) instead of 37.
    fun shown(v: Int?) = if (v == null || v == 0) "" else v.toString()
    var field by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(shown(value))) }
    // Follow outside changes (a quick pick, a clamp to the allowed maximum) without fighting typing.
    val showsValue = if (field.text.isEmpty()) value == null || value == 0 else field.text.toIntOrNull() == value
    if (!showsValue) {
        val t = shown(value)
        field = androidx.compose.ui.text.input.TextFieldValue(t, androidx.compose.ui.text.TextRange(t.length))
    }
    // Tapping a box that already holds a number selects it, so typing replaces it: "Log a past
    // session" starts at 30 minutes, and typing 37 used to make "3037" (capped to 59). The selection
    // is applied just after the tap, which would otherwise place a cursor at the end.
    var selectAll by remember { mutableStateOf(false) }
    LaunchedEffect(selectAll) {
        if (selectAll) {
            kotlinx.coroutines.delay(120)
            field = field.copy(selection = androidx.compose.ui.text.TextRange(0, field.text.length))
            selectAll = false
        }
    }
    OutlinedTextField(
        value = field,
        onValueChange = { new ->
            var digits = new.text.filter(Char::isDigit).take(6)
            var cursor = new.selection.end.coerceAtMost(digits.length)
            // "0" then "3" reads as 3, not 03.
            while (digits.length > 1 && digits.startsWith("0")) { digits = digits.drop(1); cursor = (cursor - 1).coerceAtLeast(0) }
            field = androidx.compose.ui.text.input.TextFieldValue(digits, androidx.compose.ui.text.TextRange(cursor))
            onChange(digits.toIntOrNull())
        },
        label = { Text(label) },
        placeholder = { Text("0") },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.onFocusChanged { if (it.isFocused && field.text.isNotEmpty()) selectAll = true },
    )
}

private data class Particle(
    val x: Float, val vx: Float, val vy: Float, val color: Color, val size: Float, val spin: Float, val phase: Float,
)

/** A short celebratory burst. Small, honest celebrations reinforce effort. */
@Composable
fun Confetti(modifier: Modifier = Modifier, durationMs: Long = 2600) {
    if (com.rayaurelius.sophinity.ui.theme.LocalReduceMotion.current) return
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

/** Opens Settings from any page; provided by the app shell. Null in previews and tests of single screens. */
val LocalOpenSettings = androidx.compose.runtime.staticCompositionLocalOf<(() -> Unit)?> { null }

/** A page's title with the Settings button in the top-right corner, the same on every tab. */
@Composable
fun PageHeader(title: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
        SettingsButton()
    }
}

@Composable
fun SettingsButton() {
    val open = LocalOpenSettings.current ?: return
    androidx.compose.material3.IconButton(onClick = open) {
        Icon(androidx.compose.material.icons.Icons.Outlined.Settings, stringResource(R.string.common_settings))
    }
}
