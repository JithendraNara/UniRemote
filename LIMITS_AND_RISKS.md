# LIMITS_AND_RISKS — Reality Checks

This document lists known constraints and user-facing error cases in the current code.

## Fire TV control limits

- Volume/Power: ADB/Android TV integration does not control TV volume or TV power reliably.
- App routes Volume and Power to Roku TV regardless of mode (see `MainActivity.kt`).

## Roku networking

- ECP uses cleartext HTTP on port 8060. Debug builds explicitly allow cleartext; release builds should not unless manifest overlays are configured.
- If Roku TV is asleep and Fast TV Start is disabled, the network stack is offline:
  - Requests time out; user is guided to enable Fast TV Start or wake with the physical remote.

## LAN multicast caveats

- SSDP relies on UDP multicast; some networks block discovery (AP isolation, IGMP/multicast filtering).
- The app acquires a `WifiManager.MulticastLock` during scans, but router/network must permit client-to-client traffic.

## Security notes

- No tokens are logged in code; HA calls add `Authorization: Bearer <token>` header.
- Avoid committing tokens to VCS; tokens are stored in DataStore preferences on-device.

## Error handling surfaced to users

- Roku 403 Forbidden: Guidance to enable "Control by mobile apps" and set Network Access appropriately.
- Timeouts/Connect errors: Snackbar indicates network timeout; messages hint at Fast TV Start for PowerOn.
- Discovery: "No Roku devices found" snackbar when SSDP scan returns empty.
