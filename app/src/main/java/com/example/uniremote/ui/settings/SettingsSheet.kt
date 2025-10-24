package com.example.uniremote.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.uniremote.data.AppSettings
import com.example.uniremote.net.RokuDevice

@Composable
fun SettingsSheet(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onValidateRoku: (String) -> Unit,
    onValidateHA: (String, String) -> Unit
) {
    // Legacy sheet no longer used. Keep a lightweight stub to avoid build issues.
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Settings have moved.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Use the new Settings screen from the app menu. This legacy sheet is kept as a placeholder.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss) { Text("Close") }
        }
    }
}

@Composable
fun RokuScanResultList(devices: List<RokuDevice>, onSelect: (RokuDevice) -> Unit) {
    // Placeholder to satisfy references; discovery is handled in the new Settings screen.
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Discovery results placeholder (${devices.size})", style = MaterialTheme.typography.bodyMedium)
    }
}
