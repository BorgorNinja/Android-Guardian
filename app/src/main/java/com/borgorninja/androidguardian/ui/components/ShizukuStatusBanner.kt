package com.borgorninja.androidguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borgorninja.androidguardian.shizuku.ShizukuState
import com.borgorninja.androidguardian.ui.theme.StatusError
import com.borgorninja.androidguardian.ui.theme.StatusSpeedProfile

/**
 * Persistent banner reflecting the Shizuku lifecycle state machine.
 * Every non-Granted state gets an explicit, actionable message — we never
 * silently disable the optimize button without telling the user why.
 */
@Composable
fun ShizukuStatusBanner(
    state: ShizukuState,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit
) {
    val (message, color) = when (state) {
        ShizukuState.ServiceDead ->
            "Shizuku service not detected. Install and start Shizuku (via ADB or root), then retry." to StatusError
        ShizukuState.Pending ->
            "Shizuku permission not yet requested." to StatusSpeedProfile
        ShizukuState.Denied ->
            "Shizuku permission denied. Grant access to enable compilation." to StatusError
        ShizukuState.Granted ->
            return // No banner needed when fully operational.
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (state == ShizukuState.ServiceDead) {
            TextButton(onClick = onRetry) { Text("Retry") }
        } else {
            Button(onClick = onRequestPermission) { Text("Grant") }
        }
    }
}
