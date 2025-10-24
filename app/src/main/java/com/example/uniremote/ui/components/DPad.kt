package com.example.uniremote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.uniremote.data.UiCommand

@Composable
fun DPad(
    onNav: (UiCommand) -> Unit,
    enabled: Boolean = true
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        DPadButton(
            icon = Icons.Outlined.KeyboardArrowUp,
            contentDescription = "Up",
            onClick = { onNav(UiCommand.Up) },
            modifier = Modifier.align(Alignment.TopCenter),
            enabled = enabled
        )
        DPadButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = "Left",
            onClick = { onNav(UiCommand.Left) },
            modifier = Modifier.align(Alignment.CenterStart),
            enabled = enabled
        )
        DPadButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Right",
            onClick = { onNav(UiCommand.Right) },
            modifier = Modifier.align(Alignment.CenterEnd),
            enabled = enabled
        )
        DPadButton(
            icon = Icons.Outlined.KeyboardArrowDown,
            contentDescription = "Down",
            onClick = { onNav(UiCommand.Down) },
            modifier = Modifier.align(Alignment.BottomCenter),
            enabled = enabled
        )
        DPadButton(
            icon = Icons.Outlined.RadioButtonChecked,
            contentDescription = "OK",
            onClick = { onNav(UiCommand.Ok) },
            modifier = Modifier.size(64.dp),
            filled = true,
            enabled = enabled
        )
    }
}

@Composable
private fun DPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "dpadScale")
    IconButton(
        onClick = {
            pressed = true
            onClick()
            pressed = false
        },
        modifier = modifier
            .size(if (filled) 56.dp else 48.dp)
            .scale(scale),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
