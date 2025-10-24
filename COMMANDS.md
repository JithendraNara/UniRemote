# COMMANDS — Routing and Mappings

This document defines the UI command set and how each command is routed per mode based on the current code.

## UiCommand set

Defined in `app/src/main/java/com/example/uniremote/data/CommandMapper.kt`:

- Navigation: `Home`, `Back`, `Up`, `Down`, `Left`, `Right`, `Ok`
- Media: `Play`, `Pause`
- TV controls: `VolUp`, `VolDown`, `Power`

Notes:
- `Power` in the mapping table corresponds to Roku’s `PowerOff`. There are helpers in code for `PowerOn`, but the UI currently dispatches `Power` → `PowerOff` only.

## Roku ECP tokens (case-sensitive)

Declared in `RokuEcpMap` (same file):

- `Home` → `"Home"`
- `Back` → `"Back"`
- `Up` → `"Up"`
- `Down` → `"Down"`
- `Left` → `"Left"`
- `Right` → `"Right"`
- `Ok` → `"Select"`
- `Play` → `"Play"`
- `Pause` → `"Pause"`
- `VolUp` → `"VolumeUp"`
- `VolDown` → `"VolumeDown"`
- `Power` → `"PowerOff"`

Helpers in `RokuRemote.kt` also support `PowerOn`/`PowerOff`, but UI paths use `PowerOff`.

## Fire TV ADB commands (via Home Assistant)

Declared in `FireTvAdbMap`:

- `Home` → `HOME`
- `Back` → `BACK`
- `Up` → `UP`
- `Down` → `DOWN`
- `Left` → `LEFT`
- `Right` → `RIGHT`
- `Ok` → `CENTER`
- `Play` → `PLAY`
- `Pause` → `PAUSE`
- `VolUp` → `VOLUME_UP` (not used in routing)
- `VolDown` → `VOLUME_DOWN` (not used in routing)
- `Power` → `POWER` (not used in routing)

## UI → mapper → transport

Wiring is implemented in `MainActivity.kt` and `RemoteScreen.kt`:

- Mode selection: `ModeToggle` emits `RemoteMode.ROKU` or `RemoteMode.FIRE_TV`
- DPad/Buttons (e.g., Home/Back/Ok/Play/Pause):
  - Roku mode: `UiCommand` → `RokuEcpMap[key]` → `sendRoku(ip, token)`
  - Fire TV mode: `UiCommand` → `FireTvAdbMap[key]` → `HomeAssistantClient.adbCommand(entity, cmd)`
- Exceptions (always sent to Roku regardless of mode):
  - Volume Up/Down: `sendRokuVolumeUp(ip)` / `sendRokuVolumeDown(ip)`
  - Power: `sendRokuPowerOff(ip)`

Snackbar results are produced via `UiMessage.Success`/`Error` after each network action.
