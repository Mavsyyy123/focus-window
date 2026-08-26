package com.maverick.focuswindow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * The engine. Runs quietly in the background, checks once a second which app is on
 * screen, and throws up the block screen when that app is off-limits right now.
 */
class BlockerService : Service() {

    companion object {
        const val CHANNEL_ID = "focus_window_running"
        const val NOTIFICATION_ID = 42
        private const val CHECK_INTERVAL_MS = 900L
        private const val REBLOCK_COOLDOWN_MS = 1500L

        fun start(context: Context) {
            val intent = Intent(context, BlockerService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BlockerService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastKnownForeground: String? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockTime = 0L

    private val ticker = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startInForeground()
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* ---------- the loop ---------- */

    private fun checkForegroundApp() {
        if (!Store.isEnabled(this)) return

        val current = readForegroundPackage() ?: return
        if (current == packageName) return
        if (!Store.shouldBlock(this, current)) return

        val now = System.currentTimeMillis()
        val sameAppRecently =
            current == lastBlockedPackage && now - lastBlockTime < REBLOCK_COOLDOWN_MS
        if (sameAppRecently) return

        lastBlockedPackage = current
        lastBlockTime = now

        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockActivity.EXTRA_PACKAGE, current)
        }
        startActivity(intent)
    }

    /**
     * Asks Android which app most recently came to the front. Returns the last
     * known value if nothing changed in the lookback window.
     */
    private fun readForegroundPackage(): String? {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usage.queryEvents(end - 10_000, end)
        val event = UsageEvents.Event()
        var latest: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latest = event.packageName
            }
        }

        if (latest != null) lastKnownForeground = latest
        return lastKnownForeground
    }

    /* ---------- the permanent notification Android requires ---------- */

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Window running",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows while your schedule is being enforced."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Window is on")
            .setContentText("Watching your blocked apps")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun startInForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }
}
