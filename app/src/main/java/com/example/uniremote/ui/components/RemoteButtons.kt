package com.example.uniremote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemoteButtons(
    onHome: () -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onMediaToggle: (() -> Unit)? = null,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onPower: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TonalButton(
                onClick = onHome,
                icon = Icons.Outlined.Home,
                contentDescription = "Home",
                enabled = enabled
            )
            TonalButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                enabled = enabled
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TonalButton(
                onClick = onPlay,
                icon = Icons.Outlined.PlayArrow,
                contentDescription = "Play",
                enabled = enabled
            )
            TonalButton(
                onClick = onPause,
                icon = Icons.Outlined.Pause,
                contentDescription = "Pause",
                enabled = enabled
            )
            if (onMediaToggle != null) {
                TonalButton(
                    onClick = onMediaToggle,
                    icon = Icons.Outlined.PlayArrow,
                    contentDescription = "Media Toggle",
                    enabled = enabled
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledButton(
                onClick = onVolumeUp,
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = "Volume Up",
                enabled = enabled
            )
            FilledButton(
                onClick = onVolumeDown,
                icon = Icons.AutoMirrored.Outlined.VolumeDown,
                contentDescription = "Volume Down",
                enabled = enabled
            )
            FilledButton(
                onClick = onPower,
                icon = Icons.Outlined.PowerSettingsNew,
                contentDescription = "Power",
                enabled = enabled
            )
        }
    }
}

@Composable
private fun TonalButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun FilledButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
