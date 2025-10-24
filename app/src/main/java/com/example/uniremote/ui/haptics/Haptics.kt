package com.example.uniremote.ui.haptics


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback




class Haptics(private val haptic: HapticFeedback) {
    fun tap() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    fun success() = tap()
    fun error() = tap()
}

@Composable
fun rememberHaptics(): Haptics {
    val haptic = LocalHapticFeedback.current
    return remember { Haptics(haptic) }
}
