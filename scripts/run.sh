#!/usr/bin/env bash
# Builds and runs UniRemote on a connected device or emulator.
# Auto-starts an emulator if no devices are connected.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ADB="$SCRIPT_DIR/adb.sh"

echo "🚀 UniRemote Build & Run"
echo ""

# Detect Android SDK (same logic as adb.sh)
find_sdk() {
    if [[ -n "${ANDROID_HOME:-}" ]]; then
        echo "$ANDROID_HOME"
        return
    fi
    
    local local_props="$PROJECT_ROOT/local.properties"
    if [[ -f "$local_props" ]]; then
        local sdk_dir=$(grep "^sdk.dir=" "$local_props" | cut -d'=' -f2- | tr -d '\r')
        if [[ -n "$sdk_dir" ]]; then
            sdk_dir=$(echo "$sdk_dir" | sed 's/\\\\/\//g' | sed 's/\\//g')
            echo "$sdk_dir"
            return
        fi
    fi
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "$HOME/Library/Android/sdk"
    else
        echo "$HOME/Android/Sdk"
    fi
}

SDK="$(find_sdk)"
EMULATOR="$SDK/emulator/emulator"

# Check for connected devices
check_devices() {
    local devices=$("$ADB" devices | grep -v "List of devices" | grep -v "^$" | grep -v "unauthorized" || true)
    if [[ -n "$devices" ]]; then
        echo "✅ Device(s) found:"
        echo "$devices"
        return 0
    fi
    return 1
}

# Start an emulator if needed
start_emulator() {
    echo "📱 No devices connected. Checking for emulators..."
    
    if [[ ! -f "$EMULATOR" ]]; then
        echo "❌ Emulator not found at: $EMULATOR" >&2
        echo "" >&2
        echo "Please either:" >&2
        echo "  • Connect a physical device via USB" >&2
        echo "  • Enable wireless debugging on your device" >&2
        echo "  • Install an Android Virtual Device (AVD) via Android Studio" >&2
        exit 1
    fi
    
    # Check if an emulator is already running
    local running_devices=$("$ADB" devices | grep "emulator-" || true)
    if [[ -n "$running_devices" ]]; then
        echo "✅ Emulator already running"
        return 0
    fi
    
    # List available AVDs
    local avds=$("$EMULATOR" -list-avds 2>/dev/null || true)
    if [[ -z "$avds" ]]; then
        echo "❌ No Android Virtual Devices (AVDs) found." >&2
        echo "" >&2
        echo "Please create an AVD in Android Studio:" >&2
        echo "  Tools → Device Manager → Create Virtual Device" >&2
        exit 1
    fi
    
    # Pick the first AVD
    local first_avd=$(echo "$avds" | head -n 1)
    echo "🎯 Starting emulator: $first_avd"
    
    # Start emulator in background (headless if possible)
    "$EMULATOR" -avd "$first_avd" -no-snapshot-save -no-boot-anim > /dev/null 2>&1 &
    
    echo "⏳ Waiting for emulator to boot..."
    "$ADB" wait-for-device
    
    # Wait a bit more for full boot
    sleep 5
    
    # Wait for boot to complete
    echo "⏳ Waiting for system to be ready..."
    local boot_complete=""
    for i in {1..30}; do
        boot_complete=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
        if [[ "$boot_complete" == "1" ]]; then
            break
        fi
        sleep 2
    done
    
    if [[ "$boot_complete" != "1" ]]; then
        echo "⚠️  Emulator may not be fully booted, but continuing anyway..."
    fi
    
    echo "✅ Emulator ready"
}

# Main execution
cd "$PROJECT_ROOT"

# Check for devices, start emulator if needed
if ! check_devices; then
    start_emulator
fi

echo ""
echo "🔨 Building debug APK..."
./gradlew assembleDebug

echo ""
echo "📦 Installing app..."
./gradlew installDebug

echo ""
echo "🎬 Launching UniRemote..."
"$ADB" shell am start -n com.example.uniremote/.MainActivity

echo ""
echo "✨ App launched successfully!"
echo ""
echo "📱 Running on:"
"$ADB" devices -l | grep -v "List of devices" | grep -v "^$"
