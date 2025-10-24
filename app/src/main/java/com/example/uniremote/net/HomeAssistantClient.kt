package com.example.uniremote.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Home Assistant REST API client for controlling Fire TV via the Android TV integration.
 *
 * @param baseUrl The base URL of the Home Assistant instance (e.g., "http://192.168.1.100:8123")
 * @param token The long-lived access token for authentication
 */
class HomeAssistantClient(
    private val baseUrl: String,
    private val token: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends an ADB command to Fire TV via Home Assistant's androidtv.adb_command service.
     *
     * @param entityId The entity ID of the Fire TV (e.g., "media_player.fire_tv_living_room")
     * @param command The ADB command to send (e.g., "HOME", "BACK", "UP", "CENTER")
     * @return Result.success on success, Result.failure with error details otherwise
     */
    suspend fun adbCommand(entityId: String, command: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = """
                {
                    "entity_id": "$entityId",
                    "command": "$command"
                }
            """.trimIndent()

            val url = "$baseUrl/api/services/androidtv/adb_command"
            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
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

    /**
     * Sends remote command(s) to Fire TV via Home Assistant's remote.send_command service.
     *
     * @param entityId The entity ID of the Fire TV
     * @param keys List of command keys to send
     * @return Result.success on success, Result.failure with error details otherwise
     */
    suspend fun remoteSend(entityId: String, keys: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val keysJson = keys.joinToString("\", \"", "\"", "\"")
            val json = """
                {
                    "entity_id": "$entityId",
                    "command": [$keysJson]
                }
            """.trimIndent()

            val url = "$baseUrl/api/services/remote/send_command"
            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
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

    /**
     * Validates Home Assistant connectivity by checking the API endpoint.
     *
     * @return Result.success if HA is reachable and token is valid, Result.failure otherwise
     */
    suspend fun validate(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $token")
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
}

/**
 * Fire TV command mapper - maps remote button names to ADB commands.
 */
object FireTvCommands {
    const val HOME = "HOME"
    const val BACK = "BACK"
    const val UP = "UP"
    const val DOWN = "DOWN"
    const val LEFT = "LEFT"
    const val RIGHT = "RIGHT"
    const val CENTER = "CENTER"  // OK/Select button
    const val PLAY = "PLAY"
    const val PAUSE = "PAUSE"
    const val PLAY_PAUSE = "MEDIA_PLAY_PAUSE"
    const val POWER = "POWER"
    const val SLEEP = "SLEEP"
}
