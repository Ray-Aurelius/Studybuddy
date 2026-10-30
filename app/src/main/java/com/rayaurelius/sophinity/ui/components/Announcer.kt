package com.rayaurelius.sophinity.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Short confirmations ("Habits of Curious People completed. 7h 30m added."), shown as an in-app
 * snackbar in the app's own style, with an optional action such as Undo. The scope belongs to the
 * app shell, so a message still shows (and its Undo still works) after the sheet that triggered
 * it has closed. Outside the shell (previews, tests of a lone component) it falls back to a toast.
 */
class Announcer(private val host: SnackbarHostState?, private val scope: CoroutineScope?, private val context: Context) {
    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        if (host == null || scope == null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show(); return
        }
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(
                message, actionLabel = actionLabel, withDismissAction = actionLabel == null,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalAnnouncer = staticCompositionLocalOf<Announcer?> { null }

@Composable
fun rememberAnnouncer(): Announcer = LocalAnnouncer.current ?: Announcer(null, null, LocalContext.current)
