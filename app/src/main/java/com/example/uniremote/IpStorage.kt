package com.example.uniremote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val ROKU_IP_KEY = stringPreferencesKey("roku_ip")
private val FIRE_TV_ID_KEY = stringPreferencesKey("fire_tv_id")
private val LAST_MODE_KEY = stringPreferencesKey("last_mode")
private val FLING_SID_KEY = stringPreferencesKey("fling_sid")
private val OVERLAY_ENABLED_KEY = booleanPreferencesKey("overlay_enabled")
private val FAVORITES_KEY = stringPreferencesKey("favorites")
private val FIRE_TV_INPUT_KEY = stringPreferencesKey("fire_tv_input")

/**
 * Reads all application settings from DataStore.
 *
 * @return AppSettings with all saved configuration.
 */
suspend fun Context.readSettings(): AppSettings {
    return dataStore.data.map { preferences ->
        val favsRaw = preferences[FAVORITES_KEY] ?: ""
        val favs = favsRaw.split('\n')
            .mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size >= 2) RokuFavorite(parts[0], parts[1]) else null
            }
        AppSettings(
            rokuIp = preferences[ROKU_IP_KEY] ?: "",
            fireTvId = preferences[FIRE_TV_ID_KEY] ?: "",
            // Default to SDK's DEFAULT_PLAYER_SERVICE_ID (amzn.thin.pl) if not set yet
            flingSid = preferences[FLING_SID_KEY] ?: "amzn.thin.pl",
            lastMode = RemoteMode.fromString(preferences[LAST_MODE_KEY]),
            overlayEnabled = preferences[OVERLAY_ENABLED_KEY] ?: false,
            fireTvVolumeOverlayEnabled = preferences[booleanPreferencesKey("firetv_volume_overlay_enabled")] ?: false,
            favorites = favs,
            fireTvInput = preferences[FIRE_TV_INPUT_KEY] ?: ""
        )
    }.first()
}

/**
 * Saves all application settings to DataStore.
 *
 * @param settings The settings to save.
 */
suspend fun Context.saveSettings(settings: AppSettings) {
    dataStore.edit { preferences ->
        preferences[ROKU_IP_KEY] = settings.rokuIp
        preferences[FIRE_TV_ID_KEY] = settings.fireTvId
        preferences[FLING_SID_KEY] = settings.flingSid
        preferences[LAST_MODE_KEY] = settings.lastMode.name
        preferences[OVERLAY_ENABLED_KEY] = settings.overlayEnabled
        // Persist favorites as newline-delimited "label|appId"
        preferences[FAVORITES_KEY] = settings.favorites.joinToString("\n") { f -> "${f.label}|${f.appId}" }
        preferences[FIRE_TV_INPUT_KEY] = settings.fireTvInput
    }
}

/** Saves only the selected Fire TV receiver id to DataStore. */
suspend fun Context.saveFireTvId(id: String) {
    dataStore.edit { preferences ->
        preferences[FIRE_TV_ID_KEY] = id
    }
}

/**
 * Reads the saved Roku IP address from DataStore.
 *
 * @return The saved IP address, or null if not set.
 */
suspend fun Context.readIp(): String? {
    return dataStore.data.map { preferences ->
        preferences[ROKU_IP_KEY]
    }.first()
}

/**
 * Saves the Roku IP address to DataStore.
 *
 * @param ip The IP address to save.
 */
suspend fun Context.saveIp(ip: String) {
    dataStore.edit { preferences ->
        preferences[ROKU_IP_KEY] = ip
    }
}

/**
 * Saves the current remote mode to DataStore.
 *
 * @param mode The remote mode to save.
 */
suspend fun Context.saveMode(mode: RemoteMode) {
    dataStore.edit { preferences ->
        preferences[LAST_MODE_KEY] = mode.name
    }
}

/** Saves only the overlay enabled flag. */
suspend fun Context.saveOverlayEnabled(enabled: Boolean) {
    dataStore.edit { preferences ->
        preferences[OVERLAY_ENABLED_KEY] = enabled
    }
}

/** Saves only the Fire TV volume overlay enabled flag. */
suspend fun Context.saveFireTvVolumeOverlayEnabled(enabled: Boolean) {
    dataStore.edit { preferences ->
        preferences[booleanPreferencesKey("firetv_volume_overlay_enabled")] = enabled
    }
}
