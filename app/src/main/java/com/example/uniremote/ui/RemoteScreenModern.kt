package com.example.uniremote.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SettingsInputHdmi
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.jithendranara.uniremote.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uniremote.data.RemoteMode
import com.example.uniremote.data.UiCommand
import com.example.uniremote.data.readSettings
import com.example.uniremote.net.validateRoku
import com.example.uniremote.ui.components.DPadModern
import com.example.uniremote.ui.components.StatusPill
import com.example.uniremote.ui.components.StatusState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreenModern(
    mode: RemoteMode,
    onModeChange: (RemoteMode) -> Unit,
    onCommand: (UiCommand) -> Unit,
    onOpenSettings: () -> Unit,
    favorites: List<com.example.uniremote.data.RokuFavorite> = emptyList(),
    onLaunchRoku: (String) -> Unit = {},
    fireTvInput: String = "",
    onSwitchToFireTvInput: () -> Unit = {},
    firePlayback: com.example.uniremote.net.FireTvClient.Playback? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(StatusState.Unknown) }
    LaunchedEffect(mode) {
        status = StatusState.Unknown
        while (true) {
            val s = context.readSettings()
            status = when (mode) {
                RemoteMode.ROKU -> {
                    val ip = s.rokuIp
                    if (ip.isBlank()) StatusState.Unknown else validateRoku(ip).fold(
                        onSuccess = { StatusState.Connected },
                        onFailure = { StatusState.Failed }
                    )
                }
                RemoteMode.FIRE_TV -> {
                    // Fling SDK path: we consider "Ready" (Connected) when a Fire TV receiver id is saved
                    if (s.fireTvId.isBlank()) StatusState.Unknown else StatusState.Connected
                }
            }
            delay(10_000)
        }
    }

    val haptics = com.example.uniremote.ui.haptics.rememberHaptics()
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("UniRemote", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(padding)
                .verticalScroll(scroll)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode toggle
            com.example.uniremote.ui.components.ModeToggleModern(
                selected = mode,
                onSelect = onModeChange
            )

            Crossfade(targetState = mode, label = "modeHelper") { current ->
                Text(
                    text = when (current) {
                        RemoteMode.ROKU -> "Navigation controls Roku TV"
                        RemoteMode.FIRE_TV -> "Play/Pause via Amazon Fling; navigation not supported"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            @OptIn(ExperimentalLayoutApi::class)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top row: Home/Back + Power
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Home + Back
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(
                                onClick = { haptics.tap(); onCommand(UiCommand.Home) },
                                enabled = enabled
                            ) { Icon(Icons.Outlined.Home, contentDescription = "Home") }
                            IconButton(
                                onClick = { haptics.tap(); onCommand(UiCommand.Back) },
                                enabled = enabled
                            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                        }

                        // Center: Quick switch to Fire TV HDMI input (if configured)
                        if (fireTvInput.isNotBlank()) {
                            FilledIconButton(
                                onClick = { haptics.tap(); onSwitchToFireTvInput() },
                                enabled = enabled,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFFFF9900), // Fire TV orange
                                    contentColor = Color.White
                                )
                            ) { 
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_firetv_logo),
                                    contentDescription = "Switch to Fire TV input"
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        // Right: Power
                        IconButton(
                            onClick = { haptics.tap(); onCommand(UiCommand.Power) },
                            enabled = enabled
                        ) { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = "Power") }
                    }

                    // D-Pad
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DPadModern(onNav = { haptics.tap(); onCommand(it) }, enabled = enabled, size = 240)
                    }

                    // TV (Roku) volume
                    Text(text = "TV (Roku)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = { haptics.tap(); onCommand(UiCommand.VolUp) }, enabled = enabled) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Volume Up")
                        }
                        IconButton(onClick = { haptics.tap(); onCommand(UiCommand.VolDown) }, enabled = enabled) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeDown, contentDescription = "Volume Down")
                        }
                    }

                    // Media
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AssistChip(onClick = { haptics.tap(); onCommand(UiCommand.Play) }, label = { Text("Play") }, leadingIcon = { Icon(Icons.Outlined.PlayArrow, null) })
                        AssistChip(onClick = { haptics.tap(); onCommand(UiCommand.Pause) }, label = { Text("Pause") }, leadingIcon = { Icon(Icons.Outlined.Pause, null) })
                    }

                    // Favorites
                    Text(text = "Favorites", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (favorites.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            favorites.forEach { fav ->
                                AssistChip(
                                    onClick = { haptics.tap(); onLaunchRoku(fav.appId) },
                                    label = { Text(fav.label) }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add your favorite Roku apps in Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
                        }
                    }

                    // Fire TV input quick switch
                    if (fireTvInput.isNotBlank()) {
                        AssistChip(
                            onClick = { haptics.tap(); onSwitchToFireTvInput() },
                            label = { Text("Fire TV") },
                            leadingIcon = { 
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_firetv_logo),
                                    contentDescription = null
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFFF9900), // Fire TV orange
                                labelColor = Color.White,
                                leadingIconContentColor = Color.White
                            )
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Set your Fire TV HDMI input in Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
                        }
                    }

                    val pillLabel = when (mode) {
                        RemoteMode.ROKU -> "Roku • ${status.name}"
                        RemoteMode.FIRE_TV -> {
                            val playText = when (firePlayback) {
                                com.example.uniremote.net.FireTvClient.Playback.Playing -> "Playing"
                                com.example.uniremote.net.FireTvClient.Playback.Paused -> "Paused"
                                com.example.uniremote.net.FireTvClient.Playback.Idle, null -> "Idle"
                            }
                            "Fire TV • ${status.name} • $playText"
                        }
                    }
                    StatusPill(label = pillLabel, state = status)
                }
            }
        }
    }
}
