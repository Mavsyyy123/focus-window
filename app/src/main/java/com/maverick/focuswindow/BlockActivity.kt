package com.maverick.focuswindow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * What you actually see when you try to open a blocked app. Deliberately plain,
 * a little cold, and one tap away from putting the phone down.
 */
class BlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)

        setContent {
            FocusWindowTheme {
                BlockScreen(
                    appLabel = blockedPackage?.let { labelFor(it) } ?: "That app",
                    onDismiss = { goHome() }
                )
            }
        }
    }

    private fun labelFor(packageName: String): String = try {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        packageName
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }
}

@Composable
private fun BlockScreen(appLabel: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }

    // Recheck every second so the screen closes itself the moment a window opens.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
            if (Store.isOpenNow(context) || !Store.isEnabled(context)) {
                onDismiss()
                break
            }
        }
    }

    val nextOpen = remember(tick) { Store.nextBoundary(context) }
    val now = remember(tick) { Store.nowMinutes() }

    // Back button should also just send you home, not back into the app.
    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Plum),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Not now",
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Paper,
                letterSpacing = (-1.5).sp
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "$appLabel is closed until your next window.",
                fontSize = 16.sp,
                color = Paper.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = if (nextOpen != null) {
                    "Opens ${minutesToLabel(nextOpen)}  ·  in ${countdownLabel(now, nextOpen)}"
                } else {
                    "Nothing left open today"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Marigold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(44.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Paper,
                    contentColor = Plum
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Fine, close it",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
