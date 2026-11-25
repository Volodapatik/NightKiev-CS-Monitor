package ua.nightkiev.csmonitor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ua.nightkiev.csmonitor.data.ServerInfo
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class A2SClient(
    private val timeoutMs: Int = 5000
) {

    companion object {
        // A2S_INFO request: FF FF FF FF T "Source Engine Query" 00
        private val A2S_INFO_REQUEST = byteArrayOf(
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x54, // 'T'
            'S'.code.toByte(), 'o'.code.toByte(), 'u'.code.toByte(), 'r'.code.toByte(),
            'c'.code.toByte(), 'e'.code.toByte(), ' '.code.toByte(),
            'E'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte(), 'i'.code.toByte(),
            'n'.code.toByte(), 'e'.code.toByte(), ' '.code.toByte(),
            'Q'.code.toByte(), 'u'.code.toByte(), 'e'.code.toByte(), 'r'.code.toByte(),
            'y'.code.toByte(), 0x00
        )
    }

    suspend fun query(ip: String, port: Int): ServerInfo = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val result = withTimeoutOrNull(timeoutMs.toLong()) {
                doQuery(ip, port)
            }
            if (result != null) {
                result.copy(pingMs = System.currentTimeMillis() - start)
            } else {
                ServerInfo(isOnline = false)
            }
        } catch (e: Exception) {
            ServerInfo(isOnline = false)
        }
    }

    private fun doQuery(ip: String, port: Int): ServerInfo {
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val address = InetAddress.getByName(ip)

            val request = DatagramPacket(A2S_INFO_REQUEST, A2S_INFO_REQUEST.size, address, port)
            socket.send(request)

            val buffer = ByteArray(1400)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)

            return parseResponse(buffer, response.length)
        }
    }

    private fun parseResponse(data: ByteArray, length: Int): ServerInfo {
        if (length < 5) return ServerInfo(isOnline = false)

        // Skip header 0xFFFFFFFF
        var offset = 4
        val header = data[offset].toInt() and 0xFF
        offset++

        return when (header) {
            'I'.code -> parseSource(data, offset, length)
            'm'.code -> parseGoldSrc(data, offset, length)
            else -> ServerInfo(isOnline = false)
        }
    }

    private fun parseSource(data: ByteArray, start: Int, length: Int): ServerInfo {
        var offset = start
        // Protocol version
        offset++

        val name = readString(data, offset, length).also { offset = it.second }.first
        val map = readString(data, offset, length).also { offset = it.second }.first
        // folder
        offset = readString(data, offset, length).second
        // game
        offset = readString(data, offset, length).second
        // appid short
        offset += 2
        val players = data[offset].toInt() and 0xFF
        offset++
        val maxPlayers = data[offset].toInt() and 0xFF

        return ServerInfo(
            isOnline = true,
            serverName = name,
            mapName = map,
            players = players,
            maxPlayers = maxPlayers
        )
    }

    private fun parseGoldSrc(data: ByteArray, start: Int, length: Int): ServerInfo {
        var offset = start
        // address string
        offset = readString(data, offset, length).second
        val name = readString(data, offset, length).also { offset = it.second }.first
        val map = readString(data, offset, length).also { offset = it.second }.first
        // folder
        offset = readString(data, offset, length).second
        // game
        offset = readString(data, offset, length).second
        val players = data[offset].toInt() and 0xFF
        offset++
        val maxPlayers = data[offset].toInt() and 0xFF

        return ServerInfo(
            isOnline = true,
            serverName = name,
            mapName = map,
            players = players,
            maxPlayers = maxPlayers
        )
    }

    private fun readString(data: ByteArray, offset: Int, length: Int): Pair<String, Int> {
        var end = offset
        while (end < length && data[end] != 0.toByte()) end++
        val str = if (end > offset) {
            String(data, offset, end - offset, Charset.forName("UTF-8"))
        } else ""
        return str to (end + 1)
    }
}
