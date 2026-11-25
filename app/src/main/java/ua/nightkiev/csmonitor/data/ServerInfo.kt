package ua.nightkiev.csmonitor.data

data class ServerInfo(
    val isOnline: Boolean,
    val serverName: String = "",
    val mapName: String = "",
    val players: Int = 0,
    val maxPlayers: Int = 0,
    val pingMs: Long = 0
) {
    val playersText: String
        get() = if (isOnline) "$players/$maxPlayers" else "—/—"
}

data class LogEntry(
    val timestamp: Long,
    val message: String
)
