package com.example.uniremote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.uniremote.data.AppSettings
import com.example.uniremote.data.RokuFavorite
import com.example.uniremote.data.saveIp
import com.example.uniremote.data.saveOverlayEnabled
import com.example.uniremote.data.saveFireTvVolumeOverlayEnabled
import com.example.uniremote.net.RokuDevice
import com.example.uniremote.net.RokuDiscovery
import com.example.uniremote.net.withMulticastLock
import com.example.uniremote.net.FireTvClient
import com.example.uniremote.overlay.FloatingAssistService
import kotlinx.coroutines.launch
import android.provider.Settings
import android.content.Intent
import android.content.Context
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenNew(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onValidateRoku: (String) -> Unit
) {
    // Handle system back button
    BackHandler(onBack = onDismiss)
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Devices", "Overlays", "Advanced")
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DevicesTab(currentSettings, onSave, onValidateRoku)
                1 -> OverlaysTab(currentSettings)
                2 -> AdvancedTab(currentSettings, onSave)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesTab(
    currentSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onValidateRoku: (String) -> Unit
) {
    var rokuIp by rememberSaveable { mutableStateOf(currentSettings.rokuIp) }
    var fireTvInput by rememberSaveable { mutableStateOf(currentSettings.fireTvInput) }
    var scanning by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<List<RokuDevice>>(emptyList()) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Roku TV
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tv, "Roku", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Roku TV", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    OutlinedTextField(
                        value = rokuIp,
                        onValueChange = { rokuIp = it },
                        label = { Text("IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (rokuIp.isNotBlank()) {
                                IconButton(onClick = { 
                                    scope.launch { 
                                        context.saveIp(rokuIp)
                                        onValidateRoku(rokuIp)
                                    }
                                }) {
                                    Icon(Icons.Default.Check, "Save")
                                }
                            }
                        }
                    )
                    
                    Button(
                        onClick = {
                            scanning = true
                            scope.launch {
                                scanResults = RokuDiscovery.scan(timeoutMs = 3000)
                                scanning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !scanning
                    ) {
                        if (scanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (scanning) "Scanning..." else "Scan Network")
                    }
                    
                    if (scanResults.isNotEmpty()) {
                        Text("Found ${scanResults.size} device(s):", style = MaterialTheme.typography.labelMedium)
                        scanResults.forEach { device ->
                            OutlinedButton(
                                onClick = {
                                    rokuIp = device.ip
                                    scope.launch {
                                        context.saveIp(device.ip)
                                        onValidateRoku(device.ip)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val displayName = if (!device.name.isNullOrBlank()) device.name else device.ip
                                Text(displayName)
                            }
                        }
                    }
                }
            }
        }
        
        // Fire TV Input
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Input, "Fire TV", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Fire TV HDMI Input", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Text(
                        "Set the Roku HDMI input where Fire TV is connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = fireTvInput,
                        onValueChange = { 
                            fireTvInput = it
                            onSave(currentSettings.copy(fireTvInput = it))
                        },
                        label = { Text("HDMI Input") },
                        placeholder = { Text("tvinput.hdmi1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun OverlaysTab(currentSettings: AppSettings) {
    var overlayEnabled by rememberSaveable { mutableStateOf(currentSettings.overlayEnabled) }
    var fireTvVolOverlayEnabled by rememberSaveable { mutableStateOf(currentSettings.fireTvVolumeOverlayEnabled) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating Assist Dot
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FiberManualRecord,
                            "Assist Dot",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Floating Assist Dot", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Draggable bubble with quick volume controls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = overlayEnabled,
                        onCheckedChange = { enabled ->
                            overlayEnabled = enabled
                            scope.launch { context.saveOverlayEnabled(enabled) }
                            if (enabled) {
                                if (Settings.canDrawOverlays(context)) {
                                    context.startForegroundService(Intent(context, FloatingAssistService::class.java))
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Overlay permission needed") }
                                }
                            } else {
                                context.stopService(Intent(context, FloatingAssistService::class.java))
                            }
                        }
                    )
                }
            }
        }
        
        // Fire TV Volume Overlay
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.VolumeUp,
                            "Volume",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Fire TV Volume Bar", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Persistent volume controls for Fire TV app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = fireTvVolOverlayEnabled,
                        onCheckedChange = { enabled ->
                            fireTvVolOverlayEnabled = enabled
                            scope.launch { context.saveFireTvVolumeOverlayEnabled(enabled) }
                            if (enabled) {
                                if (Settings.canDrawOverlays(context)) {
                                    com.example.uniremote.overlay.FireTvVolumeService.start(context)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Overlay permission needed") }
                                }
                            } else {
                                com.example.uniremote.overlay.FireTvVolumeService.stop(context)
                            }
                        }
                    )
                }
            }
        }
        
        // Permissions
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Permissions Required", style = MaterialTheme.typography.titleSmall)
                    
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Security, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Display Over Other Apps")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Usage Access")
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedTab(
    currentSettings: AppSettings,
    onSave: (AppSettings) -> Unit
) {
    var favorites by remember { mutableStateOf(currentSettings.favorites) }
    var newFavLabel by rememberSaveable { mutableStateOf("") }
    var newFavAppId by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Roku Favorites",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        items(favorites) { fav ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fav.label, style = MaterialTheme.typography.bodyLarge)
                        Text(fav.appId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        favorites = favorites.filter { it != fav }
                        onSave(currentSettings.copy(favorites = favorites))
                    }) {
                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add Favorite", style = MaterialTheme.typography.titleSmall)
                    
                    OutlinedTextField(
                        value = newFavLabel,
                        onValueChange = { newFavLabel = it },
                        label = { Text("Name") },
                        placeholder = { Text("Netflix") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = newFavAppId,
                        onValueChange = { newFavAppId = it },
                        label = { Text("App ID") },
                        placeholder = { Text("12") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            if (newFavLabel.isNotBlank() && newFavAppId.isNotBlank()) {
                                val newFav = RokuFavorite(newFavLabel, newFavAppId)
                                favorites = favorites + newFav
                                onSave(currentSettings.copy(favorites = favorites))
                                newFavLabel = ""
                                newFavAppId = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newFavLabel.isNotBlank() && newFavAppId.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Favorite")
                    }
                }
            }
        }
    }
}
