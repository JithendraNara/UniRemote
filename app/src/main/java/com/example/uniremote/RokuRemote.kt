package com.example.uniremote.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Roku remote control keys supported by the ECP (External Control Protocol) API.
 */
val RokuKeys = listOf(
    "Home",
    "Back",
    "Up",
    "Down",
    "Left",
    "Right",
    "Select",
    "Play",
    "Pause",
    "VolumeUp",
    "VolumeDown",
    "PowerOff",
    "PowerOn"
)

/**
 * Sends a keypress command to a Roku device over HTTP.
 *
 * @param ip The IP address of the Roku device.
 * @param key The key to press (e.g., "Home", "Select", "Play").
 * @return Result.success on HTTP 200-204, Result.failure otherwise.
 */
suspend fun sendRoku(ip: String, key: String): Result<Unit> = withContext(Dispatchers.IO) {
    fun buildClient(forPowerOn: Boolean): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(if (forPowerOn) 5 else 2, TimeUnit.SECONDS)
            .writeTimeout(if (forPowerOn) 5 else 2, TimeUnit.SECONDS)
            .readTimeout(if (forPowerOn) 5 else 2, TimeUnit.SECONDS)
            .build()

    val url = "http://$ip:8060/keypress/$key"
    val request = Request.Builder()
        .url(url)
        .header("Accept", "*/*")
        .header("User-Agent", "UniRemote/1.0")
        .post("".toRequestBody(null))
        .build()

    val maxAttempts = if (key == "PowerOn") 2 else 1
    var lastError: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            Log.d("Roku", "POST /keypress/$key (attempt ${attempt + 1}/$maxAttempts)")
            val client = buildClient(forPowerOn = key == "PowerOn")
            client.newCall(request).execute().use { response ->
                if (response.code in 200..204) {
                    Log.d("Roku", "OK ${response.code} for $url")
                    return@withContext Result.success(Unit)
                } else {
                    val hint = if (response.code == 403) {
                        " — Enable External Control: on your Roku go to Settings > System > Advanced system settings > External Control (or Control by mobile apps) and set Network Access to Default/Permissive and ensure Control by mobile apps is enabled. Also ensure the phone is on the same subnet, or use Permissive."
                    } else ""
                    val msg = "HTTP ${response.code} ${response.message} for $url$hint"
                    Log.w("Roku", msg)
                    return@withContext Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            lastError = e as? Exception ?: Exception(e.message)
            // If first attempt on PowerOn times out, brief backoff then retry once
            if (attempt < maxAttempts - 1) {
                try { Thread.sleep(400) } catch (_: InterruptedException) {}
            }
        }
    }

    // Map common network errors to a helpful message
    val root = lastError
    val friendly = when (root) {
        is java.net.SocketTimeoutException,
        is java.net.ConnectException,
        is java.net.NoRouteToHostException -> {
            val extra = if (key == "PowerOn")
                "The TV may be asleep with network disabled. On Roku TV, enable Settings > System > Power > Fast TV Start to allow wake over network. Otherwise, wake the TV with the physical remote, then try again." else
                "The Roku did not respond. If the TV is asleep, enable Fast TV Start or wake it with the physical remote."
            "Network timeout connecting to $url. $extra"
        }
        else -> root?.message ?: "Failed to reach Roku at $url"
    }
    Result.failure(Exception(friendly))
}

/**
 * Launches a Roku channel or input by appId (e.g., "12" for Netflix or "tvinput.hdmi1").
 */
suspend fun launchRoku(ip: String, appId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
        val url = "http://$ip:8060/launch/$appId"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .header("User-Agent", "UniRemote/1.0")
            .post("".toRequestBody(null))
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code in 200..204) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.code} ${response.message} for $url"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Sends Volume Up command to Roku TV.
 */
suspend fun sendRokuVolumeUp(ip: String): Result<Unit> = sendRoku(ip, "VolumeUp")

/**
 * Sends Volume Down command to Roku TV.
 */
suspend fun sendRokuVolumeDown(ip: String): Result<Unit> = sendRoku(ip, "VolumeDown")

/**
 * Sends Power Off command to Roku TV.
 */
suspend fun sendRokuPowerOff(ip: String): Result<Unit> = sendRoku(ip, "PowerOff")

/**
 * Sends Power On command to Roku TV.
 */
suspend fun sendRokuPowerOn(ip: String): Result<Unit> = sendRoku(ip, "PowerOn")

/**
 * Validates Roku connectivity by querying device info.
 *
 * @param ip The IP address of the Roku device.
 * @return Result.success if Roku responds, Result.failure otherwise.
 */
suspend fun validateRoku(ip: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        Log.d("Net", "Roku validation (debug, cleartext enabled)")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        val url = "http://$ip:8060/query/device-info"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .header("User-Agent", "UniRemote/1.0")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
