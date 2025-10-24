# DISCOVERY — Roku SSDP Flow

This document explains the Roku device discovery flow implemented in code and how it is surfaced in the UI.

## Overview

File: `app/src/main/java/com/example/uniremote/net/RokuDiscovery.kt`

- Sends SSDP M-SEARCH to discover Roku devices advertising `ST: roku:ecp`
- Parses the `Location` header from responses to derive the device base URL and IP
- Enriches each device by fetching `GET <location>/query/device-info` to extract friendly name and model
- De-duplicates by `location`

## Algorithm

1. Construct payload (CRLF line endings):
```
M-SEARCH * HTTP/1.1\r\n
HOST: 239.255.255.250:1900\r\n
MAN: "ssdp:discover"\r\n
MX: 2\r\n
ST: roku:ecp\r\n
\r\n
```
2. Open a `DatagramSocket`, enable broadcast, and send to 239.255.255.250:1900
3. Loop with `socket.soTimeout = 750ms` until total `timeoutMs` (default 3000ms):
   - Receive UDP packet
   - Parse `location` via regex `(?im)^location:\s*(.+)`
   - Extract IP with `https?://([0-9.]+):` regex
   - Fetch device info: `GET <location>/query/device-info` (OkHttp `callTimeout = 1s`)
   - Parse `<friendly-device-name>` and `<model-name>`
4. Return list of `RokuDevice(ip, location, name, model)`

Errors are caught and logged; timeouts simply continue the loop until overall timeout.

## Multicast lock

File: `app/src/main/java/com/example/uniremote/net/Multicast.kt`

- `suspend fun <T> withMulticastLock(context, block)` acquires a `WifiManager.MulticastLock` for the duration of the scan and releases it afterward.

## UI integration

File: `app/src/main/java/com/example/uniremote/ui/SettingsScreen.kt`

- Button: "Scan for Roku"
  - Sets `scanning=true`, clears previous results
  - Calls `withMulticastLock(context) { RokuDiscovery.scan() }`
  - On results: opens a `ModalBottomSheet` listing devices
  - On empty: snackbar "No Roku devices found..."
- Bottom sheet list: selecting a device
  - Fills the Roku IP field
  - Persists IP via `context.saveIp(device.ip)`
  - Shows snackbar confirming selection

## Permissions

`android.permission.CHANGE_WIFI_MULTICAST_STATE` is declared in `AndroidManifest.xml` and required for SSDP on some devices.
