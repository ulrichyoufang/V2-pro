package com.aistudio.examyfua.k7m2px.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.domain.model.Sm2Quality
import com.aistudio.examyfua.k7m2px.ui.components.EmptyStateCard
import com.aistudio.examyfua.k7m2px.ui.theme.PureWhite
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import java.util.Locale

@Composable
fun FlashcardsSection(
    decks: List<FlashcardDeckEntity>,
    allCards: List<FlashcardEntity>,
    hapticsEnabled: Boolean,
    onCreateDeck: (String, String, String) -> Unit,
    onDeleteDeck: (FlashcardDeckEntity) -> Unit,
    onAddFlashcard: (String, String, String, String) -> Unit,
    onReviewFlashcard: (FlashcardEntity, Sm2Quality, (FlashcardEntity) -> Unit) -> Unit,
    onFinishSession: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var addCardTargetDeckId by remember { mutableStateOf<String?>(null) }
    var activeStudyDeck by remember { mutableStateOf<FlashcardDeckEntity?>(null) }

    val cardsByDeck by remember(allCards) {
        derivedStateOf { allCards.groupBy { it.deckId } }
    }

    AnimatedContent(targetState = activeStudyDeck, label = "flashcards_study_transition") { currentDeck ->
        if (currentDeck != null) {
            val studyCards by remember(cardsByDeck, currentDeck) {
                derivedStateOf { cardsByDeck[currentDeck.localId].orEmpty() }
            }
            FlashcardStudySessionView(
                deck = currentDeck,
                cards = studyCards,
                hapticsEnabled = hapticsEnabled,
                onExit = { activeStudyDeck = null },
                onAddCard = { addCardTargetDeckId = currentDeck.localId },
                onReview = onReviewFlashcard,
                onFinishSession = {
                    onFinishSession()
                    activeStudyDeck = null
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "flashcards_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paquets SM-2 (${decks.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = YfuaTheme.colors.headline
                        )
                        Button(
                            onClick = { showCreateDeckDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("flashcards_new_deck_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nouveau paquet")
                        }
                    }
                }

                if (decks.isEmpty()) {
                    item(key = "flashcards_empty") {
                        EmptyStateCard(
                            icon = Icons.Filled.Style,
                            title = "Aucun paquet de flashcards",
                            description = "Créez des paquets Question / Réponse par matière pour mémoriser durablement.",
                            actionLabel = "Créer un paquet",
                            onActionClick = { showCreateDeckDialog = true }
                        )
                    }
                } else {
                    items(decks, key = { it.localId }) { deck ->
                        val deckCards = cardsByDeck[deck.localId].orEmpty()
                        val now = System.currentTimeMillis()
                        val dueCount = deckCards.count { it.nextReviewAtEpochMs <= now }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(deck.subject, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        Text(deck.title, style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                                        if (deck.description.isNotBlank()) {
                                            Text(deck.description, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDeleteDeck(deck)
                                        }
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Supprimer le paquet", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${deckCards.size} carte(s) • $dueCount à réviser",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (dueCount > 0) YfuaTheme.colors.success else YfuaTheme.colors.bodySlate
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { addCardTargetDeckId = deck.localId },
                                            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = null, tint = YfuaTheme.colors.headline, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Carte", color = YfuaTheme.colors.headline)
                                        }
                                        Button(
                                            onClick = { activeStudyDeck = deck },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Réviser")
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

    if (showCreateDeckDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDeckDialog = false },
            onSave = { title, subject, desc ->
                onCreateDeck(title, subject, desc)
                showCreateDeckDialog = false
            }
        )
    }

    if (addCardTargetDeckId != null) {
        AddFlashcardDialog(
            onDismiss = { addCardTargetDeckId = null },
            onSave = { q, a, hint ->
                onAddFlashcard(addCardTargetDeckId!!, q, a, hint)
                addCardTargetDeckId = null
            }
        )
    }
}

@Composable
private fun FlashcardStudySessionView(
    deck: FlashcardDeckEntity,
    cards: List<FlashcardEntity>,
    hapticsEnabled: Boolean,
    onExit: () -> Unit,
    onAddCard: () -> Unit,
    onReview: (FlashcardEntity, Sm2Quality, (FlashcardEntity) -> Unit) -> Unit,
    onFinishSession: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "flashcard_flip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour aux paquets", tint = YfuaTheme.colors.headline)
                }
                Column {
                    Text(deck.title, style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                    Text(
                        text = if (cards.isEmpty()) "Paquet vide" else "Carte ${currentIndex + 1} / ${cards.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            OutlinedButton(onClick = onAddCard, border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder)) {
                Text("+ Carte", color = YfuaTheme.colors.headline)
            }
        }

        if (cards.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Filled.Style,
                title = "Aucune carte dans ce paquet",
                description = "Ajoutez votre première flashcard pour démarrer.",
                actionLabel = "Ajouter une carte",
                onActionClick = onAddCard
            )
        } else if (currentIndex >= cards.size) {
            EmptyStateCard(
                icon = Icons.Filled.Style,
                title = "Session terminée !",
                description = "Toutes les cartes du paquet ont été évaluées.",
                actionLabel = "Valider (+15 XP)",
                onActionClick = onFinishSession
            )
        } else {
            val card = cards[currentIndex]

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12 * density
                    }
                    .clickable { isFlipped = !isFlipped }
                    .testTag("flashcard_flip_surface")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("QUESTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = card.question,
                                style = MaterialTheme.typography.headlineSmall,
                                color = YfuaTheme.colors.headline,
                                textAlign = TextAlign.Center
                            )
                            if (card.hint.isNotBlank()) {
                                Spacer(modifier = Modifier.height(18.dp))
                                if (showHint) {
                                    Text(
                                        text = "Indice : ${card.hint}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = YfuaTheme.colors.success,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    TextButton(onClick = { showHint = true }) {
                                        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Afficher l'indice", color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        ) {
                            Text("RÉPONSE", style = MaterialTheme.typography.labelSmall, color = YfuaTheme.colors.success)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = card.answer,
                                style = MaterialTheme.typography.titleLarge,
                                color = YfuaTheme.colors.headline,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = String.format(
                                    Locale.FRANCE,
                                    "EF %.2f • Intervalle %d j",
                                    card.easeFactor,
                                    card.intervalDays
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = YfuaTheme.colors.bodySlate
                            )
                        }
                    }
                }
            }

            if (isFlipped) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val buttons = listOf(
                        Sm2Quality.A_REVOIR to MaterialTheme.colorScheme.error,
                        Sm2Quality.DIFFICILE to YfuaTheme.colors.warning,
                        Sm2Quality.BIEN to MaterialTheme.colorScheme.secondary,
                        Sm2Quality.FACILE to YfuaTheme.colors.success
                    )
                    buttons.forEach { (quality, tint) ->
                        Button(
                            onClick = {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReview(card, quality) {
                                    isFlipped = false
                                    showHint = false
                                    currentIndex++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = PureWhite),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(quality.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDeckDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = YfuaTheme.colors.headline,
        unfocusedTextColor = YfuaTheme.colors.headline,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = YfuaTheme.colors.cardBorder,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = YfuaTheme.colors.headline,
        title = { Text("Nouveau paquet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it; err = null }, label = { Text("Titre *") }, colors = colors, singleLine = true)
                OutlinedTextField(value = subject, onValueChange = { subject = it; err = null }, label = { Text("Matière *") }, colors = colors, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optionnel)") }, colors = colors)
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || subject.isBlank()) err = "Titre et matière requis."
                    else onSave(title.trim(), subject.trim(), description.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = YfuaTheme.colors.bodySlate) } }
    )
}

@Composable
private fun AddFlashcardDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = YfuaTheme.colors.headline,
        unfocusedTextColor = YfuaTheme.colors.headline,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = YfuaTheme.colors.cardBorder,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = YfuaTheme.colors.headline,
        title = { Text("Ajouter une flashcard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = question, onValueChange = { question = it; err = null }, label = { Text("Question *") }, colors = colors)
                OutlinedTextField(value = answer, onValueChange = { answer = it; err = null }, label = { Text("Réponse *") }, colors = colors)
                OutlinedTextField(value = hint, onValueChange = { hint = it }, label = { Text("Indice (optionnel)") }, colors = colors)
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isBlank() || answer.isBlank()) err = "Question et réponse requises."
                    else onSave(question.trim(), answer.trim(), hint.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = YfuaTheme.colors.bodySlate) } }
    )
}
