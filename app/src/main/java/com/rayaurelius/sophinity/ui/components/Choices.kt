package com.rayaurelius.sophinity.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rayaurelius.sophinity.data.ChoiceStyle

/** Drop-down menus by default; the learner can switch every choice back to bubbles in Appearance. */
val LocalChoiceStyle = staticCompositionLocalOf { ChoiceStyle.DROPDOWN }

/**
 * A single choice among a few options (item type, status, goal period, playback speed, …).
 * Drawn as a drop-down menu or as a row of bubbles, following [LocalChoiceStyle].
 *
 * @param title shown as the drop-down's label; bubbles don't need one because every option is visible.
 * @param scroll lay bubbles out in one scrolling row instead of wrapping (filter bars).
 * @param titleOverBubbles also show [title] above the bubbles (off for self-explanatory filter bars).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T> Choices(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: (@Composable (T) -> Unit)? = null,
    scroll: Boolean = false,
    titleOverBubbles: Boolean = true,
    style: ChoiceStyle = LocalChoiceStyle.current,
) {
    if (style == ChoiceStyle.BUBBLES) {
        if (title != null && titleOverBubbles) {
            androidx.compose.foundation.layout.Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Choices(options, selected, onSelect, label, Modifier, null, icon, scroll, style = style)
            }
            return
        }
        val chip: @Composable (T) -> Unit = { o ->
            FilterChip(
                o == selected, { onSelect(o) }, { Text(label(o), maxLines = 1, softWrap = false) },
                leadingIcon = icon?.let { ic -> { ic(o) } },
            )
        }
        if (scroll) LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(options) { chip(it) } }
        else FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { chip(it) }
        }
        return
    }
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }, modifier) {
        OutlinedTextField(
            value = label(selected), onValueChange = {}, readOnly = true, singleLine = true,
            label = title?.let { t -> { Text(t) } },
            leadingIcon = icon?.let { ic -> { ic(selected) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(open, { open = false }, Modifier.heightIn(max = 400.dp)) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(label(o)) },
                    leadingIcon = icon?.let { ic -> { ic(o) } },
                    trailingIcon = if (o == selected) ({ Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }) else null,
                    onClick = { onSelect(o); open = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
