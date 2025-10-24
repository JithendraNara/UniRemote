# SETTINGS — Persistent Configuration (DataStore)

This document lists the DataStore keys, defaults, and where they are used.

## Data model

Defined in `app/src/main/java/com/example/uniremote/data/RemoteMode.kt`:

- `enum class RemoteMode { ROKU, FIRE_TV }`
  - `companion object fun fromString(value: String?): RemoteMode` (defaults to `ROKU`)
- `data class AppSettings(
    val rokuIp: String = "",
    val haBaseUrl: String = "",
    val haToken: String = "",
    val fireTvEntity: String = "",
    val lastMode: RemoteMode = RemoteMode.ROKU
  )`

## DataStore keys and defaults

Declared and used in `app/src/main/java/com/example/uniremote/IpStorage.kt`:

- `roku_ip` — String (default: empty)
- `ha_base_url` — String (default: empty)
- `ha_token` — String (default: empty, secret)
- `fire_tv_entity` — String (default: empty)
- `last_mode` — String (default: `ROKU` via `RemoteMode.fromString`)

Helpers:
- `suspend fun Context.readSettings(): AppSettings`
- `suspend fun Context.saveSettings(settings: AppSettings)`
- `suspend fun Context.readIp(): String?`
- `suspend fun Context.saveIp(ip: String)`
- `suspend fun Context.saveMode(mode: RemoteMode)`

## Where settings are read/written

- Read on startup: `MainActivity.kt` (`LaunchedEffect(Unit)`) → `context.readSettings()` → `settings` state
- Save on settings screen: `SettingsScreen.kt` → `onSave` callback → `context.saveSettings(newSettings)`
- Save Roku IP after SSDP selection: `SettingsScreen.kt` → `context.saveIp(device.ip)`
- Persist mode toggle: `MainActivity.kt` mode selection → `context.saveMode(mode)`

## Validation flows

- Validate Roku: `MainActivity.kt` passes `onValidateRoku(ip)` → `validateRoku(ip)` (in `RokuRemote.kt`) → `GET /query/device-info` with 2s timeouts
- Validate Home Assistant: `onValidateHA(url, token)` → `HomeAssistantClient.validate()` → `GET /api/`

Snackbars show success/error via `UiMessage`.
