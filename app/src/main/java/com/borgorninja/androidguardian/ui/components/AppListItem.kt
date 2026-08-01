package com.borgorninja.androidguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.borgorninja.androidguardian.data.AppInfo
import com.borgorninja.androidguardian.data.CompileStatus
import com.borgorninja.androidguardian.ui.theme.StatusError
import com.borgorninja.androidguardian.ui.theme.StatusSpeed
import com.borgorninja.androidguardian.ui.theme.StatusSpeedProfile
import com.borgorninja.androidguardian.ui.theme.StatusVerifyOrOther

@Composable
fun AppListItem(
    app: AppInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(app.icon)
                .crossfade(true)
                .build(),
            contentDescription = app.label,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        StatusBadge(status = app.compileStatus)
    }
}

@Composable
private fun StatusBadge(status: CompileStatus) {
    val (text, color) = when (status) {
        is CompileStatus.Speed -> "speed" to StatusSpeed
        is CompileStatus.SpeedProfile -> "speed-profile" to StatusSpeedProfile
        is CompileStatus.Verify -> "verify" to StatusVerifyOrOther
        is CompileStatus.NotCompiled -> "none" to StatusVerifyOrOther
        is CompileStatus.InProgress -> "…" to StatusSpeedProfile
        is CompileStatus.Error -> "error" to StatusError
        is CompileStatus.Other -> status.raw.take(10) to StatusVerifyOrOther
        CompileStatus.Unknown -> "?" to StatusVerifyOrOther
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
