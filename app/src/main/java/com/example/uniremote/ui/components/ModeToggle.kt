package com.example.uniremote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.uniremote.data.RemoteMode
import com.example.uniremote.ui.haptics.rememberHaptics

@Composable
fun ModeToggle(
    selected: RemoteMode,
    onSelect: (RemoteMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeToggleButton(
            icon = Icons.Outlined.Tv,
            label = "Roku",
            selected = selected == RemoteMode.ROKU,
            onClick = {
                if (selected != RemoteMode.ROKU) {
                    haptics.tap()
                    onSelect(RemoteMode.ROKU)
                }
            },
            contentDescription = "Switch to Roku mode"
        )
        Spacer(Modifier.width(8.dp))
        ModeToggleButton(
            icon = Icons.Outlined.VideogameAsset,
            label = "Fire TV",
            selected = selected == RemoteMode.FIRE_TV,
            onClick = {
                if (selected != RemoteMode.FIRE_TV) {
                    haptics.tap()
                    onSelect(RemoteMode.FIRE_TV)
                }
            },
            contentDescription = "Switch to Fire TV mode"
        )
    }
}

@Composable
private fun ModeToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "toggleColor"
    )
    Surface(
        tonalElevation = if (selected) 4.dp else 0.dp,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}
