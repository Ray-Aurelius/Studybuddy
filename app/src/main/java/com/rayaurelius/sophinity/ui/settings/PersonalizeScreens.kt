@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rayaurelius.sophinity.ui.settings

import com.rayaurelius.sophinity.domain.StudyMath
import androidx.annotation.StringRes
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.ChoiceStyle
import com.rayaurelius.sophinity.ui.components.Choices
import androidx.compose.ui.res.stringResource
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rayaurelius.sophinity.data.AppSettings
import com.rayaurelius.sophinity.data.NavPosition
import com.rayaurelius.sophinity.data.TabId
import com.rayaurelius.sophinity.data.ThemeMode
import com.rayaurelius.sophinity.ui.components.Backgrounds
import com.rayaurelius.sophinity.ui.components.BuiltInBackground
import com.rayaurelius.sophinity.ui.components.appViewModel
import com.rayaurelius.sophinity.ui.theme.ColorEngine
import com.rayaurelius.sophinity.ui.theme.ColorTheme
import com.rayaurelius.sophinity.ui.theme.ColorThemes
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.(AppSettings, SettingsViewModel) -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val s = vm.settings.collectAsStateWithLifecycle().value ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
            )
        },
    ) { pad ->
        LazyColumn(contentPadding = pad, modifier = Modifier.fillMaxSize()) {
            content(s, vm)
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@StringRes
private fun themeLabel(id: String): Int = when (id) {
    "aegean" -> R.string.theme_aegean
    "olive" -> R.string.theme_olive
    "terracotta" -> R.string.theme_terracotta
    "tyrian" -> R.string.theme_tyrian
    "delphi" -> R.string.theme_delphi
    "agora" -> R.string.theme_agora
    "marble" -> R.string.theme_marble
    ColorThemes.WALLPAPER -> R.string.theme_wallpaper
    else -> R.string.theme_custom
}

@StringRes
private fun backgroundLabel(id: String): Int = when (id) {
    Backgrounds.NONE -> R.string.bg_none
    "marble" -> R.string.bg_marble
    "aegean" -> R.string.bg_aegean
    "dawn" -> R.string.bg_dawn
    "night" -> R.string.bg_night
    "keys" -> R.string.bg_keys
    else -> R.string.bg_photo
}

// ---------------------------------------------------------------- Appearance

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importError by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    SubPage(stringResource(R.string.set_appearance), onBack) { s, vm ->
        item { Header(stringResource(R.string.app_light_dark)) }
        item {
            Choices(
                ThemeMode.entries, s.themeMode, { m -> vm.update { it.copy(themeMode = m) } },
                label = { m -> stringResource(when (m) { ThemeMode.SYSTEM -> R.string.common_match_phone; ThemeMode.LIGHT -> R.string.mode_light; ThemeMode.DARK -> R.string.mode_dark }) },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            )
        }
        item { Header(stringResource(R.string.app_menus)) }
        item {
            ToggleRow(stringResource(R.string.app_bubbles), stringResource(R.string.app_bubbles_sub), s.choiceStyle == ChoiceStyle.BUBBLES) { on ->
                vm.update { it.copy(choiceStyle = if (on) ChoiceStyle.BUBBLES else ChoiceStyle.DROPDOWN) }
            }
        }

        item { Header(stringResource(R.string.app_color_theme)) }
        item {
            FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ColorThemes.presets.forEach { t ->
                    ThemeSwatch(stringResource(themeLabel(t.id)), t, s.colorTheme == t.id) { vm.update { it.copy(colorTheme = t.id) } }
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    WallpaperSwatch(s.colorTheme == ColorThemes.WALLPAPER) { vm.update { it.copy(colorTheme = ColorThemes.WALLPAPER) } }
                }
                ThemeSwatch(stringResource(R.string.theme_custom), ColorThemes.fromSeed(s.customSeed), s.colorTheme == ColorThemes.CUSTOM) {
                    vm.update { it.copy(colorTheme = ColorThemes.CUSTOM) }
                }
            }
        }
        if (s.colorTheme == ColorThemes.CUSTOM) {
            item {
                val colorLabel = stringResource(R.string.app_color_n, "%06X")
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.app_pick_color), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorThemes.customSwatches.forEach { c ->
                            val sel = c == s.customSeed
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                                    .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable(role = Role.RadioButton) { vm.update { it.copy(customSeed = c) } }
                                    .semantics { selected = sel; contentDescription = colorLabel.format(c and 0xFFFFFF) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.app_fine_tune), style = MaterialTheme.typography.labelLarge)
                    val hue = ColorEngine.seedOf(s.customSeed).hue.toFloat()
                    Slider(hue, { h -> vm.update { it.copy(customSeed = ColorEngine.solve(h.toDouble(), 0.17, 0.22)) } }, valueRange = 0f..359f)
                }
            }
        }
        item {
            Text(
                stringResource(R.string.app_wcag_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item { HorizontalDivider(Modifier.padding(top = 8.dp)) }

        item { Header(stringResource(R.string.app_background)) }
        item {
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) scope.launch {
                    runCatching { Backgrounds.importPhoto(context, uri) }
                        .onSuccess { id -> vm.update { it.copy(background = id) } }
                        .onFailure { importError = it.message }
                }
            }
            FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf(Backgrounds.NONE) + Backgrounds.builtIn).forEach { id ->
                    BackgroundTile(stringResource(backgroundLabel(id)), s.background == id, { vm.update { it.copy(background = id) } }) {
                        if (id == Backgrounds.NONE) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        else BuiltInBackground(id, Modifier.fillMaxSize())
                    }
                }
                BackgroundTile(
                    stringResource(if (Backgrounds.isPhoto(s.background)) R.string.bg_change_photo else R.string.bg_photo),
                    Backgrounds.isPhoto(s.background),
                    { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                ) {
                    val bmp = if (Backgrounds.isPhoto(s.background)) Backgrounds.loadPhoto(s.background) else null
                    if (bmp != null) {
                        androidx.compose.foundation.Image(bmp.asImageBitmapCompat(), null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddPhotoAlternate, null)
                        }
                    }
                }
            }
            importError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        }
        if (s.background != Backgrounds.NONE) {
            item {
                SliderRow(stringResource(R.string.app_soften), stringResource(R.string.percent, (s.backgroundDim * 100).toInt()), s.backgroundDim, 0f..0.9f) { v ->
                    vm.update { it.copy(backgroundDim = v) }
                }
            }
            if (Backgrounds.isPhoto(s.background) && Build.VERSION.SDK_INT >= 31) {
                item {
                    SliderRow(stringResource(R.string.app_blur), if (s.backgroundBlur < 1f) stringResource(R.string.common_off) else "${s.backgroundBlur.toInt()}", s.backgroundBlur, 0f..24f) { v ->
                        vm.update { it.copy(backgroundBlur = v) }
                    }
                }
            }
        }
    }
}

