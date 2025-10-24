# DIAGRAMS — Architecture and Flows

Mermaid diagrams describing current code paths.

## Class/module overview

```mermaid
classDiagram
  direction LR
  class MainActivity {
    +Composable MainScreen()
    +sendNavCommand(mode, command, settings, scope, onResult)
    +sendRokuVolumeOrPower(key, ip, scope, onResult)
  }
  class AppSettings {
    +rokuIp: String
    +haBaseUrl: String
    +haToken: String
    +fireTvEntity: String
    +lastMode: RemoteMode
  }
  class RemoteMode {
    <<enum>> ROKU, FIRE_TV
  }
  class UiCommand {
    <<sealed>> Home, Back, Up, Down, Left, Right, Ok, Play, Pause, VolUp, VolDown, Power
  }
  class RokuEcpMap {
    <<object>> Map<UiCommand, String>
  }
  class FireTvAdbMap {
    <<object>> Map<UiCommand, String>
  }
  class RokuRemote {
    +sendRoku(ip, key)
    +sendRokuVolumeUp(ip)
    +sendRokuVolumeDown(ip)
    +sendRokuPowerOff(ip)
    +sendRokuPowerOn(ip)
    +validateRoku(ip)
  }
  class HomeAssistantClient {
    +adbCommand(entityId, command)
    +remoteSend(entityId, keys)
    +validate()
  }
  class RokuDiscovery {
    +scan(timeoutMs)
    +parseLocation(raw)
  }
  class IpStorage {
    +readSettings()
    +saveSettings(settings)
    +saveIp(ip)
    +saveMode(mode)
  }
  class SettingsScreen {
    +Composable SettingsScreen(...)
  }
  class RemoteScreen {
    +Composable RemoteScreen(...)
  }

  MainActivity --> RemoteScreen
  MainActivity --> SettingsScreen
  MainActivity --> RokuRemote
  MainActivity --> HomeAssistantClient
  MainActivity --> IpStorage
  SettingsScreen --> RokuDiscovery
  SettingsScreen --> IpStorage
  RokuDiscovery --> RokuRemote : device-info
  UiCommand --> RokuEcpMap
  UiCommand --> FireTvAdbMap
```

## Sequence: Tap "Home" in Roku mode

```mermaid
sequenceDiagram
  participant U as User
  participant RS as RemoteScreen
  participant MA as MainActivity
  participant M as RokuEcpMap
  participant RR as RokuRemote

  U->>RS: Tap Home
  RS->>MA: onHome()
  MA->>M: map UiCommand.Home → "Home"
  MA->>RR: sendRoku(ip, "Home")
  RR-->>MA: Result(success/failure)
  MA-->>RS: UiMessage → Snackbar
```

## Sequence: Tap "Up" in Fire TV mode

```mermaid
sequenceDiagram
  participant U as User
  participant RS as RemoteScreen
  participant MA as MainActivity
  participant M as FireTvAdbMap
  participant HA as HomeAssistantClient

  U->>RS: Tap Up
  RS->>MA: onNav(UiCommand.Up)
  MA->>M: map UiCommand.Up → "UP"
  MA->>HA: adbCommand(entityId, "UP")
  HA-->>MA: Result(success/failure)
  MA-->>RS: UiMessage → Snackbar
```

## Sequence: Run "Scan for Roku"

```mermaid
sequenceDiagram
  participant U as User
  participant SS as SettingsScreen
  participant MC as Multicast Lock
  participant RD as RokuDiscovery
  participant IS as IpStorage

  U->>SS: Tap Scan for Roku
  SS->>MC: withMulticastLock { ... }
  MC->>RD: scan(3000ms)
  RD-->>SS: List<RokuDevice(name, model, ip)>
  SS->>U: Show modal list
  U->>SS: Select device
  SS->>IS: saveIp(ip)
  SS-->>U: Snackbar "Selected: <name> (<ip>)"
```

## State: Mode toggle and routing

```mermaid
stateDiagram-v2
  [*] --> Roku
  Roku --> FireTV: User toggles
  FireTV --> Roku: User toggles

  state Roku {
    [*] --> Navigating
  }
  state FireTV {
    [*] --> Navigating
  }

  note right of Roku
    Navigation → RokuEcpMap → RokuRemote
  end note

  note right of FireTV
    Navigation → FireTvAdbMap → HomeAssistantClient
  end note

  note bottom of stateDiagram-v2
    VolumeUp/Down & Power always go to RokuRemote
  end note
```
