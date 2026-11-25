package ua.nightkiev.csmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import ua.nightkiev.csmonitor.MainActivity
import ua.nightkiev.csmonitor.R
import ua.nightkiev.csmonitor.data.AppPreferences
import ua.nightkiev.csmonitor.data.ServerInfo
import ua.nightkiev.csmonitor.network.A2SClient

class MonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var prefs: AppPreferences
    private val client = A2SClient(AppPreferences.TIMEOUT_MS)

    private var isOnline: Boolean? = null
    private var consecutiveFails = 0
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildPersistentNotification("—")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        prefs.isMonitoring = true
        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                performCheck()
                delay(prefs.checkIntervalSec * 1000L)
            }
        }
    }

    private suspend fun performCheck() {
        val info = client.query(prefs.serverIp, prefs.serverPort)
        if (info.isOnline) {
            consecutiveFails = 0
            handleStatusChange(true, info)
        } else {
            consecutiveFails++
            if (consecutiveFails >= prefs.maxAttempts) {
                handleStatusChange(false, info)
            }
            // else keep previous status (anti-spam)
        }
    }

    private fun handleStatusChange(online: Boolean, info: ServerInfo) {
        val previous = isOnline
        isOnline = online

        val statusText = if (online) getString(R.string.status_online) else getString(R.string.status_offline)
        updatePersistentNotification(statusText)

        if (previous != null && previous != online) {
            if (online) {
                sendStatusNotification(getString(R.string.notification_online))
                prefs.addLog("🟢 Сервер знову онлайн. Карта: ${info.mapName}, гравці: ${info.playersText}")
            } else {
                sendStatusNotification(getString(R.string.notification_offline))
                prefs.addLog("🔴 Сервер тимчасово не відповідає")
            }
        } else if (previous == null) {
            prefs.addLog(if (online) "Старт: Онлайн (${info.mapName})" else "Старт: Офлайн")
        } else if (online) {
            // just update log occasionally? skip to avoid spam
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_description)
            enableVibration(true)
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val persistent = NotificationChannel(
            PERSISTENT_CHANNEL_ID,
            "Постійний моніторинг",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Постійне сповіщення про роботу моніторингу"
            setShowBadge(false)
        }
        manager.createNotificationChannel(persistent)
    }

    private fun buildPersistentNotification(status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.monitoring_active, status))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updatePersistentNotification(status: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildPersistentNotification(status))
    }

    private fun sendStatusNotification(text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(STATUS_NOTIFICATION_ID, notification)

        // Vibration
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(400)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        prefs.isMonitoring = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val STATUS_NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "cs_monitor_status"
        const val PERSISTENT_CHANNEL_ID = "cs_monitor_persistent"

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
