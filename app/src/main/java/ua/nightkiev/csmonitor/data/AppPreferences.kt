package ua.nightkiev.csmonitor.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverIp: String
        get() = prefs.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var serverPort: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    var checkIntervalSec: Int
        get() = prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_INTERVAL, value).apply()

    var maxAttempts: Int
        get() = prefs.getInt(KEY_ATTEMPTS, DEFAULT_ATTEMPTS)
        set(value) = prefs.edit().putInt(KEY_ATTEMPTS, value).apply()

    var autoStartOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var isMonitoring: Boolean
        get() = prefs.getBoolean(KEY_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_MONITORING, value).apply()

    fun addLog(message: String) {
        val list = getLogs().toMutableList()
        list.add(0, LogEntry(System.currentTimeMillis(), message))
        if (list.size > MAX_LOGS) list.subList(MAX_LOGS, list.size).clear()
        saveLogs(list)
    }

    fun getLogs(): List<LogEntry> {
        val json = prefs.getString(KEY_LOGS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                LogEntry(obj.getLong("ts"), obj.getString("msg"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLogs(logs: List<LogEntry>) {
        val arr = JSONArray()
        logs.forEach { entry ->
            arr.put(JSONObject().apply {
                put("ts", entry.timestamp)
                put("msg", entry.message)
            })
        }
        prefs.edit().putString(KEY_LOGS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "nightkiev_monitor"
        private const val KEY_IP = "server_ip"
        private const val KEY_PORT = "server_port"
        private const val KEY_INTERVAL = "check_interval"
        private const val KEY_ATTEMPTS = "max_attempts"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_MONITORING = "is_monitoring"
        private const val KEY_LOGS = "logs"

        const val DEFAULT_IP = "91.211.118.84"
        const val DEFAULT_PORT = 27035
        const val DEFAULT_INTERVAL = 15
        const val DEFAULT_ATTEMPTS = 3
        const val MAX_LOGS = 50
        const val TIMEOUT_MS = 5000
    }
}
