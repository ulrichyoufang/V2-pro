package com.aistudio.examyfua.k7m2px.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.domain.model.PlanningItem
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.ui.components.EmptyStateCard
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PlanningFilter(val label: String) {
    TOUT("Tout"),
    AUJOURD_HUI("Aujourd'hui"),
    OFFICIEL("Officiel"),
    EXAMENS("Examens"),
    EN_RETARD("En retard"),
    TERMINE("Terminé")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    planningItems: List<PlanningItem>,
    tasks: List<TaskEntity>,
    searchQuery: String,
    debouncedQuery: String,
    isRefreshing: Boolean,
    hapticsEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSaveTask: (String?, String, String, TaskCategory, Long, TaskPriority, Boolean) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var activeFilter by remember { mutableStateOf(PlanningFilter.TOUT) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    val tasksById = remember(tasks) { tasks.associateBy { it.localId } }

    val filteredItems by remember(planningItems, debouncedQuery, activeFilter) {
        derivedStateOf {
            val now = System.currentTimeMillis()
            val endOfToday = now + 86_400_000L
            planningItems.filter { item ->
                val matchesSearch = debouncedQuery.isBlank() ||
                    item.title.contains(debouncedQuery, ignoreCase = true) ||
                    item.subject.contains(debouncedQuery, ignoreCase = true) ||
                    item.roomLocation.contains(debouncedQuery, ignoreCase = true)

                val matchesFilter = when (activeFilter) {
                    PlanningFilter.TOUT -> true
                    PlanningFilter.AUJOURD_HUI -> item.dueDateEpochMs <= endOfToday && !item.isCompleted
                    PlanningFilter.OFFICIEL -> item.isOfficial
                    PlanningFilter.EXAMENS -> item.category == TaskCategory.EXAMEN
                    PlanningFilter.EN_RETARD -> !item.isOfficial && !item.isCompleted && item.dueDateEpochMs < now
                    PlanningFilter.TERMINE -> item.isCompleted
                }
                matchesSearch && matchesFilter
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("planning_add_task_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une tâche")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Planning & Tâches",
                    style = MaterialTheme.typography.headlineSmall,
                    color = YfuaTheme.colors.headline
                )
                IconButton(
                    onClick = {
                        val export = buildString {
                            appendLine("=== Emploi du temps YFUA ===")
                            val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                            filteredItems.forEach { t ->
                                val tag = if (t.isOfficial) "Officiel" else if (t.isCompleted) "Terminé" else "En cours"
                                appendLine("• [${t.category.label}] ${t.title} - ${t.subject} (${fmt.format(Date(t.dueDateEpochMs))}) [$tag]")
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, export)
                        }
                        context.startActivity(Intent.createChooser(intent, "Exporter le planning"))
                    },
                    modifier = Modifier.testTag("planning_share_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Partager le planning", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Rechercher un cours, TD, TP ou examen…", color = YfuaTheme.colors.bodySlate) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = YfuaTheme.colors.headline,
                    unfocusedTextColor = YfuaTheme.colors.headline,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = YfuaTheme.colors.cardBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("planning_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlanningFilter.entries.forEach { filter ->
                    val selected = activeFilter == filter
                    val isExamFilter = filter == PlanningFilter.EXAMENS
                    FilterChip(
                        selected = selected,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isExamFilter) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = if (isExamFilter) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary,
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

            Spacer(modifier = Modifier.height(12.dp))

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                if (filteredItems.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item(key = "planning_empty") {
                            EmptyStateCard(
                                icon = Icons.Filled.EventNote,
                                title = "Aucun élément",
                                description = "Ajoutez une tâche personnelle ou actualisez pour charger votre emploi du temps officiel.",
                                actionLabel = "Nouvelle tâche",
                                onActionClick = {
                                    editingTask = null
                                    showTaskDialog = true
                                }
                            )
                        }
                    }
                } else {
                    val nowEpochMs = System.currentTimeMillis()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            if (item.isOfficial) {
                                OfficialScheduleRowCard(item = item)
                            } else {
                                val entity = tasksById[item.id]
                                if (entity != null) {
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            when (value) {
                                                SwipeToDismissBoxValue.StartToEnd -> {
                                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onToggleComplete(entity)
                                                    false
                                                }
                                                SwipeToDismissBoxValue.EndToStart -> {
                                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onDeleteTask(entity)
                                                    true
                                                }
                                                SwipeToDismissBoxValue.Settled -> false
                                            }
                                        }
                                    )
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {
                                            val bgColor = when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.StartToEnd -> YfuaTheme.colors.success
                                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                            }
                                            val align = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                                Alignment.CenterStart
                                            } else {
                                                Alignment.CenterEnd
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(bgColor, RoundedCornerShape(16.dp))
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = align
                                            ) {
                                                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                                    Icon(Icons.Filled.Check, contentDescription = "Terminer", tint = Color.White)
                                                } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Color.White)
                                                }
                                            }
                                        }
                                    ) {
                                        TaskRowCard(
                                            task = entity,
                                            nowEpochMs = nowEpochMs,
                                            onToggle = {
                                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onToggleComplete(entity)
                                            },
                                            onEdit = {
                                                editingTask = entity
                                                showTaskDialog = true
                                            },
                                            onDelete = {
                                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onDeleteTask(entity)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            initial = editingTask,
            onDismiss = { showTaskDialog = false },
            onSave = { title, subject, category, dueMs, priority, scheduleNotif ->
                onSaveTask(editingTask?.localId, title, subject, category, dueMs, priority, scheduleNotif)
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun OfficialScheduleRowCard(item: PlanningItem) {
    val fmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.FRANCE) }
    val isExam = item.category == TaskCategory.EXAMEN
    val badgeContainer = if (isExam) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val badgeContent = if (isExam) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = "Officiel",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "Officiel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Surface(color = badgeContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = item.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeContent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                Text(
                    text = "${item.subject} • ${fmt.format(Date(item.dueDateEpochMs))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                val extra = listOfNotNull(
                    item.roomLocation.takeIf { it.isNotBlank() }?.let { "Salle : $it" },
                    item.instructor.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (extra.isNotBlank()) {
                    Text(extra, style = MaterialTheme.typography.labelSmall, color = YfuaTheme.colors.bodySlate)
                }
            }
        }
    }
}

