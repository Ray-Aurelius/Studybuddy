package com.rayaurelius.sophinity.ui.components

import com.rayaurelius.sophinity.ui.i18n.AppLocale
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.random.Random

/** Built-in backgrounds, drawn in code so they stay crisp at any size and cost nothing to ship. */
object Backgrounds {
    const val NONE = "none"
    const val PHOTO_PREFIX = "photo:"
    val builtIn = listOf("marble", "aegean", "dawn", "night", "keys")

    fun isPhoto(id: String) = id.startsWith(PHOTO_PREFIX)

    private fun photoFile(context: Context) = File(context.filesDir, "backgrounds/photo.jpg")

    /**
     * Copies the chosen image into app storage (downscaled to at most 2048px) so it survives the
     * original being moved or deleted, and is included in Android backup. Returns the new setting id.
     */
    suspend fun importPhoto(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val bmp = resolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error(AppLocale.localized(context).getString(com.rayaurelius.sophinity.R.string.err_image))
        val out = photoFile(context)
        out.parentFile?.mkdirs()
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bmp.recycle()
        PHOTO_PREFIX + System.currentTimeMillis()
    }

    fun deletePhoto(context: Context) { photoFile(context).delete() }

    @Composable
    fun loadPhoto(id: String): Bitmap? {
        val context = LocalContext.current
        val bmp by produceState<Bitmap?>(null, id) {
            value = withContext(Dispatchers.IO) {
                photoFile(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
            }
        }
        return bmp
    }
}

/**
 * Draws the chosen background behind the app, with a theme-colored veil on top so text and
 * cards stay readable. [dim] 0 = background at full strength, 0.9 = barely visible.
 */
@Composable
fun AppBackground(id: String, dim: Float, blurDp: Float, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(base)) {
        when {
            id == Backgrounds.NONE -> return@Box
            Backgrounds.isPhoto(id) -> {
                val bmp = Backgrounds.loadPhoto(id) ?: return@Box
                Image(
                    bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().then(
                        if (blurDp > 0f && Build.VERSION.SDK_INT >= 31) Modifier.blur(blurDp.dp) else Modifier
                    ),
                )
            }
            else -> BuiltInBackground(id, Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(base.copy(alpha = dim.coerceIn(0f, 0.95f))))
    }
}

@Composable
fun BuiltInBackground(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (id) {
            "marble" -> marble()
            "aegean" -> aegean()
            "dawn" -> drawRect(Brush.verticalGradient(listOf(Color(0xFFFFD6A5), Color(0xFFFFB4A2), Color(0xFFE5989B), Color(0xFFB5838D))))
            "night" -> night()
            "keys" -> keys()
        }
    }
}

private fun DrawScope.marble() {
    drawRect(Brush.linearGradient(listOf(Color(0xFFF7F5F0), Color(0xFFE9E6DF), Color(0xFFF4F1EA))))
    val rnd = Random(11)
    repeat(9) { i ->
        val p = Path()
        var x = rnd.nextFloat() * size.width
        var y = -20f
        p.moveTo(x, y)
        while (y < size.height + 40) {
            val nx = x + (rnd.nextFloat() - 0.45f) * size.width * 0.25f
            val ny = y + size.height * (0.08f + rnd.nextFloat() * 0.1f)
            p.quadraticTo((x + nx) / 2 + (rnd.nextFloat() - 0.5f) * 80f, (y + ny) / 2, nx, ny)
            x = nx; y = ny
        }
        drawPath(p, Color(0xFF9C978C).copy(alpha = 0.10f + 0.12f * (i % 3) / 2f), style = Stroke(width = 1.5f + (i % 4) * 1.6f))
    }
}

private fun DrawScope.aegean() {
    drawRect(Brush.verticalGradient(0f to Color(0xFF9FD3FF), 0.55f to Color(0xFFE6F4FF), 0.56f to Color(0xFF2F9BFF), 1f to Color(0xFF0B5FAE)))
    // Sun and a few gentle wave lines.
    drawCircle(Color(0xFFFFE58A).copy(alpha = 0.8f), radius = size.minDimension * 0.08f, center = Offset(size.width * 0.75f, size.height * 0.42f))
    for (i in 0 until 6) {
        val y = size.height * (0.6f + i * 0.065f)
        val p = Path()
        p.moveTo(0f, y)
        var x = 0f
        val w = size.width / 6
        while (x < size.width) {
            p.quadraticTo(x + w / 4, y - 6f, x + w / 2, y)
            p.quadraticTo(x + 3 * w / 4, y + 6f, x + w, y)
            x += w
        }
        drawPath(p, Color.White.copy(alpha = 0.25f), style = Stroke(2f))
    }
}

private fun DrawScope.night() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1026), Color(0xFF1B2452), Color(0xFF2E3A7A))))
    val rnd = Random(3)
    repeat(160) {
        val r = 0.6f + rnd.nextFloat() * 1.8f
        drawCircle(Color.White.copy(alpha = 0.3f + rnd.nextFloat() * 0.6f), r, Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height))
    }
}

private fun DrawScope.keys() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFEAF4FF), Color(0xFFD6E9FF))))
    val u = 9.dp.toPx()
    val color = Color(0xFF2F9BFF).copy(alpha = 0.16f)
    var row = 0f
    while (row < size.height) {
        var x0 = 0f
        val p = Path()
        while (x0 < size.width) {
            p.moveTo(x0, row + 6 * u); p.lineTo(x0, row + u); p.lineTo(x0 + 5 * u, row + u); p.lineTo(x0 + 5 * u, row + 5 * u)
            p.lineTo(x0 + 2 * u, row + 5 * u); p.lineTo(x0 + 2 * u, row + 3 * u); p.lineTo(x0 + 3 * u, row + 3 * u)
            x0 += 6 * u
        }
        drawPath(p, color, style = Stroke(u * 0.7f))
        drawLine(color, Offset(0f, row + 6 * u), Offset(size.width, row + 6 * u), strokeWidth = u * 0.7f)
        row += 14 * u
    }
}
