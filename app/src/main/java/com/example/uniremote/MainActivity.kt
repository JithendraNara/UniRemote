package com.example.uniremote

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.uniremote.data.AppSettings
// import com.example.uniremote.data.FireTvAdbMap // no longer used (HA removed)
import com.example.uniremote.data.RemoteMode
import com.example.uniremote.data.RokuEcpMap
import com.example.uniremote.data.UiCommand
import com.example.uniremote.data.readSettings
import com.example.uniremote.data.saveMode
import com.example.uniremote.data.saveSettings
import com.example.uniremote.net.*
import com.example.uniremote.ui.SettingsScreen
import com.example.uniremote.ui.SettingsScreenNew
import com.example.uniremote.ui.theme.UniRemoteTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.provider.Settings
import android.widget.Toast
import android.content.Intent
import android.net.Uri

import com.example.uniremote.overlay.FloatingAssistService
import com.example.uniremote.data.saveOverlayEnabled

class MainActivity : ComponentActivity() {
    @Volatile
    var lastKnownRokuIp: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniRemoteTheme {
                MainScreen()
            }
        }

        // Ensure floating assist dot runs if overlay permission is granted
        lifecycleScope.launch {
            try {
                val s = readSettings()
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    if (!s.overlayEnabled) {
                        // Persist the user's implicit consent by enabling the overlay setting
                        saveOverlayEnabled(true)
                    }
                    tryStartAssistOverlay()
                } else if (s.overlayEnabled) {
                    // User wanted it enabled; prompt to grant permission
                    tryStartAssistOverlay()
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    private fun tryStartAssistOverlay() {
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, FloatingAssistService::class.java))
        } else {
            // Prompt user to enable overlay permission via system settings
            Toast.makeText(this, "Allow 'Display over other apps' to enable the assistive dot", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    /**
     * Attempts to open the Amazon Fire TV remote app on this phone. If not installed,
     * falls back to a Play Store search so the user can install it quickly.
     */
    fun openFireTvRemoteApp() {
        openFireTvRemoteApp(this)
    }
}

/**
 * Top-level helper that opens the Amazon Fire TV remote app if installed, or routes
 * to the Play Store/web as a fallback. Safe to call from Composables with a Context.
 */
fun openFireTvRemoteApp(context: android.content.Context) {
    val pm = context.packageManager
    fun tip(msg: String) {
        try { android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
    }

    fun startResolvedLauncher(pkg: String): Boolean {
        return try {
            // Resolve the app's actual launcher activity via PM, then start explicitly
            val probe = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
            val ri = pm.resolveActivity(probe, 0)
            if (ri?.activityInfo != null) {
                val component = android.content.ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                tip("Opening Fire TV Remote (A2: resolved component)")
                context.startActivity(intent)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    // A) Try hard-coded explicit components first (most reliable on your device)
    run {
        val explicitComponents = listOf(
            "com.amazon.storm.lightning.client.aosp" to "com.amazon.storm.lightning.client.aosp.MainActivity",
            "com.amazon.storm.lightning.client" to "com.amazon.storm.lightning.client.MainActivity"
        )
        for ((pkg, cls) in explicitComponents) {
            try {
                // Try explicit component
                val component = android.content.ComponentName(pkg, cls)
                val explicit = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                tip("Opening Fire TV Remote (A: explicit component)")
                context.startActivity(explicit)
                return
            } catch (_: Exception) {
                // try next component
                // If the explicit name failed, try resolving via PM for this package before moving on
                if (startResolvedLauncher(pkg)) return
            }
        }
    }

    // B) Try getLaunchIntentForPackage on known candidates
    run {
        val candidates = listOf(
            "com.amazon.storm.lightning.client.aosp",
            "com.amazon.storm.lightning.client",
            "com.amazon.firetv.remote",
            "com.amazon.storm.lightning.client.fireos",
            "com.amazon.storm.lightning",
            "com.amazon.tv.remote"
        )
        for (pkg in candidates) {
            try {
                // Prefer a resolved launcher component for reliability
                if (startResolvedLauncher(pkg)) return
                val launch = pm.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    tip("Opening Fire TV Remote (B: launch intent)")
                    context.startActivity(launch)
                    return
                }
            } catch (_: Exception) { /* try next */ }
        }
    }

    // C) Query CATEGORY_LAUNCHER and pick Amazon packages
    try {
        val launcher = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchables = pm.queryIntentActivities(launcher, 0)
        val prioritized = launchables.firstOrNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: ""
            pkg.startsWith("com.amazon.storm.lightning") || pkg == "com.amazon.firetv.remote"
        }
        if (prioritized != null) {
            val pkg = prioritized.activityInfo.packageName
            val cls = prioritized.activityInfo.name
            val explicit = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(pkg, cls)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try {
                tip("Opening Fire TV Remote (C: launcher query)")
                context.startActivity(explicit)
                return
            } catch (_: Exception) { /* continue */ }
        }
    } catch (_: Exception) { /* continue */ }

    // D) Prefer verified app link if a non-browser can handle it
    try {
        val verifiedUri = Uri.parse("https://getstarted.amazonfiretvapp.com")
        val viewIntent = Intent(Intent.ACTION_VIEW, verifiedUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val allHandlers = pm.queryIntentActivities(viewIntent, 0)
        if (allHandlers.isNotEmpty()) {
            fun isBrowserPackage(pkg: String): Boolean {
                val p = pkg.lowercase()
                return p.contains("chrome") ||
                    p.contains("browser") ||
                    p.contains("mozilla") ||
                    p.contains("firefox") ||
                    p.contains("opera") ||
                    p.contains("vivaldi") ||
                    p.contains("duckduckgo") ||
                    p.contains("brave") ||
                    p.contains("edge") ||
                    p.contains("yandex") ||
                    (p.contains("samsung") && p.contains("browser"))
            }

            val preferred = allHandlers
                .filter { ri -> !isBrowserPackage(ri.activityInfo?.packageName ?: "") }
                .sortedWith(compareBy<android.content.pm.ResolveInfo>({
                    val pkg = it.activityInfo?.packageName ?: ""
                    if (pkg.startsWith("com.amazon")) 0 else 1
                }, {
                    val label = try { it.loadLabel(pm)?.toString()?.lowercase() ?: "" } catch (_: Exception) { "" }
                    if ("fire tv" in label || ("fire" in label && "remote" in label)) 0 else 1
                }))
                .firstOrNull()

            if (preferred != null) {
                val pkg = preferred.activityInfo.packageName
                val cls = preferred.activityInfo.name
                val explicit = Intent(Intent.ACTION_VIEW, verifiedUri)
                    .setClassName(pkg, cls)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    tip("Opening Fire TV Remote (D: verified link handler)")
                    context.startActivity(explicit)
                    return
                } catch (_: Exception) { /* fall through */ }
            }
        }
    } catch (_: Exception) { /* ignore and continue */ }
    // 1) Try known package ids first
    val candidates = listOf(
        "com.amazon.storm.lightning.client",
        "com.amazon.storm.lightning.client.aosp",
        "com.amazon.firetv.remote",
        // Fallback guesses often seen on different builds/locales
        "com.amazon.storm.lightning.client.fireos",
        "com.amazon.storm.lightning",
        "com.amazon.tv.remote"
    )
    for (pkg in candidates) {
        val launch = pm.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launch)
                android.widget.Toast.makeText(context, "Opening Fire TV Remote", android.widget.Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) { /* try next */ }
        }
    }

    // 2) Fuzzy match: look for a launchable activity whose label or package suggests Fire TV Remote
    try {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = pm.queryIntentActivities(launcherIntent, 0)
        val match = infos.firstOrNull { ri ->
            val label = try { ri.loadLabel(pm)?.toString() ?: "" } catch (_: Exception) { "" }
            val pkg = ri.activityInfo?.packageName ?: ""
            val l = label.lowercase()
            val p = pkg.lowercase()
            // Heuristics: label mentions Fire TV/Remote, or Amazon+Fire in package
            ("fire tv" in l || ("fire" in l && "remote" in l)) ||
            ("amazon" in p && ("fire" in p || "storm.lightning" in p) && ("remote" in p || "client" in p))
        }
        if (match != null) {
            val pkg = match.activityInfo.packageName
            pm.getLaunchIntentForPackage(pkg)?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launch)
                    android.widget.Toast.makeText(context, "Opening ${match.loadLabel(pm)}", android.widget.Toast.LENGTH_SHORT).show()
                    return
                } catch (_: Exception) { /* fall through to store */ }
            }
        }
    } catch (_: Exception) { /* ignore and fall through */ }

    // Fallback: Play Store search, then web
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=Amazon%20Fire%20TV%20Remote")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        tip("Opening Play Store search (fallback)")
        context.startActivity(market)
    } catch (_: Exception) {
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=Amazon%20Fire%20TV%20Remote")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            tip("Opening web Play Store (fallback)")
            context.startActivity(web)
        } catch (_: Exception) { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = com.example.uniremote.ui.haptics.rememberHaptics()
    val fireClient = remember { com.example.uniremote.net.FireTvClient(context) }

    var settings by remember { mutableStateOf(AppSettings()) }
    var currentMode by rememberSaveable { mutableStateOf(RemoteMode.ROKU) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var uiMessage by remember { mutableStateOf<com.example.uniremote.ui.UiMessage?>(null) }

    // Load saved settings on first composition
    LaunchedEffect(Unit) {
        settings = context.readSettings()
        currentMode = settings.lastMode
        // Cache for Activity hardware volume handling
        (context as? MainActivity)?.lastKnownRokuIp = settings.rokuIp
    }

    // Show snackbar and haptics for UiMessage
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            when (it) {
                is com.example.uniremote.ui.UiMessage.Success -> {
                    snackbarHostState.showSnackbar(it.message)
                    haptics.success()
                }
                is com.example.uniremote.ui.UiMessage.Error -> {
                    snackbarHostState.showSnackbar(it.message)
                    haptics.error()
                }
            }
            uiMessage = null
        }
    }

    if (showSettings) {
        SettingsScreenNew(
            currentSettings = settings,
            onDismiss = { showSettings = false },
            onSave = { newSettings ->
                scope.launch {
                    context.saveSettings(newSettings)
                    settings = newSettings
                    // Update Activity cache for hardware volume
                    (context as? MainActivity)?.lastKnownRokuIp = newSettings.rokuIp
                    uiMessage = com.example.uniremote.ui.UiMessage.Success("Settings saved")
                }
            },
            onValidateRoku = { ip ->
                scope.launch {
                    validateRoku(ip).fold(
                        onSuccess = { uiMessage = com.example.uniremote.ui.UiMessage.Success("✅ Roku connected successfully!") },
                        onFailure = { uiMessage = com.example.uniremote.ui.UiMessage.Error("❌ Roku validation failed: ${it.message}") }
                    )
                }
            },
            // HA validation removed
        )
    } else {
        // Screen owns its Scaffold; keep SnackbarHost here without a parent Scaffold
        Box(modifier = Modifier.fillMaxSize()) {
            com.example.uniremote.ui.RemoteScreenModern(
                mode = currentMode,
                onModeChange = { mode ->
                    currentMode = mode
                    scope.launch { context.saveMode(mode) }
                },
                onCommand = { command ->
                    if (!isActionEnabled(settings, currentMode)) {
                        haptics.error()
                    } else when (command) {
                        UiCommand.VolUp -> {
                            if (settings.rokuIp.isNotBlank()) {
                                sendRokuVolumeOrPower("VolumeUp", settings.rokuIp, scope) { uiMessage = it }
                            } else haptics.error()
                        }
                        UiCommand.VolDown -> {
                            if (settings.rokuIp.isNotBlank()) {
                                sendRokuVolumeOrPower("VolumeDown", settings.rokuIp, scope) { uiMessage = it }
                            } else haptics.error()
                        }
                        UiCommand.Power -> {
                            if (settings.rokuIp.isNotBlank()) {
                                sendRokuVolumeOrPower("PowerOff", settings.rokuIp, scope) { uiMessage = it }
                            } else haptics.error()
                        }
                        else -> {
                            if (currentMode == RemoteMode.FIRE_TV) {
                                // Attempt to connect and control via Fling SDK
                                if (settings.fireTvId.isBlank()) {
                                    uiMessage = com.example.uniremote.ui.UiMessage.Error("Select a Fire TV in Settings → Scan for Fire TV")
                                    return@RemoteScreenModern
                                }
                                // Connect lazily using the selected id
                                fireClient.connect(com.example.uniremote.net.FireTvClient.Receiver(settings.fireTvId, "", ""))
                                when (command) {
                                    UiCommand.Play -> scope.launch { fireClient.play("") }
                                    UiCommand.Pause -> scope.launch { fireClient.pause() }
                                    else -> {
                                        // Not supported via Fling SDK
                                        uiMessage = com.example.uniremote.ui.UiMessage.Error("Navigation/Home/Back not supported by Fling SDK")
                                        haptics.error()
                                    }
                                }
                            } else {
                                sendNavCommand(currentMode, command, settings, scope) { uiMessage = it }
                            }
                        }
                    }
                },
                onOpenSettings = { showSettings = true },
                favorites = settings.favorites,
                onLaunchRoku = { appId ->
                    if (settings.rokuIp.isBlank()) {
                        uiMessage = com.example.uniremote.ui.UiMessage.Error("Please configure Roku IP in settings")
                    } else {
                        scope.launch {
                            com.example.uniremote.net.launchRoku(settings.rokuIp, appId).fold(
                                onSuccess = { uiMessage = com.example.uniremote.ui.UiMessage.Success("Launching ${settings.favorites.find { it.appId == appId }?.label ?: "app"}") },
                                onFailure = { uiMessage = com.example.uniremote.ui.UiMessage.Error(it.message ?: "Launch failed") }
                            )
                        }
                    }
                },
                fireTvInput = settings.fireTvInput,
                onSwitchToFireTvInput = {
                    val appId = settings.fireTvInput
                    if (appId.isBlank()) {
                        uiMessage = com.example.uniremote.ui.UiMessage.Error("Set Fire TV input in Settings")
                    } else if (settings.rokuIp.isBlank()) {
                        uiMessage = com.example.uniremote.ui.UiMessage.Error("Please configure Roku IP in settings")
                    } else {
                        scope.launch {
                            com.example.uniremote.net.launchRoku(settings.rokuIp, appId).fold(
                                onSuccess = {
                                    uiMessage = com.example.uniremote.ui.UiMessage.Success("Switched to Fire TV input")
                                    // Open local Fire TV remote app so the user can control the TV right away
                                    openFireTvRemoteApp(context)
                                },
                                onFailure = { uiMessage = com.example.uniremote.ui.UiMessage.Error(it.message ?: "Switch failed") }
                            )
                        }
                    }
                },
                firePlayback = run {
                    val playback by fireClient.playback.collectAsState(initial = com.example.uniremote.net.FireTvClient.Playback.Idle)
                    playback
                },
                enabled = isActionEnabled(settings, currentMode)
            )

            // Global Snackbar host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

fun isActionEnabled(settings: AppSettings, mode: RemoteMode): Boolean {
    return when (mode) {
        RemoteMode.ROKU -> settings.rokuIp.isNotBlank()
        RemoteMode.FIRE_TV -> settings.fireTvId.isNotBlank() // no HA dependency for Fling
    }
}

@Composable
fun RemoteButton(
    contentDescription: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Sends navigation commands based on the current mode.
 */
fun sendNavCommand(
    mode: RemoteMode,
    command: UiCommand,
    settings: AppSettings,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (com.example.uniremote.ui.UiMessage) -> Unit
) {
    scope.launch {
        when (mode) {
            RemoteMode.ROKU -> {
                if (settings.rokuIp.isBlank()) {
                    onResult(com.example.uniremote.ui.UiMessage.Error("Please configure Roku IP in settings"))
                    return@launch
                }
                // Use CommandMapper to get the correct Roku ECP key
                val rokuKey = RokuEcpMap[command]
                if (rokuKey == null) {
                    onResult(com.example.uniremote.ui.UiMessage.Error("Command not supported for Roku"))
                    return@launch
                }
                sendRoku(settings.rokuIp, rokuKey).fold(
                    onSuccess = { onResult(com.example.uniremote.ui.UiMessage.Success("Command sent to Roku")) },
                    onFailure = { err ->
                        onResult(
                            com.example.uniremote.ui.UiMessage.Error(
                                err.message ?: "Roku command failed. Check Roku IP and Wi‑Fi."
                            )
                        )
                    }
                )
            }
            RemoteMode.FIRE_TV -> {
                onResult(com.example.uniremote.ui.UiMessage.Error("Navigation/Home/Back not supported by Fire TV Fling SDK"))
            }
        }
    }
}

fun sendRokuVolumeOrPower(
    key: String,
    rokuIp: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (com.example.uniremote.ui.UiMessage) -> Unit
) {
    scope.launch {
        if (rokuIp.isBlank()) {
            onResult(com.example.uniremote.ui.UiMessage.Error("Please configure Roku IP in settings"))
            return@launch
        }
        val result = when (key) {
            "VolumeUp" -> sendRokuVolumeUp(rokuIp)
            "VolumeDown" -> sendRokuVolumeDown(rokuIp)
            "PowerOff" -> sendRokuPowerOff(rokuIp)
            "PowerOn" -> sendRokuPowerOn(rokuIp)
            else -> sendRoku(rokuIp, key)
        }
        result.fold(
            onSuccess = { onResult(com.example.uniremote.ui.UiMessage.Success("Command sent to Roku")) },
            onFailure = { err ->
                onResult(
                    com.example.uniremote.ui.UiMessage.Error(
                        err.message ?: "Roku TV control failed. Check Roku IP and network."
                    )
                )
            }
        )
    }
}

