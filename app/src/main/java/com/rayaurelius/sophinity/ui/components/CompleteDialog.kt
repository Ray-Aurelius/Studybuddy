package com.rayaurelius.sophinity.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.domain.StudyMath

/**
 * Confirms "Mark as completed" and says exactly what it does: the item moves to Finished and
 * [creditSec] is added so it counts as its full length ([totalSec]). Nothing happens until confirmed,
 * since a long audiobook can add hours.
 */
@Composable
fun CompleteDialog(title: String, creditSec: Long, totalSec: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.TaskAlt, null) },
        title = { Text(stringResource(R.string.complete_title)) },
        text = {
            Text(
                if (creditSec > 0) stringResource(R.string.complete_body, title, StudyMath.formatDuration((creditSec + 30) / 60 * 60), StudyMath.formatDuration(totalSec))
                else stringResource(R.string.complete_body_none, title)
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.complete_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
