#!/usr/bin/env bash
# Auto-finds Android SDK and invokes adb without requiring PATH setup.

set -euo pipefail

# Detect Android SDK location
find_sdk() {
    # 1. Check ANDROID_HOME environment variable
    if [[ -n "${ANDROID_HOME:-}" ]]; then
        echo "$ANDROID_HOME"
        return
    fi
    
    # 2. Check local.properties
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local project_root="$(cd "$script_dir/.." && pwd)"
    local local_props="$project_root/local.properties"
    
    if [[ -f "$local_props" ]]; then
        local sdk_dir=$(grep "^sdk.dir=" "$local_props" | cut -d'=' -f2- | tr -d '\r')
        if [[ -n "$sdk_dir" ]]; then
            # Handle escaped characters and backslashes (Windows paths)
            sdk_dir=$(echo "$sdk_dir" | sed 's/\\\\/\//g' | sed 's/\\//g')
            echo "$sdk_dir"
            return
        fi
    fi
    
    # 3. Use platform-specific defaults
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "$HOME/Library/Android/sdk"
    else
        echo "$HOME/Android/Sdk"
    fi
}

SDK="$(find_sdk)"
ADB="$SDK/platform-tools/adb"

# Check if adb exists
if [[ ! -f "$ADB" ]]; then
    echo "❌ adb not found at: $ADB" >&2
    echo "" >&2
    echo "Please install Android SDK Platform-tools:" >&2
    echo "  • Open Android Studio → SDK Manager → SDK Tools → Android SDK Platform-Tools" >&2
    echo "  • Or run: brew install android-platform-tools (macOS)" >&2
    echo "" >&2
    echo "SDK path used: $SDK" >&2
    echo "Set ANDROID_HOME or update local.properties if this is incorrect." >&2
    exit 1
fi

# Execute adb with all passed arguments
exec "$ADB" "$@"