private fun android.graphics.Bitmap.asImageBitmapCompat() = asImageBitmap()

@Composable
private fun ThemeSwatch(label: String, theme: ColorTheme, selected: Boolean, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3 < 0.5f }
    val r = remember(theme, dark) { theme.roles(dark, false) }
    SwatchFrame(label, selected, onClick) {
        Box(Modifier.fillMaxSize().background(Color(r.surfaceContainerLow))) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(r.primary)))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(r.secondary)))
                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(r.tertiary)))
                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(r.primaryContainer)))
                }
            }
        }
    }
}

@Composable
private fun WallpaperSwatch(selected: Boolean, onClick: () -> Unit) {
    val cs = if (Build.VERSION.SDK_INT >= 31) androidx.compose.material3.dynamicLightColorScheme(LocalContext.current) else MaterialTheme.colorScheme
    SwatchFrame(stringResource(R.string.theme_wallpaper), selected, onClick) {
        Box(Modifier.fillMaxSize().background(cs.surfaceContainerLow)) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(cs.primary))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(cs.secondary))
                    Box(Modifier.size(16.dp).clip(CircleShape).background(cs.tertiary))
                }
            }
        }
    }
}

@Composable
private fun SwatchFrame(label: String, selected: Boolean, onClick: () -> Unit, preview: @Composable () -> Unit) {
    Column(
        Modifier.width(84.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(76.dp, 56.dp).clip(MaterialTheme.shapes.small)
                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        ) {
            preview()
            if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun BackgroundTile(label: String, selected: Boolean, onClick: () -> Unit, preview: @Composable () -> Unit) {
    Column(
        Modifier.width(96.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp, 120.dp).clip(MaterialTheme.shapes.small)
                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        ) { preview() }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun SliderRow(title: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    var local by remember(current) { mutableFloatStateOf(current) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge); Text(value, style = MaterialTheme.typography.labelLarge) }
        Slider(local, { local = it }, valueRange = range, onValueChangeFinished = { onChange(local) })
    }
}

// ---------------------------------------------------------------- Accessibility

@Composable
fun AccessibilityScreen(onBack: () -> Unit) {
    SubPage(stringResource(R.string.set_accessibility), onBack) { s, vm ->
        item { Header(stringResource(R.string.a11y_text)) }
        item {
            val pct = (s.textScale * 100).toInt()
            SliderRow(stringResource(R.string.a11y_text_size), stringResource(R.string.percent, pct), s.textScale, 0.85f..1.6f) { v ->
                vm.update { it.copy(textScale = (v * 20).toInt() / 20f) }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.a11y_preview), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.wisdom_quote, stringResource(R.string.wisdom_4)), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.media_progress, StudyMath.formatDuration(5 * 3600L, true), StudyMath.formatDuration(9 * 3600L + 1800, true), 52), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { ToggleRow(stringResource(R.string.a11y_bold), stringResource(R.string.a11y_bold_sub), s.boldText) { on -> vm.update { it.copy(boldText = on) } } }
        item {
            ToggleRow(stringResource(R.string.a11y_readable), stringResource(R.string.a11y_readable_sub), s.readableFont) { on ->
                vm.update { it.copy(readableFont = on) }
            }
        }
        item { HorizontalDivider() }
        item { Header(stringResource(R.string.a11y_contrast_header)) }
        item {
            ToggleRow(stringResource(R.string.a11y_contrast), stringResource(R.string.a11y_contrast_sub), s.highContrast) { on ->
                vm.update { it.copy(highContrast = on) }
            }
        }
        item { HorizontalDivider() }
        item { Header(stringResource(R.string.a11y_motion)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.a11y_reduce_motion), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.a11y_reduce_motion_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Choices(
                    listOf(null, true, false), s.reduceMotion, { v -> vm.update { it.copy(reduceMotion = v) } },
                    label = { v -> stringResource(when (v) { null -> R.string.common_match_phone; true -> R.string.common_on; false -> R.string.common_off }) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Text(
                stringResource(R.string.a11y_system_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked, null) },
        modifier = Modifier.clickable(role = Role.Switch) { onChange(!checked) },
    )
}

// ---------------------------------------------------------------- Toolbar

@Composable
fun ToolbarScreen(onBack: () -> Unit) {
    SubPage(stringResource(R.string.set_toolbar), onBack) { s, vm ->
        item { Header(stringResource(R.string.tb_position)) }
        item {
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavPosition.entries.forEach { p ->
                    val label = stringResource(when (p) { NavPosition.BOTTOM -> R.string.tb_bottom; NavPosition.LEFT -> R.string.tb_left; NavPosition.RIGHT -> R.string.tb_right })
                    SwatchFrame(label, s.navPosition == p, { vm.update { it.copy(navPosition = p) } }) { PositionPreview(p) }
                }
            }
        }
        item { ToggleRow(stringResource(R.string.tb_labels), stringResource(R.string.tb_labels_sub), s.navLabels) { on -> vm.update { it.copy(navLabels = on) } } }
        item { HorizontalDivider() }
        item { Header(stringResource(R.string.tb_tabs)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_today)) },
                supportingContent = { Text(stringResource(R.string.tb_today_sub)) },
                trailingContent = { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.tb_always_on), tint = MaterialTheme.colorScheme.outline) },
            )
        }
        TabId.entries.forEach { t ->
            item(key = t.name) {
                val label = stringResource(when (t) { TabId.STATS -> R.string.nav_stats; TabId.SESSIONS -> R.string.nav_sessions; TabId.LIBRARY -> R.string.nav_library; TabId.GOALS -> R.string.nav_goals; TabId.LAURELS -> R.string.nav_laurels })
                ToggleRow(label, stringResource(if (t == TabId.SESSIONS) R.string.tb_sessions_hidden_sub else R.string.tb_hidden_sub), t !in s.hiddenTabs) { on ->
                    vm.update { it.copy(hiddenTabs = if (on) it.hiddenTabs - t else it.hiddenTabs + t) }
                }
            }
        }
    }
}

@Composable
private fun PositionPreview(p: NavPosition) {
    val bar = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        when (p) {
            NavPosition.BOTTOM -> Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(12.dp).background(bar))
            NavPosition.LEFT -> Box(Modifier.align(Alignment.CenterStart).width(14.dp).fillMaxHeight().background(bar))
            NavPosition.RIGHT -> Box(Modifier.align(Alignment.CenterEnd).width(14.dp).fillMaxHeight().background(bar))
        }
    }
}

