package com.example.uniremote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class RemoteAction(
    val icon: @Composable () -> Unit,
    val contentDescription: String,
    val onClick: () -> Unit,
    val filled: Boolean = false,
    val enabled: Boolean = true
)

@Composable
fun RemoteRow(actions: List<RemoteAction>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        actions.forEachIndexed { index, a ->
            IconButton(
                onClick = a.onClick,
                enabled = a.enabled,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (a.filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (a.filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) { a.icon() }
            if (index < actions.lastIndex) Spacer(Modifier.width(12.dp))
        }
    }
}
