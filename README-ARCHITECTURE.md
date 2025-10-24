# UniRemote — Architecture Overview

This document maps the current UniRemote Android app (Kotlin + Jetpack Compose) based strictly on the code in this repository. It is intended for handoff and safe future changes.

## What UniRemote does

- Roku TV control via Roku ECP (External Control Protocol) over HTTP
- Fire TV control via Home Assistant (Android TV integration) using REST (ADB/remote services)
- Single-screen remote UI with a mode toggle (Roku / Fire TV)
- Roku device discovery via SSDP (multicast M-SEARCH) with name/model enrichment
- VS Code one-click run scripts and tasks to build/install/launch without PATH setup

## Project layout

Top-level files/folders with purpose:

- `app/` — Android application module
  - `src/main/AndroidManifest.xml` — base manifest; declares INTERNET and CHANGE_WIFI_MULTICAST_STATE
  - `src/debug/AndroidManifest.xml` — debug overlay; enables cleartext and sets `networkSecurityConfig`
  - `src/debug/res/xml/network_security_config.xml` — debug network config (cleartext permitted globally)
  - `src/main/res/xml/network_security_config.xml` — main network config for private ranges (not referenced in manifest by default)
  - `src/main/java/com/example/uniremote/` — app sources
    - `MainActivity.kt` — top-level Compose UI scaffolding, mode toggle, navigation routing, snackbar handling
    - `IpStorage.kt` — DataStore (Preferences) helpers for reading/writing `AppSettings`
    - `data/` — models and command mapping
      - `RemoteMode.kt` — `enum class RemoteMode { ROKU, FIRE_TV }` and `data class AppSettings`
      - `CommandMapper.kt` — `sealed interface UiCommand` + Roku and Fire TV mapping tables
    - `net/` — networking and discovery
      - `RokuDiscovery.kt` — SSDP scan, location parsing, device info fetch (name/model)
      - `Multicast.kt` — `withMulticastLock(context) { ... }` helper for SSDP scans
      - `HomeAssistantClient.kt` — REST client for HA services (androidtv.adb_command, remote.send_command, validate)
    - `RokuRemote.kt` — Roku ECP HTTP functions: `sendRoku`, volume/power helpers, `validateRoku`
    - `ui/` — Compose UI
      - `RemoteScreen.kt` — main remote layout (top row Home/Back/Power, center D-Pad, side volume, bottom playback)
      - `SettingsScreen.kt` — settings UI (Roku IP, SSDP scanning modal; HA base URL/token/entity; validate buttons; save)
      - `UiMessage.kt` — simple `Success`/`Error` wrapper for snackbar messaging
      - `components/`
        - `DPad.kt` — directional controls + OK (emits `UiCommand`)
        - `ModeToggle.kt` — Roku/Fire TV toggle
        - `RemoteButtons.kt` — shared button styles (currently used in older layout)
      - `haptics/Haptics.kt` — lightweight haptic helper
      - `theme/` — Material 3 theme files
- `scripts/`
  - `run.sh` — build, install, and launch; starts emulator if no device is connected
  - `adb.sh` — finds SDK and proxies to `adb` without requiring PATH
- `.vscode/tasks.json` — VS Code tasks (assemble, install, run, logcat, ADB helper tasks)
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` — Gradle Kotlin DSL config

## Build & run

You can build and run from VS Code or terminal:

- VS Code tasks (see `.vscode/tasks.json`):
  - Run App (device or emulator) — full flow: build → install → launch (auto-starts emulator if none)
  - Assemble Debug, Install Debug, Open App (only), Show Devices, Pair/Connect Wireless, View Logs (logcat)
- Scripts:
  - `./scripts/run.sh` — end-to-end build/install/launch, auto-detects SDK, starts emulator if needed
  - `./scripts/adb.sh` — wrapper for `adb` that locates the SDK
- Gradle:
  - `./gradlew assembleDebug`
  - `./gradlew installDebug`

## Permissions & network policy

- Permissions declared in `app/src/main/AndroidManifest.xml`:
  - `android.permission.INTERNET`
  - `android.permission.CHANGE_WIFI_MULTICAST_STATE` (required for SSDP discovery)
- Debug networking (`app/src/debug/AndroidManifest.xml`):
  - `android:usesCleartextTraffic="true"`
  - `android:networkSecurityConfig="@xml/network_security_config"` (debug config permits cleartext to any host)
- Main `res/xml/network_security_config.xml` allows cleartext for private ranges and local hostnames but is not referenced by the main manifest. Release builds therefore default to HTTPS-only unless the attribute is set via a product flavor/overlay.

## Known limitations

- Fire TV volume/power: Routed to Roku TV by design; ADB cannot adjust TV volume or control TV power reliably. Volume/Power buttons always target Roku.
- Roku cleartext HTTP: ECP uses HTTP on port 8060. Debug builds explicitly allow cleartext; release builds should remain HTTPS-only (no ECP) unless you wire the network config.
- Sleeping Roku TV: If Fast TV Start is disabled, the Roku’s network stack sleeps; ECP requests time out until the TV is woken physically or via supported wake paths.
- LAN discovery: SSDP (multicast) can be blocked by AP isolation or routers that filter IGMP/multicast.
