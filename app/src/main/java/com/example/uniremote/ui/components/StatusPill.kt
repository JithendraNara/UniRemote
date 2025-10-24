package com.example.uniremote.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class StatusState { Connected, Unknown, Failed }

@Composable
fun StatusPill(
    label: String,
    state: StatusState,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (state) {
        StatusState.Connected -> Color(0xFF2E7D32) to Color.White // green
        StatusState.Unknown -> Color(0xFFFFC107) to Color.Black   // amber
        StatusState.Failed -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label, color = fg) },
        modifier = modifier.padding(top = 4.dp),
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = bg,
            disabledLabelColor = fg
        )
    )
}
