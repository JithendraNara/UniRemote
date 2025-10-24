package com.example.uniremote.data

/**
 * Unified command interface for remote control actions.
 * Maps UI commands to platform-specific key codes.
 */
sealed interface UiCommand {
    object Home : UiCommand
    object Back : UiCommand
    object Up : UiCommand
    object Down : UiCommand
    object Left : UiCommand
    object Right : UiCommand
    object Ok : UiCommand
    object Play : UiCommand
    object Pause : UiCommand
    object VolUp : UiCommand
    object VolDown : UiCommand
    object Power : UiCommand
}

/**
 * Roku ECP (External Control Protocol) key mapping.
 * Keys are case-sensitive and must match Roku's exact format.
 * Reference: https://developer.roku.com/docs/developer-program/dev-tools/external-control-api.md
 */
object RokuEcpMap : Map<UiCommand, String> by mapOf(
    UiCommand.Home to "Home",
    UiCommand.Back to "Back",
    UiCommand.Up to "Up",
    UiCommand.Down to "Down",
    UiCommand.Left to "Left",
    UiCommand.Right to "Right",
    UiCommand.Ok to "Select",
    UiCommand.Play to "Play",
    UiCommand.Pause to "Pause",
    UiCommand.VolUp to "VolumeUp",
    UiCommand.VolDown to "VolumeDown",
    UiCommand.Power to "PowerOff"
)

/**
 * Fire TV ADB key event mapping.
 * All keys must be in ALL CAPS for ADB input keyevent commands.
 */
object FireTvAdbMap : Map<UiCommand, String> by mapOf(
    UiCommand.Home to "HOME",
    UiCommand.Back to "BACK",
    UiCommand.Up to "UP",
    UiCommand.Down to "DOWN",
    UiCommand.Left to "LEFT",
    UiCommand.Right to "RIGHT",
    UiCommand.Ok to "CENTER",
    UiCommand.Play to "PLAY",
    UiCommand.Pause to "PAUSE",
    UiCommand.VolUp to "VOLUME_UP",    // Not used (routed to Roku)
    UiCommand.VolDown to "VOLUME_DOWN", // Not used (routed to Roku)
    UiCommand.Power to "POWER"          // Not used (routed to Roku)
)

/**
 * Simple validation tests for command mappings.
 * Ensures all required keys are present with correct formatting.
 */
object CommandMapperTests {
    fun validateRokuMap(): Boolean {
        val expectedKeys = setOf(
            "Home", "Back", "Up", "Down", "Left", "Right", "Select",
            "Play", "Pause", "VolumeUp", "VolumeDown", "PowerOff"
        )
        val actualKeys = RokuEcpMap.values.toSet()
        return expectedKeys == actualKeys
    }

    fun validateFireTvMap(): Boolean {
        val expectedKeys = setOf(
            "HOME", "BACK", "UP", "DOWN", "LEFT", "RIGHT", "CENTER",
            "PLAY", "PAUSE", "VOLUME_UP", "VOLUME_DOWN", "POWER"
        )
        val actualKeys = FireTvAdbMap.values.toSet()
        return expectedKeys == actualKeys
    }

    fun runTests(): String {
        val rokuValid = validateRokuMap()
        val fireTvValid = validateFireTvMap()
        return buildString {
            appendLine("CommandMapper Tests:")
            appendLine("  Roku ECP Map: ${if (rokuValid) "✓ PASS" else "✗ FAIL"}")
            appendLine("  Fire TV ADB Map: ${if (fireTvValid) "✓ PASS" else "✗ FAIL"}")
            if (rokuValid && fireTvValid) {
                appendLine("All command mappings validated successfully!")
            }
        }
    }
}
