package com.example.uniremote.data

/**
 * Remote control mode selector.
 */
enum class RemoteMode {
    ROKU,
    FIRE_TV;

    companion object {
        fun fromString(value: String?): RemoteMode {
            return when (value?.uppercase()) {
                "FIRE_TV" -> FIRE_TV
                else -> ROKU
            }
        }
    }
}

/**
 * Application settings data.
 */
data class AppSettings(
    val rokuIp: String = "",
    val fireTvId: String = "", // new: selected Fire TV receiver ID via Fling SDK
    val flingSid: String = "amzn.thin.pl", // Amazon Fling Service ID (SID) default from SDK
    val lastMode: RemoteMode = RemoteMode.ROKU,
    val overlayEnabled: Boolean = false,
    val fireTvVolumeOverlayEnabled: Boolean = false, // Shows volume overlay when Fire TV app is open
    val favorites: List<RokuFavorite> = emptyList(),
    // Roku TV input where Fire TV is connected. Example: "tvinput.hdmi1"
    val fireTvInput: String = ""
)

/** Simple favorite app descriptor for Roku launcher. */
data class RokuFavorite(
    val label: String,
    val appId: String
)
