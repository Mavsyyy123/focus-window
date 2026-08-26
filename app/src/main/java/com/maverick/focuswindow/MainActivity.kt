package com.maverick.focuswindow

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusWindowTheme {
                AppRoot()
            }
        }
    }
}

private enum class Tab(val label: String) {
    TODAY("Today"), SCHEDULE("Schedule"), APPS("Apps")
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.TODAY) }

    // Everything reads from Store; this counter forces a refresh after a change.
    var version by remember { mutableIntStateOf(0) }
    var clock by remember { mutableIntStateOf(Store.nowMinutes()) }
    var permissionsOk by remember { mutableStateOf(Permissions.allGranted(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            clock = Store.nowMinutes()
            permissionsOk = Permissions.allGranted(context)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(containerColor = Ink) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Header()
            Spacer(Modifier.height(16.dp))
            TabBar(current = tab, onSelect = { tab = it })
            Spacer(Modifier.height(18.dp))

            when (tab) {
                Tab.TODAY -> TodayTab(
                    version = version,
                    clock = clock,
                    permissionsOk = permissionsOk,
                    onChanged = { version++ }
                )
                Tab.SCHEDULE -> ScheduleTab(version = version, onChanged = { version++ })
                Tab.APPS -> AppsTab(version = version, onChanged = { version++ })
            }
        }
    }
}

/* ---------------------------------------------------------------- header */

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "FOCUS WINDOW",
            fontSize = 12.sp,
            letterSpacing = 2.4.sp,
            fontWeight = FontWeight.SemiBold,
            color = Muted
        )
        Text(
            Store.DAY_NAMES[Store.todayIndex()],
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            color = Muted
        )
    }
}

@Composable
private fun TabBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tab.entries.forEach { entry ->
            val selected = entry == current
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) Paper else Color.Transparent)
                    .clickable { onSelect(entry) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entry.label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Ink else Muted
                )
            }
        }
    }
}

/* ---------------------------------------------------------------- today */

