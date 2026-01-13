package com.tendertracker.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tendertracker.data.sync.SyncStatus
import java.time.Duration
import java.time.Instant

@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    lastSync: Instant?,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSyncClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status icon
        when (status) {
            SyncStatus.IDLE -> Icon(
                Icons.Default.CloudDone,
                contentDescription = "Synced",
                tint = Color(0xFF4CAF50)
            )
            SyncStatus.SYNCING -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            SyncStatus.ERROR -> Icon(
                Icons.Default.CloudOff,
                contentDescription = "Sync error",
                tint = Color(0xFFF44336)
            )
            SyncStatus.OFFLINE -> Icon(
                Icons.Default.WifiOff,
                contentDescription = "Offline",
                tint = Color(0xFF9E9E9E)
            )
        }

        // Pending badge
        if (pendingCount > 0) {
            Badge {
                Text("$pendingCount")
            }
        }

        // Last sync time
        lastSync?.let { time ->
            Text(
                text = formatRelativeTime(time),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRelativeTime(time: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(time, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        else -> "${duration.toDays()}d ago"
    }
}
