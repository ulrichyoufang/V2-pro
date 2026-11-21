package com.aistudio.examyfua.k7m2px.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.domain.model.PomodoroMode
import com.aistudio.examyfua.k7m2px.domain.model.Sm2Quality
import com.aistudio.examyfua.k7m2px.ui.theme.JetBrainsMonoFamily
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.aistudio.examyfua.k7m2px.ui.viewmodel.PomodoroTimerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    pomodoroState: PomodoroTimerState,
    pomodoroHistory: List<PomodoroSessionEntity>,
    decks: List<FlashcardDeckEntity>,
    flashcards: List<FlashcardEntity>,
    hapticsEnabled: Boolean,
    onSelectMode: (PomodoroMode, Int, Int) -> Unit,
    onSubjectChange: (String) -> Unit,
    onToggleRunning: () -> Unit,
    onResetPomodoro: () -> Unit,
    onCompleteCycleNow: () -> Unit,
    onCreateDeck: (String, String, String) -> Unit,
    onDeleteDeck: (FlashcardDeckEntity) -> Unit,
    onAddFlashcard: (String, String, String, String) -> Unit,
    onReviewFlashcard: (FlashcardEntity, Sm2Quality, (FlashcardEntity) -> Unit) -> Unit,
    onFinishFlashcardSession: () -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Pomodoro") },
                icon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                modifier = Modifier.testTag("focus_tab_pomodoro")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Flashcards SM-2") },
                icon = { Icon(Icons.Filled.Style, contentDescription = null) },
                modifier = Modifier.testTag("focus_tab_flashcards")
            )
        }

        AnimatedContent(targetState = selectedSubTab, label = "focus_subtab") { tab ->
            if (tab == 0) {
                PomodoroSubScreen(
                    state = pomodoroState,
                    history = pomodoroHistory,
                    onSelectMode = onSelectMode,
                    onSubjectChange = onSubjectChange,
                    onToggleRunning = onToggleRunning,
                    onResetPomodoro = onResetPomodoro,
                    onCompleteCycleNow = onCompleteCycleNow
                )
            } else {
                FlashcardsSection(
                    decks = decks,
                    allCards = flashcards,
                    hapticsEnabled = hapticsEnabled,
                    onCreateDeck = onCreateDeck,
                    onDeleteDeck = onDeleteDeck,
                    onAddFlashcard = onAddFlashcard,
                    onReviewFlashcard = onReviewFlashcard,
                    onFinishSession = onFinishFlashcardSession
                )
            }
        }
    }
}

@Composable
private fun PomodoroSubScreen(
    state: PomodoroTimerState,
    history: List<PomodoroSessionEntity>,
    onSelectMode: (PomodoroMode, Int, Int) -> Unit,
    onSubjectChange: (String) -> Unit,
    onToggleRunning: () -> Unit,
    onResetPomodoro: () -> Unit,
    onCompleteCycleNow: () -> Unit
) {
    var customFocus by remember { mutableIntStateOf(35) }
    var customBreak by remember { mutableIntStateOf(7) }
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE) }

    val mins = state.remainingSeconds / 60
    val secs = state.remainingSeconds % 60
    val formattedTime = String.format(Locale.FRANCE, "%02d:%02d", mins, secs)
    val sweepFraction = if (state.totalPhaseSeconds > 0) {
        state.remainingSeconds.toFloat() / state.totalPhaseSeconds.toFloat()
    } else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "pomo_modes") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroMode.entries.forEach { mode ->
                    val selected = state.mode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectMode(mode, customFocus, customBreak) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = YfuaTheme.colors.bodySlate
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = YfuaTheme.colors.cardBorder
                        )
                    )
                }
            }
        }

        if (state.mode == PomodoroMode.PERSONNALISE && !state.isRunning) {
            item(key = "pomo_custom_sliders") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Travail : $customFocus min", color = YfuaTheme.colors.headline, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = customFocus.toFloat(),
                            onValueChange = {
                                customFocus = it.toInt()
                                onSelectMode(PomodoroMode.PERSONNALISE, customFocus, customBreak)
                            },
                            valueRange = 5f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text("Pause : $customBreak min", color = YfuaTheme.colors.bodySlate, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = customBreak.toFloat(),
                            onValueChange = {
                                customBreak = it.toInt()
                                onSelectMode(PomodoroMode.PERSONNALISE, customFocus, customBreak)
                            },
                            valueRange = 1f..30f,
                            colors = SliderDefaults.colors(
                                thumbColor = YfuaTheme.colors.success,
                                activeTrackColor = YfuaTheme.colors.success
                            )
                        )
                    }
                }
            }
        }

        item(key = "pomo_subject") {
            OutlinedTextField(
                value = state.subject,
                onValueChange = onSubjectChange,
                label = { Text("Matière étudiée") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = YfuaTheme.colors.headline,
                    unfocusedTextColor = YfuaTheme.colors.headline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = YfuaTheme.colors.cardBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth().testTag("pomodoro_subject_input")
            )
        }

        item(key = "pomo_timer_card") {
            val trackColor = MaterialTheme.colorScheme.primaryContainer
            val ringColor = if (state.isBreakPhase) YfuaTheme.colors.success else MaterialTheme.colorScheme.primary

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.isBreakPhase) "PHASE DE PAUSE" else "SESSION CONCENTRATION (+25 XP)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isBreakPhase) YfuaTheme.colors.success else MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * sweepFraction.coerceIn(0f, 1f),
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = formattedTime,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = YfuaTheme.colors.headline
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onToggleRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp).testTag("pomodoro_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isRunning) "Pause" else "Démarrer", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onResetPomodoro,
                            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).testTag("pomodoro_reset_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Réinitialiser", tint = YfuaTheme.colors.headline)
                        }
                        OutlinedButton(
                            onClick = onCompleteCycleNow,
                            border = BorderStroke(1.dp, YfuaTheme.colors.success),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).testTag("pomodoro_complete_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Valider le cycle", tint = YfuaTheme.colors.success)
                        }
                    }
                }
            }
        }

        item(key = "pomo_history_header") {
            Text(
                text = "Historique récent (${history.size})",
                style = MaterialTheme.typography.titleMedium,
                color = YfuaTheme.colors.headline
            )
        }

        items(history.take(10), key = { it.localId }) { session ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(session.subject, style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                        Text(
                            "${session.focusMinutes} min • ${fmt.format(Date(session.completedAtEpochMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = YfuaTheme.colors.bodySlate
                        )
                    }
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "+${session.xpEarned} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
