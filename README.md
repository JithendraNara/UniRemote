# UniRemote

A unified Android remote control app that manages both Roku TV and Fire TV from a single interface, with intelligent volume/power routing.

## Features

- **Dual-Mode Remote Control**: Toggle between Roku TV and Fire TV with one tap
  - **Roku Mode**: Direct control via HTTP (External Control Protocol)
  - **Fire TV Mode**: Control via Home Assistant integration with Android TV
- **Smart Volume & Power**: Always controls Roku TV, even in Fire TV mode
  - Volume buttons always adjust TV volume
  - Power button always controls TV power
  - Perfect for Fire TV connected via HDMI - control TV and streaming device seamlessly
- **Complete Remote Functions**:
  - D-pad navigation (Up, Down, Left, Right, OK/Select)
  - Home and Back buttons
  - Play and Pause controls
  - Volume Up/Down and Power On/Off
- **Home Assistant Integration**: Network-based Fire TV control without direct pairing
   - Or use the built-in Amazon Fling SDK path (no Home Assistant required) for local media control
- **Persistent Settings**: Saves all configuration (IPs, tokens, last mode)
- **Connection Validation**: Test Roku and Home Assistant connectivity before use
- **Material 3 Design**: Modern, clean interface with dynamic color support

## Requirements

  - Home Assistant instance running on your network
  - Fire TV with ADB debugging enabled
  - Android TV integration configured in Home Assistant

## Automatic Roku Discovery

UniRemote can automatically find Roku devices on your Wi-Fi network using SSDP (Simple Service Discovery Protocol).

- Tap the **Scan for Roku** button in Settings (next to the Roku IP field).
- The app sends a multicast SSDP M-SEARCH to `239.255.255.250:1900` and listens for Roku responses.
- All discovered Roku devices are shown in a list with their name, model, and IP address.
- Tap a device to save its IP instantly—no manual typing required!

**Requirements:**
- Your phone and Roku TV must be on the same Wi-Fi network.
- The app requests the `CHANGE_WIFI_MULTICAST_STATE` permission to receive SSDP responses.
- Some Wi-Fi networks (especially guest or enterprise) may block device-to-device discovery.

**How it works:**
- UniRemote acquires a Wi-Fi MulticastLock during the scan (released automatically).
- It sends a discovery packet and waits up to 3 seconds for responses.
- For each Roku found, it fetches device info (name/model) for easy identification.
- If no devices are found, you'll see a helpful message and troubleshooting hints.

**Privacy:**
- Discovery is local-only and does not send any data outside your network.

**Troubleshooting:**
- Make sure your phone's Wi-Fi is ON and connected to the same network as your Roku TV.
- Ensure your Roku TV is powered on and connected to Wi-Fi.
- If no devices are found, try again or check your router's settings for client isolation.

---


## Quick Start - VS Code One-Click Run 🚀

**No PATH setup required!** The project includes smart scripts that auto-detect your Android SDK.

### Option 1: Using VS Code Tasks (Recommended)

1. **Connect your device** via USB or wireless debugging
2. **Run the app**: Press **Ctrl/Cmd+Shift+P** → **Tasks: Run Task** → **"Run App (device or emulator)"**

That's it! The script will:
- ✅ Auto-detect your Android SDK location
- ✅ Find your connected device or start an emulator
- ✅ Build, install, and launch the app

### Option 2: Command Line

```bash
# One command to build and run
./scripts/run.sh

# Or manually:
./gradlew assembleDebug
./scripts/adb.sh install app/build/outputs/apk/debug/app-debug.apk
./scripts/adb.sh shell am start -n com.example.uniremote/.MainActivity
```

### Available VS Code Tasks

Access via **Ctrl/Cmd+Shift+P** → **Tasks: Run Task**:

- **Run App (device or emulator)** - Full build and run (default: Ctrl/Cmd+Shift+B)
- **Open App (only)** - Launch already installed app
- **Show Devices** - List connected devices
- **Pair Wireless** - Pair device via wireless debugging
- **Connect Wireless Device** - Connect to paired device
- **View Logs (logcat)** - Stream device logs
- **Assemble Debug** - Build APK only
- **Install Debug** - Install without running

### Wireless Debugging Setup

To run without a USB cable:

1. **On your Android device** (Android 11+):
   - Settings → Developer options → Wireless debugging → **Turn ON**
   - Tap "Pair device with pairing code"
   - Note the IP:port and pairing code

2. **In VS Code**:
   - Run task: **"Pair Wireless"**
   - Enter the IP:port when prompted
   - Enter the pairing code in the terminal
   - Run task: **"Connect Wireless Device"** (use the IP:port from the main wireless debugging screen)

