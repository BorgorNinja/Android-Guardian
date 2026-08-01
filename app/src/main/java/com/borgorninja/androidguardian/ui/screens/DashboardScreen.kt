package com.borgorninja.androidguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.borgorninja.androidguardian.data.AppInfo
import com.borgorninja.androidguardian.ui.components.ConsoleLog
import com.borgorninja.androidguardian.ui.viewmodel.ConsoleEntry

@Composable
fun DashboardScreen(
    selectedApp: AppInfo?,
    isCompiling: Boolean,
    consoleLog: List<ConsoleEntry>,
    onOptimize: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedApp == null) {
            Text(
                "Select an app from the list to view its compilation dashboard.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            return@Column
        }

        Text(
            text = selectedApp.label,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = selectedApp.packageName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOptimize,
                enabled = !isCompiling,
                modifier = Modifier.weight(1f)
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = ButtonDefaults.buttonColors().contentColor
                    )
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                }
                Text(if (isCompiling) " Optimizing…" else " Optimize App", maxLines = 1)
            }

            OutlinedButton(onClick = onReset, enabled = !isCompiling) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(" Reset")
            }

            if (isCompiling) {
                OutlinedButton(onClick = onCancel) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text(" Cancel")
                }
            }
        }

        Text(
            text = "Console",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ConsoleLog(
            entries = consoleLog,
            modifier = Modifier.weight(1f)
        )
    }
}
