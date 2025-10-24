# NETWORKING — Protocols and API Usage

This document captures the concrete HTTP endpoints, discovery protocol, timeouts, and error handling present in the code.

## Roku ECP (External Control Protocol)

- Base URL: `http://<ROKU_IP>:8060/`
- Endpoints used (see `RokuRemote.kt`):
  - `POST /keypress/<Key>` — sends a keypress (e.g., `Home`, `Select`, `Back`, `VolumeUp`)
  - `GET /query/device-info` — used by validation and discovery enrichment
- HTTP client: OkHttp
  - Default timeouts in `sendRoku`: 2s connect/write/read
  - For `PowerOn`, the client uses 5s timeouts and 1 retry (attempts: 2)
  - Headers: `Accept: */*`, `User-Agent: UniRemote/1.0`
- Error handling
  - Success: HTTP 200–204 → `Result.success(Unit)`
  - 403 Forbidden → error includes guidance to enable Roku “Control by mobile apps” and permissive/default Network Access
  - Exceptions (timeouts/connectivity) are mapped to user-friendly messages; if TV is asleep, guidance suggests enabling Fast TV Start or waking via physical remote

## Roku SSDP Discovery

Implemented in `RokuDiscovery.kt`:

- SSDP request: UDP multicast M-SEARCH to `239.255.255.250:1900`
```
M-SEARCH * HTTP/1.1\r\n
HOST: 239.255.255.250:1900\r\n
MAN: "ssdp:discover"\r\n
MX: 2\r\n
ST: roku:ecp\r\n
\r\n
```
- Response parsing:
  - Extract `Location` header via regex `(?im)^location:\s*(.+)`
  - Parse IP from `https?://<ip>:`
  - Enrich with `GET <location>/query/device-info`, parsing `<friendly-device-name>` and `<model-name>`
- Timeouts:
  - UDP socket `soTimeout = 750ms` in a loop up to ~3s total
  - HTTP device-info: `callTimeout = 1s`
- Multicast lock: `withMulticastLock(context) { RokuDiscovery.scan() }` acquires a `WifiManager.MulticastLock` for the duration of the scan

## Home Assistant (Fire TV via Android TV integration)

Implemented in `HomeAssistantClient.kt`:

- Base URL: `<HA_BASE_URL>` (from DataStore)
- Authentication: `Authorization: Bearer <token>` (Long-Lived Access Token from DataStore)
- Endpoints:
  - `POST /api/services/androidtv/adb_command` — body `{ "entity_id": "...", "command": "HOME|BACK|UP|..." }`
  - `POST /api/services/remote/send_command` — body `{ "entity_id": "...", "command": ["...","..."] }`
  - `GET /api/` — simple validation/ping
- Timeouts: 2s connect/write/read (OkHttp client)

## Network security

- Debug build (`app/src/debug/AndroidManifest.xml`):
  - `android:usesCleartextTraffic="true"`
  - `android:networkSecurityConfig="@xml/network_security_config"` (debug variant allows cleartext to any host)
- Main `res/xml/network_security_config.xml` permits cleartext for private IP ranges and local hostnames but is not referenced in the main manifest; release builds thus default to HTTPS-only unless configured via manifest overlay/flavor.

## UI feedback

- Each network action returns a `Result` which is converted to `UiMessage.Success/Error` in `MainActivity.kt` and surfaced via Snackbars.
