package com.maverick.focuswindow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------- palette ----------
 * Slate board, pine for open, plum for closed, marigold for "you are here".
 * Same identity as the planner, so the two feel like one product.
 */
val Ink = Color(0xFF101A22)
val Surface = Color(0xFF1A2833)
val Board = Color(0xFF223341)
val Paper = Color(0xFFE8EEF2)
val Muted = Color(0xFF8399A8)
val Pine = Color(0xFF35A47F)
val PineDeep = Color(0xFF24705A)
val Plum = Color(0xFF7C3A52)
val Marigold = Color(0xFFE3A126)

private val Scheme = darkColorScheme(
    primary = Pine,
    onPrimary = Ink,
    secondary = Marigold,
    background = Ink,
    onBackground = Paper,
    surface = Surface,
    onSurface = Paper,
    error = Plum
)

@Composable
fun FocusWindowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

/**
 * The signature piece: one whole day drawn as a strip from midnight to midnight.
 * Green blocks are your open windows, the gold line is right now.
 */
@Composable
fun DayRibbon(
    windows: List<TimeWindow>,
    nowMinutes: Int?,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            val w = size.width
            val h = size.height
            val radius = CornerRadius(14f, 14f)

            drawRoundRect(color = Board, size = Size(w, h), cornerRadius = radius)

            windows.filter { it.valid }.forEach { window ->
                val left = (window.start / 1440f) * w
                val width = ((window.end - window.start) / 1440f) * w
                drawRect(
                    color = Pine,
                    topLeft = Offset(left, 0f),
                    size = Size(width, h)
                )
            }

            if (nowMinutes != null) {
                val x = (nowMinutes / 1440f) * w
                drawRect(
                    color = Marigold,
                    topLeft = Offset(x - 1.5f, 0f),
                    size = Size(3f, h)
                )
                drawCircle(color = Marigold, radius = 7f, center = Offset(x, 0f))
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth()) {
            listOf("12a", "6a", "12p", "6p", "12a").forEachIndexed { index, label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Muted,
                    modifier = Modifier.weight(if (index == 4) 0.0001f else 1f)
                )
            }
        }
    }
}

/** Restarts the watcher after the phone reboots, so a restart isn't a loophole. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Store.isEnabled(context)) {
            BlockerService.start(context)
        }
    }
}
