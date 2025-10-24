package com.example.uniremote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniremote.data.UiCommand
import com.example.uniremote.ui.haptics.rememberHaptics

@Composable
fun DPadModern(
    onNav: (UiCommand) -> Unit,
    enabled: Boolean = true,
    size: Int = 200
) {
    val haptics = rememberHaptics()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size.dp)) {
        DPadBtn(
            icon = { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Up") },
            onClick = { haptics.tap(); onNav(UiCommand.Up) },
            modifier = Modifier.align(Alignment.TopCenter),
            enabled = enabled
        )
        DPadBtn(
            icon = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Left") },
            onClick = { haptics.tap(); onNav(UiCommand.Left) },
            modifier = Modifier.align(Alignment.CenterStart),
            enabled = enabled
        )
        DPadBtn(
            icon = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Right") },
            onClick = { haptics.tap(); onNav(UiCommand.Right) },
            modifier = Modifier.align(Alignment.CenterEnd),
            enabled = enabled
        )
        DPadBtn(
            icon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Down") },
            onClick = { haptics.tap(); onNav(UiCommand.Down) },
            modifier = Modifier.align(Alignment.BottomCenter),
            enabled = enabled
        )
        DPadBtn(
            icon = { Icon(Icons.Outlined.RadioButtonChecked, contentDescription = "OK") },
            onClick = { haptics.tap(); onNav(UiCommand.Ok) },
            modifier = Modifier.size(64.dp),
            filled = true,
            enabled = enabled
        )
    }
}

@Composable
private fun DPadBtn(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "dpadModernScale")
    IconButton(
        onClick = {
            pressed = true
            onClick()
            pressed = false
        },
        modifier = modifier
            .size(56.dp)
            .scale(scale),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        )
    ) {
        icon()
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDPadModern() {
    com.example.uniremote.ui.theme.UniRemoteTheme {
        DPadModern(onNav = {}, enabled = true)
    }
}
