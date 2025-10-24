package com.example.uniremote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun ModeToggleModern(
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SegmentedButton(
            icon = { Icon(Icons.Outlined.Tv, contentDescription = null) },
            label = "Roku",
            selected = selected == RemoteMode.ROKU,
            onClick = {
                if (selected != RemoteMode.ROKU) {
                    haptics.tap(); onSelect(RemoteMode.ROKU)
                }
            },
            contentDescription = "Switch to Roku mode"
        )
        Spacer(Modifier.width(8.dp))
        SegmentedButton(
            icon = { Icon(Icons.Outlined.VideogameAsset, contentDescription = null) },
            label = "Fire TV",
            selected = selected == RemoteMode.FIRE_TV,
            onClick = {
                if (selected != RemoteMode.FIRE_TV) {
                    haptics.tap(); onSelect(RemoteMode.FIRE_TV)
                }
            },
            contentDescription = "Switch to Fire TV mode"
        )
    }
}

@Composable
private fun SegmentedButton(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "segmentedColor"
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
            icon()
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}
