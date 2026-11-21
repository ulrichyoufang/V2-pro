package com.aistudio.examyfua.k7m2px.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.SyncLogEntity
import com.aistudio.examyfua.k7m2px.domain.model.SyncProgressState
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectivityStatusBanner(
    isOnline: Boolean,
    isAuthenticated: Boolean,
    syncState: SyncProgressState,
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectivelyConnected = isOnline && isAuthenticated
    val dotColor = when {
        syncState is SyncProgressState.Syncing -> MaterialTheme.colorScheme.secondary
        syncState is SyncProgressState.Error -> MaterialTheme.colorScheme.error
        effectivelyConnected -> YfuaTheme.colors.success
        else -> YfuaTheme.colors.warning
    }
    val statusText = when {
        syncState is SyncProgressState.Syncing -> syncState.stepLabel
        effectivelyConnected && pendingCount > 0 -> "Connecté • $pendingCount modification(s) en attente"
        effectivelyConnected -> "Connecté"
        else -> "Mode Hors-ligne"
    }
    val icon = when {
        syncState is SyncProgressState.Syncing -> Icons.Filled.CloudSync
        syncState is SyncProgressState.Error -> Icons.Filled.Warning
        effectivelyConnected -> Icons.Filled.CloudDone
        else -> Icons.Filled.CloudOff
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connectivity_status_banner")
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = statusText,
                        tint = dotColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = YfuaTheme.colors.headline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (pendingCount > 0 && effectivelyConnected && syncState !is SyncProgressState.Syncing) {
                    Text(
                        text = "Synchroniser",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = syncState is SyncProgressState.Syncing) {
                if (syncState is SyncProgressState.Syncing) {
                    val progress = if (syncState.totalCount > 0) {
                        syncState.processedCount.toFloat() / syncState.totalCount.toFloat()
                    } else 0f
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncDetailsDialog(
    isOnline: Boolean,
    isAuthenticated: Boolean,
    pendingCount: Int,
    lastSyncEpochMs: Long,
    syncLogs: List<SyncLogEntity>,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = YfuaTheme.colors.headline,
        textContentColor = YfuaTheme.colors.bodySlate,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synchronisation Hybride", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isAuthenticated && isOnline) {
                        "Connecté à Firestore • $pendingCount modification(s) en file d'attente."
                    } else if (!isAuthenticated) {
                        "Mode Hors-ligne : vos données sont stockées localement et prêtes à être liées à un compte Google."
                    } else {
                        "Réseau indisponible : $pendingCount modification(s) seront envoyées automatiquement dès le retour de la connexion via WorkManager."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = YfuaTheme.colors.bodySlate
                )
                if (lastSyncEpochMs > 0L) {
                    Text(
                        text = "Dernière synchronisation : ${formatter.format(Date(lastSyncEpochMs))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = YfuaTheme.colors.success
                    )
                }
                if (syncLogs.isNotEmpty()) {
                    Text(
                        text = "Journal récent :",
                        style = MaterialTheme.typography.labelMedium,
                        color = YfuaTheme.colors.headline
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(syncLogs.take(8), key = { it.id }) { log ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "${log.entityType} • ${log.action}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = log.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = YfuaTheme.colors.headline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAuthenticated && isOnline) {
                Button(
                    onClick = {
                        onSyncNow()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("sync_now_dialog_button")
                ) {
                    Text("Synchroniser maintenant")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = YfuaTheme.colors.bodySlate)
            }
        }
    )
}
