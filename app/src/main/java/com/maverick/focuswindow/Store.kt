package com.maverick.focuswindow

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * One stretch of time when your blocked apps are allowed to open.
 * Times are stored as minutes after midnight, so 12:30pm = 750.
 */
data class TimeWindow(val start: Int, val end: Int) {
    val valid: Boolean get() = end > start
}

object Store {

    private const val PREFS = "focus_window_prefs"
    private const val KEY_SCHEDULE = "schedule"
    private const val KEY_APPS = "blocked_apps"
    private const val KEY_ENABLED = "enabled"

    val DAY_NAMES = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )
    val DAY_SHORT = listOf("S", "M", "T", "W", "T", "F", "S")

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /* ---------- on / off ---------- */

    fun isEnabled(c: Context): Boolean = prefs(c).getBoolean(KEY_ENABLED, false)

    fun setEnabled(c: Context, value: Boolean) {
        prefs(c).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    /* ---------- which apps get blocked ---------- */

    fun blockedApps(c: Context): Set<String> =
        prefs(c).getStringSet(KEY_APPS, emptySet())?.toSet() ?: emptySet()

    fun setBlockedApps(c: Context, apps: Set<String>) {
        prefs(c).edit().putStringSet(KEY_APPS, apps).apply()
    }

    /* ---------- the weekly schedule ---------- */

    fun schedule(c: Context): Map<Int, List<TimeWindow>> {
        val raw = prefs(c).getString(KEY_SCHEDULE, null) ?: return defaultSchedule()
        return try {
            decode(raw)
        } catch (e: Exception) {
            defaultSchedule()
        }
    }

    fun setSchedule(c: Context, schedule: Map<Int, List<TimeWindow>>) {
        prefs(c).edit().putString(KEY_SCHEDULE, encode(schedule)).apply()
    }

    fun defaultSchedule(): Map<Int, List<TimeWindow>> {
        val map = mutableMapOf<Int, List<TimeWindow>>()
        for (day in 0..6) {
            val weekend = day == 0 || day == 6
            map[day] = if (weekend) {
                listOf(TimeWindow(10 * 60, 12 * 60), TimeWindow(19 * 60, 21 * 60))
            } else {
                listOf(TimeWindow(12 * 60, 13 * 60), TimeWindow(19 * 60, 20 * 60))
            }
        }
        return map
    }

    private fun encode(schedule: Map<Int, List<TimeWindow>>): String {
        val root = JSONObject()
        for (day in 0..6) {
            val arr = JSONArray()
            schedule[day].orEmpty().forEach { w ->
                arr.put(JSONObject().put("s", w.start).put("e", w.end))
            }
            root.put(day.toString(), arr)
        }
        return root.toString()
    }

    private fun decode(raw: String): Map<Int, List<TimeWindow>> {
        val root = JSONObject(raw)
        val map = mutableMapOf<Int, List<TimeWindow>>()
        for (day in 0..6) {
            val arr = root.optJSONArray(day.toString()) ?: JSONArray()
            val list = mutableListOf<TimeWindow>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(TimeWindow(o.getInt("s"), o.getInt("e")))
            }
            map[day] = list.sortedBy { it.start }
        }
        return map
    }

    /* ---------- what time is it, and am I allowed ---------- */

    fun todayIndex(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

    fun nowMinutes(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    fun todayWindows(c: Context): List<TimeWindow> =
        schedule(c)[todayIndex()].orEmpty().filter { it.valid }.sortedBy { it.start }

    /** True when right now falls inside one of today's open windows. */
    fun isOpenNow(c: Context): Boolean {
        val now = nowMinutes()
        return todayWindows(c).any { now >= it.start && now < it.end }
    }

    /** The next minute-of-day when the status flips, or null if nothing is left today. */
    fun nextBoundary(c: Context): Int? {
        val now = nowMinutes()
        val windows = todayWindows(c)
        val current = windows.firstOrNull { now >= it.start && now < it.end }
        if (current != null) return current.end
        return windows.firstOrNull { it.start > now }?.start
    }

    /** The one question the service asks over and over. */
    fun shouldBlock(c: Context, packageName: String): Boolean =
        isEnabled(c) && packageName in blockedApps(c) && !isOpenNow(c)
}

/* ---------- formatting helpers used all over the UI ---------- */

fun minutesToLabel(minutes: Int): String {
    val hour24 = (minutes / 60) % 24
    val mins = minutes % 60
    val suffix = if (hour24 >= 12) "pm" else "am"
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    return "%d:%02d %s".format(hour12, mins, suffix)
}

fun countdownLabel(fromMinutes: Int, toMinutes: Int): String {
    val gap = toMinutes - fromMinutes
    if (gap <= 0) return "now"
    val hours = gap / 60
    val mins = gap % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

/* ---------- permission checks ---------- */

object Permissions {

    /** Usage access lets the app see which app is currently on screen. */
    fun hasUsageAccess(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            c.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Drawing over other apps is what lets the block screen appear on top. */
    fun hasOverlay(c: Context): Boolean = Settings.canDrawOverlays(c)

    fun allGranted(c: Context): Boolean = hasUsageAccess(c) && hasOverlay(c)
}
