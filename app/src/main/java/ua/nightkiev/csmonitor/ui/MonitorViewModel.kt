package ua.nightkiev.csmonitor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.nightkiev.csmonitor.data.AppPreferences
import ua.nightkiev.csmonitor.data.LogEntry
import ua.nightkiev.csmonitor.data.ServerInfo
import ua.nightkiev.csmonitor.network.A2SClient
import ua.nightkiev.csmonitor.service.MonitorService

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val client = A2SClient()

    private val _serverInfo = MutableStateFlow(ServerInfo(isOnline = false))
    val serverInfo: StateFlow<ServerInfo> = _serverInfo.asStateFlow()

    private val _isMonitoring = MutableStateFlow(prefs.isMonitoring)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _logs = MutableStateFlow(prefs.getLogs())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Settings
    private val _ip = MutableStateFlow(prefs.serverIp)
    val ip: StateFlow<String> = _ip.asStateFlow()

    private val _port = MutableStateFlow(prefs.serverPort.toString())
    val port: StateFlow<String> = _port.asStateFlow()

    private val _interval = MutableStateFlow(prefs.checkIntervalSec.toString())
    val interval: StateFlow<String> = _interval.asStateFlow()

    private val _attempts = MutableStateFlow(prefs.maxAttempts.toString())
    val attempts: StateFlow<String> = _attempts.asStateFlow()

    private val _autoStart = MutableStateFlow(prefs.autoStartOnBoot)
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    fun refreshOnce() {
        viewModelScope.launch {
            val info = client.query(prefs.serverIp, prefs.serverPort)
            _serverInfo.value = info
        }
    }

    fun startMonitoring() {
        MonitorService.start(getApplication())
        _isMonitoring.value = true
        prefs.isMonitoring = true
        refreshLogs()
    }

    fun stopMonitoring() {
        MonitorService.stop(getApplication())
        _isMonitoring.value = false
        prefs.isMonitoring = false
        refreshLogs()
    }

    fun saveSettings(
        ip: String,
        port: String,
        interval: String,
        attempts: String,
        autoStart: Boolean
    ) {
        prefs.serverIp = ip.trim()
        prefs.serverPort = port.toIntOrNull() ?: AppPreferences.DEFAULT_PORT
        prefs.checkIntervalSec = interval.toIntOrNull()?.coerceAtLeast(5) ?: AppPreferences.DEFAULT_INTERVAL
        prefs.maxAttempts = attempts.toIntOrNull()?.coerceIn(1, 10) ?: AppPreferences.DEFAULT_ATTEMPTS
        prefs.autoStartOnBoot = autoStart

        _ip.value = prefs.serverIp
        _port.value = prefs.serverPort.toString()
        _interval.value = prefs.checkIntervalSec.toString()
        _attempts.value = prefs.maxAttempts.toString()
        _autoStart.value = prefs.autoStartOnBoot
    }

    fun refreshLogs() {
        _logs.value = prefs.getLogs()
    }

    fun updateIp(value: String) { _ip.value = value }
    fun updatePort(value: String) { _port.value = value }
    fun updateInterval(value: String) { _interval.value = value }
    fun updateAttempts(value: String) { _attempts.value = value }
    fun updateAutoStart(value: Boolean) { _autoStart.value = value }
}
