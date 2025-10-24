package com.example.uniremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.uniremote.data.RemoteMode
import com.example.uniremote.data.UiCommand
import com.example.uniremote.ui.components.DPad

@Composable
fun RemoteScreen(
    mode: RemoteMode,
    onNav: (UiCommand) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onMediaToggle: (() -> Unit)? = null,
    onPower: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Outer container
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Card resembling Roku remote panel
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top controls: Home, Back, Power
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = onHome,
                            enabled = enabled,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Icon(Icons.Outlined.Home, contentDescription = "Home") }
                        IconButton(
                            onClick = onBack,
                            enabled = enabled,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                    }
                    IconButton(
                        onClick = onPower,
                        enabled = enabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = "Power") }
                }

                // Middle: D-Pad centered, volume rail to the side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(8.dp))
                    DPad(
                        onNav = onNav,
                        enabled = enabled
                    )
                    // Vertical volume rail
                    Column(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = onVolumeUp,
                            enabled = enabled,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Volume Up") }
                        IconButton(
                            onClick = onVolumeDown,
                            enabled = enabled,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) { Icon(Icons.AutoMirrored.Outlined.VolumeDown, contentDescription = "Volume Down") }
                    }
                }

                // Playback controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AssistChip(
                        onClick = onPlay,
                        label = { Text("Play") },
                        enabled = enabled,
                        leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) }
                    )
                    Spacer(Modifier.width(12.dp))
                    AssistChip(
                        onClick = onPause,
                        label = { Text("Pause") },
                        enabled = enabled,
                        leadingIcon = { Icon(Icons.Outlined.Pause, contentDescription = null) }
                    )
                }

                // Mode helper text
                Text(
                    text = when (mode) {
                        RemoteMode.ROKU -> "Navigation controls Roku TV"
                        RemoteMode.FIRE_TV -> "Navigation controls Fire TV via Home Assistant"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
