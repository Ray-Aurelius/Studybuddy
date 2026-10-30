package com.rayaurelius.sophinity.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.rayaurelius.sophinity.R
import kotlinx.coroutines.launch

/**
 * A bottom sheet for editing something. A sheet normally closes when a swipe or fling carries past
 * the top of its content, when the scrim is tapped or on back, which silently threw away a
 * half-filled form. While [dirty], none of those close it: the person is asked whether to discard
 * their changes, and "Keep editing" leaves the sheet open. The sheet's own Cancel button still
 * closes it straight away, since that is a deliberate choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(dirty: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val isDirty by rememberUpdatedState(dirty)
    var askDiscard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && isDirty) { askDiscard = true; false } else true
        },
    )
    ModalBottomSheet(
        onDismissRequest = { if (isDirty) askDiscard = true else onDismiss() },
        sheetState = state,
        content = content,
    )
    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false; scope.launch { state.show() } },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(onClick = { askDiscard = false; scope.launch { state.show() } }) { Text(stringResource(R.string.discard_keep)) }
            },
            dismissButton = {
                TextButton(onClick = { askDiscard = false; onDismiss() }) {
                    Text(stringResource(R.string.discard_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
