# TODO_NEXT — Curated Changes and Ideas

Concrete fixes and improvements derived from the current codebase. Prioritize top section; lower items are nice-to-haves.

## Short-term fixes

- Eliminate legacy `ui/settings/SettingsSheet.kt` usage entirely
  - Current app uses `SettingsScreen.kt`. Keep the legacy file stubbed or remove references to avoid Compose API mismatches.
- Consolidate package/path for `RokuRemote.kt`
  - File path is `com/example/uniremote/RokuRemote.kt` but package is `com.example.uniremote.net` — consider moving under `net/` for clarity.
- Ensure manifest/network config intent
  - Debug overlay sets cleartext globally; main `network_security_config.xml` isn’t referenced. Decide intended release behavior and wire via manifest if needed.

## UX polish

- Remote layout tweaks: spacing, tonal contrast, and elevation to more closely match Roku app
- Add loading/disabled states per button during network calls (prevent spam)
- Improve haptics: distinct patterns for success vs error

## Features

- Multiple saved Roku devices with quick switch
- Auto-pick last seen Roku on startup (cache discovery results)
- Optional Wake-on-LAN attempt before Roku `PowerOn` (parse MAC from device-info)
- Voice/mic input and on-screen keyboard shortcuts
- Channel/app launcher (Roku ECP `/launch/<appId>`)
- Homescreen widget and Quick Settings tiles for key actions (e.g., Volume, Power)

## Developer tooling

- Unit tests for `CommandMapper` (already has inline validators — expose as tests)
- Instrumented tests for Settings validation and SSDP scan sheet
- CI task to assemble debug and run a small smoke test