@Composable
private fun TodayTab(
    version: Int,
    clock: Int,
    permissionsOk: Boolean,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    val enabled = remember(version) { Store.isEnabled(context) }
    val windows = remember(version, clock) { Store.todayWindows(context) }
    val open = remember(version, clock) { Store.isOpenNow(context) }
    val boundary = remember(version, clock) { Store.nextBoundary(context) }
    val blockedCount = remember(version) { Store.blockedApps(context).size }

    Column(Modifier.verticalScroll(scroll)) {

        if (!permissionsOk) {
            SetupCard()
            Spacer(Modifier.height(14.dp))
        }

        // Status block
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (open) PineDeep else Plum)
                .padding(24.dp)
        ) {
            Text(
                if (!enabled) "Off" else if (open) "Allowed" else "Blocked",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Paper,
                letterSpacing = (-1.5).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    !enabled -> "Nothing is being blocked right now"
                    boundary == null && open -> "Open for the rest of today"
                    boundary == null -> "Nothing left open today"
                    open -> "Closes ${minutesToLabel(boundary)} · in ${countdownLabel(clock, boundary)}"
                    else -> "Opens ${minutesToLabel(boundary)} · in ${countdownLabel(clock, boundary)}"
                },
                fontSize = 15.sp,
                color = Paper.copy(alpha = 0.85f)
            )
        }

        Spacer(Modifier.height(18.dp))

        DayRibbon(windows = windows, nowMinutes = clock)

        Spacer(Modifier.height(22.dp))

        // Master switch
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Enforce my schedule", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Paper)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (blockedCount == 0) "Pick apps in the Apps tab first"
                    else "$blockedCount app${if (blockedCount == 1) "" else "s"} on the list",
                    fontSize = 13.sp,
                    color = Muted
                )
            }
            Switch(
                checked = enabled,
                enabled = permissionsOk && blockedCount > 0,
                onCheckedChange = { on ->
                    Store.setEnabled(context, on)
                    if (on) BlockerService.start(context) else BlockerService.stop(context)
                    onChanged()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink,
                    checkedTrackColor = Pine,
                    uncheckedThumbColor = Muted,
                    uncheckedTrackColor = Board
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "You can always turn this off or uninstall the app. It adds friction, not a cage — that pause is usually enough.",
            fontSize = 13.sp,
            color = Muted,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SetupCard() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Marigold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(18.dp)
    ) {
        Text("Two permissions to go", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Marigold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Android keeps these behind Settings on purpose. Grant both and blocking starts working.",
            fontSize = 13.sp,
            color = Muted,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(14.dp))

        PermissionRow(
            title = "Usage access",
            why = "Lets the app see which app is on screen",
            granted = Permissions.hasUsageAccess(context)
        ) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        Spacer(Modifier.height(10.dp))

        PermissionRow(
            title = "Display over other apps",
            why = "Lets the block screen appear on top",
            granted = Permissions.hasOverlay(context)
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }
}

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (granted) Pine else Marigold)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Paper)
            Text(why, fontSize = 12.sp, color = Muted)
        }
        if (granted) {
            Text("Done", fontSize = 13.sp, color = Pine)
        } else {
            TextButton(onClick = onOpen) {
                Text("Open", fontSize = 13.sp, color = Marigold, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ---------------------------------------------------------------- schedule */

@Composable
private fun ScheduleTab(version: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    var day by remember { mutableIntStateOf(Store.todayIndex()) }
    val schedule = remember(version) { Store.schedule(context) }
    val windows = schedule[day].orEmpty().sortedBy { it.start }
    val scroll = rememberScrollState()

    fun replaceDay(newWindows: List<TimeWindow>) {
        val updated = schedule.toMutableMap()
        updated[day] = newWindows.sortedBy { it.start }
        Store.setSchedule(context, updated)
        onChanged()
    }

    Column(Modifier.verticalScroll(scroll)) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Store.DAY_SHORT.forEachIndexed { index, letter ->
                val selected = index == day
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) Paper else Surface)
                        .clickable { day = index },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            letter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) Ink else Muted
                        )
                        if (index == Store.todayIndex()) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Marigold)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        DayRibbon(
            windows = windows,
            nowMinutes = if (day == Store.todayIndex()) Store.nowMinutes() else null
        )

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(Store.DAY_NAMES[day], fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Paper)
            val total = windows.sumOf { maxOf(0, it.end - it.start) }
            Text(
                if (total == 0) "0m open" else "${total / 60}h ${total % 60}m open",
                fontSize = 12.sp,
                color = Muted
            )
        }

        Spacer(Modifier.height(10.dp))

        if (windows.isEmpty()) {
            Text(
                "No open windows. Your blocked apps stay closed all day.",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        windows.forEachIndexed { index, window ->
            HorizontalDivider(color = Board)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeChip(window.start) { picked ->
                    replaceDay(windows.toMutableList().also { it[index] = window.copy(start = picked) })
                }
                Text("  to  ", fontSize = 13.sp, color = Muted)
                TimeChip(window.end) { picked ->
                    replaceDay(windows.toMutableList().also { it[index] = window.copy(end = picked) })
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    replaceDay(windows.filterIndexed { i, _ -> i != index })
                }) {
                    Text("Remove", fontSize = 13.sp, color = Plum)
                }
            }
            if (!window.valid) {
                Text(
                    "End must be after start. For a window past midnight, make two windows.",
                    fontSize = 12.sp,
                    color = Marigold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        HorizontalDivider(color = Board)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val last = windows.lastOrNull()
                    val start = ((last?.end ?: (12 * 60 - 60)) + 60).coerceAtMost(22 * 60)
                    replaceDay(windows + TimeWindow(start, (start + 60).coerceAtMost(1439)))
                },
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Paper, contentColor = Ink)
            ) { Text("Add window", fontSize = 14.sp) }

            Button(
                onClick = {
                    val copied = mutableMapOf<Int, List<TimeWindow>>()
                    for (d in 0..6) copied[d] = windows
                    Store.setSchedule(context, copied)
                    onChanged()
                },
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Paper)
            ) { Text("Copy to week", fontSize = 14.sp) }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun TimeChip(minutes: Int, onPicked: (Int) -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Surface)
            .clickable {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onPicked(hour * 60 + minute) },
                    minutes / 60,
                    minutes % 60,
                    false
                ).show()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(minutesToLabel(minutes), fontSize = 14.sp, color = Paper, fontWeight = FontWeight.Medium)
    }
}

/* ---------------------------------------------------------------- apps */

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

@Composable
private fun AppsTab(version: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember(version) { mutableStateOf(Store.blockedApps(context)) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
        loading = false
    }

    Column {
        Text(
            "Pick the apps that eat your day. Everything else stays untouched.",
            fontSize = 13.sp,
            color = Muted,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(14.dp))

        if (loading) {
            Text("Reading your app list…", fontSize = 14.sp, color = Muted)
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(apps, key = { it.packageName }) { app ->
                val checked = app.packageName in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (checked) Surface else Color.Transparent)
                        .clickable {
                            selected = if (checked) selected - app.packageName
                            else selected + app.packageName
                            Store.setBlockedApps(context, selected)
                            onChanged()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.icon != null) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Board))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        app.label,
                        fontSize = 15.sp,
                        color = Paper,
                        fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Pine,
                            uncheckedColor = Board,
                            checkmarkColor = Ink
                        )
                    )
                }
            }
        }
    }
}

private fun loadApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcherIntent, 0)
        .mapNotNull { resolved ->
            val pkg = resolved.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val label = resolved.loadLabel(pm).toString()
            val icon: ImageBitmap? = try {
                resolved.loadIcon(pm).toImageBitmap()
            } catch (e: Exception) {
                null
            }
            InstalledApp(pkg, label, icon)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun Drawable.toImageBitmap(): ImageBitmap =
    toBitmap(width = 96, height = 96).asImageBitmap()
