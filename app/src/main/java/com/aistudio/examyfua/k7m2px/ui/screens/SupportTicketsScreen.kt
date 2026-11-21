package com.aistudio.examyfua.k7m2px.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.SupportTicketEntity
import com.aistudio.examyfua.k7m2px.domain.model.TicketStatus
import com.aistudio.examyfua.k7m2px.ui.components.EmptyStateCard
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketsScreen(
    currentUid: String?,
    tickets: List<SupportTicketEntity>,
    onSubmitTicket: (subject: String, message: String) -> Unit,
    onMarkResolved: (ticketId: String) -> Unit,
    onRequestSignIn: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = YfuaTheme.colors.headline,
        unfocusedTextColor = YfuaTheme.colors.headline,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = YfuaTheme.colors.cardBorder,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Support & Aide", color = YfuaTheme.colors.headline) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("support_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (currentUid == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Filled.CloudOff,
                    title = "Connexion requise pour le support",
                    description = "Connectez votre compte pour envoyer une demande et recevoir les réponses en temps réel.",
                    actionLabel = "Se connecter",
                    onActionClick = onRequestSignIn
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Nouvelle demande d'assistance",
                                style = MaterialTheme.typography.titleMedium,
                                color = YfuaTheme.colors.headline
                            )
                        }

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Objet") },
                            colors = fieldColors,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_subject_input")
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Décrivez votre question ou problème") },
                            colors = fieldColors,
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_message_input")
                        )

                        Button(
                            onClick = {
                                if (subject.isNotBlank() && message.isNotBlank()) {
                                    onSubmitTicket(subject.trim(), message.trim())
                                    subject = ""
                                    message = ""
                                }
                            },
                            enabled = subject.isNotBlank() && message.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("support_submit_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Envoyer au support", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    "Mes demandes (${tickets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = YfuaTheme.colors.headline
                )
            }

            if (tickets.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.SupportAgent,
                        title = "Aucune demande envoyée",
                        description = "Vos échanges avec l'administration apparaîtront ici."
                    )
                }
            } else {
                items(tickets, key = { it.id }) { ticket ->
                    val status = TicketStatus.safeValueOf(ticket.status)
                    val (badgeBg, badgeFg) = when (status) {
                        TicketStatus.OUVERT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
                        TicketStatus.REPONDU -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                        TicketStatus.RESOLU -> YfuaTheme.colors.successContainer to YfuaTheme.colors.success
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    ticket.subject,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = YfuaTheme.colors.headline,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(color = badgeBg, shape = RoundedCornerShape(999.dp)) {
                                    Text(
                                        text = status.label,
                                        color = badgeFg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                ticket.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = YfuaTheme.colors.bodySlate
                            )

                            Text(
                                "Mis à jour : ${dateFormat.format(Date(ticket.updatedAtEpochMs))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = YfuaTheme.colors.bodySlate
                            )

                            if (ticket.adminReply.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "Réponse de l'administration",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            ticket.adminReply,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            if (status != TicketStatus.RESOLU) {
                                OutlinedButton(
                                    onClick = { onMarkResolved(ticket.id) },
                                    border = BorderStroke(1.dp, YfuaTheme.colors.success),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = YfuaTheme.colors.success, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Marquer comme résolu", color = YfuaTheme.colors.success)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