@Composable
private fun TaskRowCard(
    task: TaskEntity,
    nowEpochMs: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val category = TaskCategory.safeValueOf(task.category)
    val priority = TaskPriority.safeValueOf(task.priority)
    val isOverdue = !task.isCompleted && task.dueDateEpochMs < nowEpochMs
    val isExam = category == TaskCategory.EXAMEN
    val fmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.FRANCE) }

    val priorityColor = when (priority) {
        TaskPriority.HAUTE -> MaterialTheme.colorScheme.error
        TaskPriority.MOYENNE -> YfuaTheme.colors.warning
        TaskPriority.BASSE -> YfuaTheme.colors.success
    }

    val badgeContainer = if (isExam) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val badgeContent = if (isExam) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isOverdue) MaterialTheme.colorScheme.error else YfuaTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp).testTag("task_toggle_${task.localId}")
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (task.isCompleted) "Terminé" else "En cours",
                    tint = if (task.isCompleted) YfuaTheme.colors.success else MaterialTheme.colorScheme.secondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = badgeContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeContent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Box(modifier = Modifier.size(8.dp).background(priorityColor, RoundedCornerShape(4.dp)))
                    Text(priority.label, style = MaterialTheme.typography.labelSmall, color = priorityColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (task.isCompleted) YfuaTheme.colors.bodySlate else YfuaTheme.colors.headline,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${task.subject} • ${fmt.format(Date(task.dueDateEpochMs))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else YfuaTheme.colors.bodySlate
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Modifier la tâche", tint = YfuaTheme.colors.bodySlate)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Supprimer la tâche", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    initial: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, TaskCategory, Long, TaskPriority, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var category by remember { mutableStateOf(TaskCategory.safeValueOf(initial?.category)) }
    var priority by remember { mutableStateOf(TaskPriority.safeValueOf(initial?.priority)) }
    var daysOffset by remember { mutableIntStateOf(1) }
    var scheduleNotif by remember { mutableStateOf(category == TaskCategory.EXAMEN) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
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
        title = { Text(if (initial == null) "Nouvelle tâche" else "Modifier la tâche") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Titre *") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; errorMsg = null },
                    label = { Text("Matière *") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("task_subject_input")
                )

                Text("Catégorie", style = MaterialTheme.typography.labelMedium, color = YfuaTheme.colors.bodySlate)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (cat == TaskCategory.EXAMEN) scheduleNotif = true
                            },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Text("Priorité", style = MaterialTheme.typography.labelMedium, color = YfuaTheme.colors.bodySlate)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { pri ->
                        FilterChip(selected = priority == pri, onClick = { priority = pri }, label = { Text(pri.label) })
                    }
                }

                if (initial == null) {
                    Text("Échéance", style = MaterialTheme.typography.labelMedium, color = YfuaTheme.colors.bodySlate)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Aujourd'hui", 1 to "Demain", 3 to "+3 jours", 7 to "+7 jours").forEach { (days, label) ->
                            FilterChip(selected = daysOffset == days, onClick = { daysOffset = days }, label = { Text(label) })
                        }
                    }
                }

                if (category == TaskCategory.EXAMEN) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = scheduleNotif, onCheckedChange = { scheduleNotif = it })
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rappel alarme 1h avant", style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.headline)
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || subject.isBlank()) {
                        errorMsg = "Le titre et la matière sont obligatoires."
                    } else {
                        val dueMs = initial?.dueDateEpochMs
                            ?: (System.currentTimeMillis() + daysOffset * 86_400_000L + 3_600_000L)
                        onSave(title.trim(), subject.trim(), category, dueMs, priority, scheduleNotif)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("task_save_button")
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = YfuaTheme.colors.bodySlate) }
        }
    )
}