3. **Run the app** normally with **"Run App (device or emulator)"**

## Troubleshooting

### "adb not found"

The scripts couldn't locate your Android SDK. Fix:

1. **Install Android Studio** and SDK:
   - Download from https://developer.android.com/studio
   - Open Android Studio → SDK Manager → Install "Android SDK Platform-Tools"

2. **Or install adb standalone** (macOS):
   ```bash
   brew install android-platform-tools
   ```

3. **Set ANDROID_HOME** (if SDK is in a custom location):
   ```bash
   export ANDROID_HOME=/path/to/your/android/sdk
   ```

### "No devices connected" / "No AVDs found"

**Option A - Connect a physical device:**
1. Enable USB debugging:
   - Settings → About phone → Tap "Build number" 7 times
   - Settings → Developer options → Enable "USB debugging"
2. Connect via USB and accept the authorization prompt
3. Or use wireless debugging (see above)

**Option B - Create an emulator:**
1. Open Android Studio
2. Tools → Device Manager → Create Device
3. Select a device definition and system image
4. The script will auto-start it when you run the app

### Scripts not executable

If you get "Permission denied":

```bash
chmod +x scripts/adb.sh scripts/run.sh
```

Or initialize them with git:

```bash
git update-index --chmod=+x scripts/adb.sh scripts/run.sh
```

## Using the App

UniRemote supports **dual-mode operation**: control either your Roku TV or Fire TV with a simple toggle. Volume and Power buttons **always** control the Roku TV, so they work even when viewing Fire TV content.

### Setting up Roku TV Control

1. **Find your Roku TV's IP address**:
   - On your Roku TV: Settings → Network → About
   - Look for the "IP address" field (e.g., 192.168.1.100)

2. **Configure UniRemote**:
   - Tap the Settings icon (⚙️) in the top-right corner
   - Enter the Roku IP address
   - Tap "Validate Roku Connection" to test
   - Tap "Save All Settings"

3. **Use the remote**:
   - Select "Roku TV" mode at the top
   - D-pad for navigation
   - OK button to select
   - Home/Back for navigation
   - Play/Pause for media control
   - Volume and Power always control Roku TV

**Network Security Note:**
- Roku's External Control Protocol (ECP) uses unencrypted HTTP on port 8060.
- **Debug builds allow cleartext (HTTP) globally via network_security_config. Release builds remain HTTPS-only.**
- All Roku communication stays within your local network and never leaves your home.

### Fire TV (Amazon Fling SDK)

Control Fire TV directly over Wi‑Fi using the Amazon Fling SDK (no Home Assistant or ADB required).

Prerequisites:
- Your phone and Fire TV are on the same Wi‑Fi
- Developer options → ADB debugging may be needed for some devices to advertise on the network

Steps:
1. Tap the Settings icon (⚙️)
2. In the "Fire TV (Fling SDK)" card, tap "Scan for Fire TV"
3. Select your Fire TV from the list; this saves its receiver ID
4. Switch the mode toggle to "Fire TV"
5. Play/Pause/Stop actions work locally via Fling; no Home Assistant or ADB needed

Notes:
- Navigation keys (Home/Back/D‑pad) are not exposed by the Fling media controller; they typically require ADB. The app will show a helpful message if you try them in Fire TV mode.
- Volume and Power continue to control Roku to keep TV control consistent.

Support note:
- Amazon has announced end of standard support for the Fling SDK on March 5, 2026. As a forward‑looking path, consider implementing Matter Casting. See Amazon’s Fling discontinuation & Matter Casting replacement announcement.

If you prefer full navigation for Fire TV, use the Home Assistant method below or the official Fire TV Remote app.

### Setting up Fire TV Control via Home Assistant

