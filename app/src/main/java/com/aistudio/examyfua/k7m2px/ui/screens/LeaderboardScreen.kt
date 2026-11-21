package com.aistudio.examyfua.k7m2px.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.remote.RemoteLeaderboardEntry
import com.aistudio.examyfua.k7m2px.data.remote.RemotePeerNudge
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.ui.components.EmptyStateCard
import com.aistudio.examyfua.k7m2px.ui.components.StudentAvatar
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.aistudio.examyfua.k7m2px.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    currentUid: String?,
    profile: StudentProfile?,
    leaderboardState: UiState<List<RemoteLeaderboardEntry>>,
    receivedNudgesState: UiState<List<RemotePeerNudge>>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSendPeerNudge: (recipientUid: String, recipientName: String, specialty: String, isReminder: Boolean) -> Unit,
    onMarkNudgeRead: (String) -> Unit,
    onRequestSignIn: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    if (currentUid == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            EmptyStateCard(
                icon = Icons.Filled.CloudOff,
                title = "Classement disponible en mode connecté",
                description = "Connectez votre compte pour consulter le classement et encourager vos camarades.",
                actionLabel = "Se connecter",
                onActionClick = onRequestSignIn
            )
        }
        return
    }

    val unreadNudges by remember(receivedNudgesState) {
        derivedStateOf {
            when (receivedNudgesState) {
                is UiState.Success -> receivedNudgesState.data.filter { !it.read }
                else -> emptyList()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Classement Général") },
                modifier = Modifier.testTag("leaderboard_tab_general")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Ma Filière") },
                modifier = Modifier.testTag("leaderboard_tab_specialty")
            )
        }

        AnimatedContent(targetState = leaderboardState, label = "leaderboard_state_anim") { state ->
            when (state) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Chargement du classement…", color = YfuaTheme.colors.bodySlate)
                    }
                }

                is UiState.Error -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.EmojiEvents,
                            title = "Classement indisponible",
                            description = state.message
                        )
                    }
                }

                is UiState.Success -> {
                    val mySpecialty = profile?.specialty?.displayName ?: ""
                    val entries by remember(state.data, selectedTab, mySpecialty) {
                        derivedStateOf {
                            if (selectedTab == 0) state.data else state.data.filter { it.specialty == mySpecialty }
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (unreadNudges.isNotEmpty()) {
                                item(key = "nudges_header") {
                                    Text(
                                        text = "Messages d'entraide (${unreadNudges.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                itemsIndexed(unreadNudges, key = { _, n -> "nudge_${n.id}" }) { _, nudge ->
                                    Surface(
                                        color = YfuaTheme.colors.successContainer,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, YfuaTheme.colors.success.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (nudge.type == "REMINDER") "Rappel d'étude" else "Encouragement",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = YfuaTheme.colors.success
                                                )
                                                Text(nudge.message, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.onSuccessContainer)
                                            }
                                            IconButton(onClick = { onMarkNudgeRead(nudge.id) }) {
                                                Icon(Icons.Filled.Check, contentDescription = "Marquer comme lu", tint = YfuaTheme.colors.success)
                                            }
                                        }
                                    }
                                }
                            }

                            if (entries.isEmpty()) {
                                item(key = "leaderboard_empty") {
                                    EmptyStateCard(
                                        icon = Icons.Filled.EmojiEvents,
                                        title = "Aucun étudiant classé",
                                        description = if (selectedTab == 1) {
                                            "Aucun autre étudiant inscrit en $mySpecialty."
                                        } else {
                                            "Synchronisez votre progression pour apparaître dans le classement."
                                        }
                                    )
                                }
                            } else {
                                itemsIndexed(entries, key = { _, e -> e.userId }) { index, entry ->
                                    val isMe = entry.userId == currentUid
                                    val rank = index + 1
                                    Surface(
                                        color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isMe || rank <= 3) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else YfuaTheme.colors.cardBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#$rank",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = if (rank <= 3) MaterialTheme.colorScheme.tertiary else YfuaTheme.colors.bodySlate,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(40.dp)
                                                )
                                                StudentAvatar(initials = entry.initials, size = 44.dp)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isMe) "${entry.displayName} (Vous)" else entry.displayName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = YfuaTheme.colors.headline,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = entry.specialty,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${entry.level} • ${entry.totalStudyMinutes / 60}h ${entry.totalStudyMinutes % 60}m",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = YfuaTheme.colors.bodySlate,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(10.dp)) {
                                                    Text(
                                                        text = "${entry.xpPoints} XP",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }

                                            if (selectedTab == 1 && !isMe) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = { onSendPeerNudge(entry.userId, entry.displayName, entry.specialty, true) },
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Rappel", color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                    Button(
                                                        onClick = { onSendPeerNudge(entry.userId, entry.displayName, entry.specialty, false) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = YfuaTheme.colors.success, contentColor = YfuaTheme.colors.onSuccess),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Encourager", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
