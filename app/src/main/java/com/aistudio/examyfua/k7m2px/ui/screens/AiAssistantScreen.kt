package com.aistudio.examyfua.k7m2px.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.domain.model.AiChatMessage
import com.aistudio.examyfua.k7m2px.domain.model.AiGenerationState
import com.aistudio.examyfua.k7m2px.domain.model.AiProviderMode
import com.aistudio.examyfua.k7m2px.domain.model.AppConfigState
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme

@Composable
fun AiAssistantScreen(
    profile: StudentProfile?,
    appConfig: AppConfigState,
    isOnline: Boolean,
    isSuperAdmin: Boolean,
    messages: List<AiChatMessage>,
    generationState: AiGenerationState,
    dailyRequestsUsed: Int,
    onSendPrompt: (String) -> Unit,
    onCancelGeneration: () -> Unit,
    onClearConversation: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputPrompt by rememberSaveable { mutableStateOf("") }

    val isGenerating = generationState is AiGenerationState.Loading || generationState is AiGenerationState.Streaming
    val canSend = appConfig.aiEnabled && isOnline && !isGenerating && inputPrompt.isNotBlank()

    LaunchedEffect(messages.size, generationState) {
        if (messages.isNotEmpty() || generationState is AiGenerationState.Streaming) {
            val total = messages.size + if (isGenerating) 1 else 0
            if (total > 0) {
                listState.animateScrollToItem(total - 1)
            }
        }
    }

    val specialtyLabel = profile?.specialty?.displayName ?: "filière technologique"
    val quickPrompts = remember(specialtyLabel) {
        listOf(
            "Explique un concept clé en $specialtyLabel avec des exemples concrets.",
            "Génère un quiz de 5 questions corrigées en $specialtyLabel.",
            "Propose un plan de révision sur 5 jours pour mes examens.",
            "Comment rédiger efficacement mon compte rendu de TP ?"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assistant IA YFUA",
                        style = MaterialTheme.typography.titleMedium,
                        color = YfuaTheme.colors.headline,
                        fontWeight = FontWeight.Bold
                    )
                    val activeModel = if (appConfig.aiProvider == AiProviderMode.CUSTOM_PROXY && appConfig.customApiModel.isNotBlank()) {
                        appConfig.customApiModel
                    } else {
                        appConfig.aiModelName
                    }
                    val quotaText = if (isSuperAdmin) "Illimité (Admin)" else "$dailyRequestsUsed/${appConfig.aiDailyLimit} aujourd'hui"
                    Text(
                        text = "$activeModel • $quotaText",
                        style = MaterialTheme.typography.labelSmall,
                        color = YfuaTheme.colors.bodySlate
                    )
                }
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = onClearConversation,
                        modifier = Modifier.testTag("ai_clear_conversation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Effacer la discussion",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (!appConfig.aiEnabled) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.widthIn(max = 460.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Assistant IA désactivé",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "L'administrateur a temporairement désactivé l'accès à l'assistant IA de la plateforme.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            return@Column
        }

        if (!isOnline) {
            Surface(
                color = YfuaTheme.colors.warningContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = YfuaTheme.colors.warning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connexion Internet requise pour générer des réponses IA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = YfuaTheme.colors.headline
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && !isGenerating) {
                item(key = "ai_welcome_prompts") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Suggestions pour ${profile?.level?.displayName ?: "votre parcours"}",
                            style = MaterialTheme.typography.titleSmall,
                            color = YfuaTheme.colors.headline,
                            fontWeight = FontWeight.SemiBold
                        )
                        quickPrompts.forEachIndexed { idx, suggestion ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isOnline) { onSendPrompt(suggestion) }
                                    .testTag("ai_quick_prompt_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = YfuaTheme.colors.headline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                AiMessageBubble(
                    message = msg,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Réponse YFUA IA", msg.text))
                        onShowMessage("Texte copié dans le presse-papiers.")
                    }
                )
            }

            when (generationState) {
                is AiGenerationState.Loading -> {
                    item(key = "ai_loading_indicator") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Réflexion en cours…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = YfuaTheme.colors.bodySlate
                                )
                            }
                        }
                    }
                }

                is AiGenerationState.Streaming -> {
                    item(key = "ai_streaming_bubble") {
                        AiMessageBubble(
                            message = AiChatMessage(
                                id = "streaming",
                                isFromUser = false,
                                text = generationState.partialText,
                                modelLabel = appConfig.aiModelName
                            ),
                            onCopy = {}
                        )
                    }
                }

                is AiGenerationState.Error -> {
                    item(key = "ai_error_banner") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = generationState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                AiGenerationState.Idle -> Unit
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = { Text("Poser une question de cours ou d'examen…") },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = YfuaTheme.colors.headline,
                        unfocusedTextColor = YfuaTheme.colors.headline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = YfuaTheme.colors.cardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input")
                )

                if (isGenerating) {
                    FilledIconButton(
                        onClick = onCancelGeneration,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.size(48.dp).testTag("ai_stop_button")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Arrêter la génération")
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            val textToSend = inputPrompt.trim()
                            inputPrompt = ""
                            onSendPrompt(textToSend)
                        },
                        enabled = canSend,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(48.dp).testTag("ai_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer à l'IA")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiChatMessage,
    onCopy: () -> Unit
) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else {
        YfuaTheme.colors.cardBorder
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = YfuaTheme.colors.headline
                )
                if (!message.isFromUser && message.id != "streaming") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.modelLabel.ifBlank { "Gemini" },
                            style = MaterialTheme.typography.labelSmall,
                            color = YfuaTheme.colors.bodySlate
                        )
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copier la réponse",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