Fire TV control works through [Home Assistant](https://www.home-assistant.io/) using the Android TV integration. This allows network-based control without requiring your phone to pair directly with the Fire TV.

#### Prerequisites
- Home Assistant installed and running on your network
- Fire TV connected to the same network
- Home Assistant accessible via local IP or hostname

#### Step 1: Enable ADB on Fire TV

1. On your Fire TV: **Settings** → **My Fire TV** → **Developer Options**
2. Turn ON **ADB Debugging**
3. Turn ON **Apps from Unknown Sources** (if needed)
4. Note your Fire TV's IP address (Settings → My Fire TV → About → Network)

#### Step 2: Add Fire TV to Home Assistant

1. In Home Assistant web interface: **Settings** → **Devices & Services**
2. Click **"+ ADD INTEGRATION"**
3. Search for and select **"Android TV"** or **"Android TV / Fire TV"**
4. Select **"Android TV (network-based)"**
5. Enter your Fire TV's IP address
6. Complete the ADB pairing process (you may see an auth prompt on your TV)
7. Name your device (e.g., "Fire TV Living Room")
8. The integration will create a `media_player` entity (e.g., `media_player.fire_tv_living_room`)

#### Step 3: Create a Home Assistant Long-Lived Access Token

1. In Home Assistant: Click your profile (bottom-left avatar)
2. Scroll down to **"Long-Lived Access Tokens"**
3. Click **"CREATE TOKEN"**
4. Give it a name (e.g., "UniRemote App")
5. Copy the token (it starts with `eyJ...`) - you won't be able to see it again!

#### Step 4: Configure UniRemote

1. In UniRemote, tap the Settings icon (⚙️)
2. Under **Home Assistant Settings**, enter:
   - **Home Assistant URL**: Your HA instance URL (e.g., `http://192.168.1.50:8123` or `https://homeassistant.local:8123`)
   - **Long-Lived Access Token**: Paste the token from Step 3
   - **Fire TV Entity ID**: The entity ID from HA (e.g., `media_player.fire_tv_living_room`)
     - Find this in HA under Settings → Devices & Services → Android TV → click your device
3. Tap **"Validate Home Assistant Connection"** to test
4. Tap **"Save All Settings"**

#### Step 5: Use Fire TV Mode

1. Select **"Fire TV"** mode at the top of the app
2. Navigation (D-pad, Home, Back, Play, Pause) will control Fire TV via Home Assistant
3. **Volume and Power continue to control your Roku TV** for unified TV control

### How It Works

- **Roku Mode**: All commands sent directly to Roku via HTTP (ECP protocol)
- **Fire TV Mode**: Navigation commands routed through Home Assistant's Android TV integration
- **Volume & Power**: Always sent to Roku TV, regardless of mode - this ensures your TV's physical controls work even when using HDMI input from Fire TV

### Troubleshooting Fire TV

**"Fire TV command failed"**
- Verify Home Assistant is reachable from your phone
- Check Fire TV entity ID is correct in settings
- Ensure ADB Debugging is still enabled on Fire TV
- In Home Assistant, check the Android TV integration is "Connected"
- Try restarting the Android TV integration in Home Assistant

**Fire TV stops responding**
- Fire TV may disable ADB after updates or reboots
- Re-enable Developer Options → ADB Debugging on Fire TV
- Restart the Android TV integration in Home Assistant

**Want to test HA connection manually**
- Use the "Validate Home Assistant Connection" button in Settings
- Check Home Assistant logs for any error messages

### Using the Official Fire TV Remote App (Legacy)

Tap "Open Fire TV Remote" to:
- Launch the official Fire TV Remote app if installed
- Open the Play Store to install it if not found
- Fall back to browser if Play Store is unavailable

## Network Configuration

The app uses HTTP (cleartext traffic) to communicate with Roku devices. This is configured securely:

- **Debug builds only**: Cleartext traffic is permitted for private IP ranges
- **Production builds**: No cleartext traffic allowed
- **Private networks only**: Only 10.0.0.0/8, 172.16.0.0/12, and 192.168.0.0/16 ranges

## Project Structure

```
app/src/main/
├── java/com/example/uniremote/
│   ├── MainActivity.kt          # Main UI with Compose
│   ├── RokuRemote.kt            # Roku HTTP communication
│   ├── IpStorage.kt             # DataStore for IP persistence
│   └── ui/theme/                # Material 3 theme files
├── res/
│   ├── values/
│   │   ├── strings.xml          # App name
│   │   └── themes.xml           # Base theme
│   └── xml/
│       └── network_security_config.xml  # Network security rules
└── AndroidManifest.xml          # App manifest with permissions
```

## Technologies

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: Single-activity
- **Networking**: OkHttp for HTTP requests
- **Storage**: DataStore Preferences
- **Concurrency**: Kotlin Coroutines
- **Build**: Gradle with Kotlin DSL

## Troubleshooting

**"Please enter and save an IP address first"**
- Make sure you've entered the Roku IP and tapped "Save"

**"Test failed: Connection refused/timeout"**
- Verify the IP address is correct
- Ensure your phone and Roku are on the same Wi-Fi network
- Check that your Roku TV is powered on
- Some networks may block device-to-device communication

**Fire TV Remote not launching**
- Install the official app from the Play Store first
- Grant any necessary permissions when prompted

## License

This project is provided as-is for educational and personal use.

## Support

For Roku TV API documentation, visit: https://developer.roku.com/docs/developer-program/dev-tools/external-control-api.md
