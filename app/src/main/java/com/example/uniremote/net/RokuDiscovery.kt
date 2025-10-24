package com.example.uniremote.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

// Data class for discovered Roku device
data class RokuDevice(
    val ip: String,
    val location: String,
    val name: String?,
    val model: String?
)

object RokuDiscovery {
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private val SSDP_PAYLOAD = """M-SEARCH * HTTP/1.1
HOST: 239.255.255.250:1900
MAN: "ssdp:discover"
MX: 2
ST: roku:ecp

""".replace("\n", "\r\n")
    private val LOCATION_HEADER = Regex("""(?im)^location:\s*(.+)""")
    private val client = OkHttpClient.Builder().callTimeout(java.time.Duration.ofSeconds(1)).build()
    private val FRIENDLY_NAME_PATTERN = Pattern.compile("<friendly-device-name>(.*?)</friendly-device-name>", Pattern.DOTALL)
    private val MODEL_NAME_PATTERN = Pattern.compile("<model-name>(.*?)</model-name>", Pattern.DOTALL)

    suspend fun scan(timeoutMs: Long = 3000): List<RokuDevice> = withContext(Dispatchers.IO) {
        val found = ConcurrentHashMap<String, RokuDevice>()
        val start = System.currentTimeMillis()
        val socket = DatagramSocket()
        try {
            socket.broadcast = true
            val addr = InetAddress.getByName(SSDP_ADDRESS)
            val packet = DatagramPacket(
                SSDP_PAYLOAD.toByteArray(),
                SSDP_PAYLOAD.length,
                addr,
                SSDP_PORT
            )
            socket.send(packet)
            val buf = ByteArray(2048)
            while (System.currentTimeMillis() - start < timeoutMs) {
                try {
                    socket.soTimeout = 750
                    val resp = DatagramPacket(buf, buf.size)
                    socket.receive(resp)
                    val raw = String(resp.data, 0, resp.length)
                    val location = parseLocation(raw) ?: continue
                    if (found.containsKey(location)) continue
                    val ip = parseIpFromLocation(location) ?: continue
                    val (name, model) = fetchDeviceInfo(location)
                    found[location] = RokuDevice(ip, location, name, model)
                } catch (e: SocketTimeoutException) {
                    // Continue until total timeout
                } catch (e: Exception) {
                    Log.w("RokuDiscovery", "Error parsing SSDP: ${e.message}")
                }
            }
        } finally {
            socket.close()
        }
        found.values.toList()
    }

    fun parseLocation(raw: String): String? {
        return LOCATION_HEADER.find(raw)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun parseIpFromLocation(location: String): String? {
        return Regex("https?://([0-9.]+):").find(location)?.groupValues?.getOrNull(1)
    }

    private fun fetchDeviceInfo(location: String): Pair<String?, String?> {
        return try {
            val url = if (location.endsWith("/")) location else "$location/"
            val req = Request.Builder().url("${url}query/device-info").get().build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null to null
            val body = resp.body?.string() ?: return null to null
            val name = FRIENDLY_NAME_PATTERN.matcher(body).find().let {
                if (it) FRIENDLY_NAME_PATTERN.matcher(body).replaceFirst("$1") else null
            }
            val model = MODEL_NAME_PATTERN.matcher(body).find().let {
                if (it) MODEL_NAME_PATTERN.matcher(body).replaceFirst("$1") else null
            }
            name to model
        } catch (e: Exception) {
            null to null
        }
    }
}
