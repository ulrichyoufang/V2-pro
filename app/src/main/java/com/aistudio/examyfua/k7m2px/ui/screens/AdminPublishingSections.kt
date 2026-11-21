package com.aistudio.examyfua.k7m2px.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.data.local.OfficialScheduleEntity
import com.aistudio.examyfua.k7m2px.data.remote.RemoteAnnouncement
import com.aistudio.examyfua.k7m2px.data.remote.RemotePushBroadcast
import com.aistudio.examyfua.k7m2px.data.remote.RemoteSupportTicket
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.AnnouncementPriority
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.domain.model.TicketStatus
import com.aistudio.examyfua.k7m2px.ui.components.EmptyStateCard
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.aistudio.examyfua.k7m2px.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = YfuaTheme.colors.headline,
    unfocusedTextColor = YfuaTheme.colors.headline,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = YfuaTheme.colors.cardBorder,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDropdownSelector(
    label: String,
    selectedValue: String,
    allLabel: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val fieldColors = adminFieldColors()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = if (selectedValue == "ALL") allLabel else selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = fieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text(allLabel, color = YfuaTheme.colors.headline) },
                onClick = { onSelected("ALL"); expanded = false }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = YfuaTheme.colors.headline) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun AdminSchedulesTab(
    institutions: List<InstitutionEntity>,
    officialSchedules: List<OfficialScheduleEntity>,
    onUpsertOfficialSchedule: (
        id: String?,
        title: String,
        subject: String,
        category: TaskCategory,
        dueDateEpochMs: Long,
        priority: TaskPriority,
        targetInstitution: String,
        targetLevel: String,
        targetSpecialty: String,
        roomLocation: String,
        instructor: String
    ) -> Unit,
    onDeleteOfficialSchedule: (String) -> Unit
) {
    val fieldColors = adminFieldColors()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    val institutionOptions = remember(institutions) {
        institutions.filter { it.isActive }.map { it.name }.distinct()
    }

    var editingId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var roomLocation by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.COURS) }
    var priority by remember { mutableStateOf(TaskPriority.HAUTE) }
    var daysOffset by remember { mutableIntStateOf(1) }
    var slotHour by remember { mutableIntStateOf(8) }
    var targetInstitution by remember { mutableStateOf("ALL") }
    var targetLevel by remember { mutableStateOf("ALL") }
    var targetSpecialty by remember { mutableStateOf("ALL") }

    fun resetForm() {
        editingId = null
        title = ""
        subject = ""
        roomLocation = ""
        instructor = ""
        category = TaskCategory.COURS
        priority = TaskPriority.HAUTE
        daysOffset = 1
        slotHour = 8
        targetInstitution = "ALL"
        targetLevel = "ALL"
        targetSpecialty = "ALL"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "schedule_editor_card") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (editingId == null) "Publier une séance officielle" else "Modifier la séance",
                            style = MaterialTheme.typography.titleMedium,
                            color = YfuaTheme.colors.headline
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Intitulé de la séance") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_schedule_title")
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Matière") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = roomLocation,
                            onValueChange = { roomLocation = it },
                            label = { Text("Salle / Labo") },
                            colors = fieldColors,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = instructor,
                            onValueChange = { instructor = it },
                            label = { Text("Enseignant") },
                            colors = fieldColors,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Aujourd'hui", 1 to "Demain", 2 to "+2 j", 3 to "+3 j", 7 to "+7 j").forEach { (d, lbl) ->
                            FilterChip(
                                selected = daysOffset == d,
                                onClick = { daysOffset = d },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(8, 10, 14, 16).forEach { hr ->
                            FilterChip(
                                selected = slotHour == hr,
                                onClick = { slotHour = hr },
                                label = { Text("${hr}h00") }
                            )
                        }
                    }

                    AdminDropdownSelector(
                        label = "Établissement cible",
                        selectedValue = targetInstitution,
                        allLabel = "Tous les établissements",
                        options = institutionOptions,
                        onSelected = { targetInstitution = it }
                    )

                    AdminDropdownSelector(
                        label = "Niveau cible",
                        selectedValue = targetLevel,
                        allLabel = "Tous les niveaux",
                        options = AcademicLevel.entries.map { it.displayName },
                        onSelected = { targetLevel = it }
                    )

                    AdminDropdownSelector(
                        label = "Filière cible",
                        selectedValue = targetSpecialty,
                        allLabel = "Toutes les filières",
                        options = AcademicSpecialty.entries.map { it.displayName },
                        onSelected = { targetSpecialty = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (editingId != null) {
                            OutlinedButton(
                                onClick = { resetForm() },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text("Annuler") }
                        }
                        Button(
                            onClick = {
                                if (title.isNotBlank() && subject.isNotBlank()) {
                                    val cal = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, daysOffset)
                                        set(Calendar.HOUR_OF_DAY, slotHour)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    onUpsertOfficialSchedule(
                                        editingId,
                                        title.trim(),
                                        subject.trim(),
                                        category,
                                        cal.timeInMillis,
                                        priority,
                                        targetInstitution,
                                        targetLevel,
                                        targetSpecialty,
                                        roomLocation.trim(),
                                        instructor.trim()
                                    )
                                    resetForm()
                                }
                            },
                            enabled = title.isNotBlank() && subject.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("admin_publish_schedule_button")
                        ) {
                            Text(if (editingId == null) "Publier" else "Mettre à jour", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item(key = "schedules_header") {
            Text("Séances publiées (${officialSchedules.size})", style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
        }

        items(officialSchedules, key = { it.id }) { sched ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${sched.title} • ${sched.subject}", style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline, fontWeight = FontWeight.Bold)
                        Text(
                            "${TaskCategory.safeValueOf(sched.category).label} • ${dateFormat.format(Date(sched.dueDateEpochMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (sched.roomLocation.isNotBlank() || sched.instructor.isNotBlank()) {
                            Text(
                                listOfNotNull(
                                    sched.roomLocation.takeIf { it.isNotBlank() },
                                    sched.instructor.takeIf { it.isNotBlank() }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = YfuaTheme.colors.bodySlate
                            )
                        }
                        Text(
                            "${if (sched.targetInstitution == "ALL") "Tous instituts" else sched.targetInstitution} • ${if (sched.targetSpecialty == "ALL") "Toutes filières" else sched.targetSpecialty}",
                            style = MaterialTheme.typography.labelSmall,
                            color = YfuaTheme.colors.bodySlate
                        )
                    }
                    IconButton(
                        onClick = {
                            editingId = sched.id
                            title = sched.title
                            subject = sched.subject
                            roomLocation = sched.roomLocation
                            instructor = sched.instructor
                            category = TaskCategory.safeValueOf(sched.category)
                            priority = TaskPriority.safeValueOf(sched.priority)
                            targetInstitution = sched.targetInstitution
                            targetLevel = sched.targetLevel
                            targetSpecialty = sched.targetSpecialty
                        }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteOfficialSchedule(sched.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAnnouncementsAndPushTab(
    institutions: List<InstitutionEntity>,
    announcementsState: UiState<List<RemoteAnnouncement>>,
    pushBroadcastsState: UiState<List<RemotePushBroadcast>>,
    onPublishAnnouncement: (String, String, AnnouncementPriority, String, String, String, Boolean) -> Unit,
    onUpdateAnnouncement: (String, String, String, AnnouncementPriority, Boolean, Boolean) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onSendPushBroadcast: (title: String, body: String, topicType: String, topicValue: String) -> Unit
) {
    val fieldColors = adminFieldColors()
    val institutionOptions = remember(institutions) {
        institutions.filter { it.isActive }.map { it.name }.distinct()
    }

    var annTitle by remember { mutableStateOf("") }
    var annBody by remember { mutableStateOf("") }
    var annPriority by remember { mutableStateOf(AnnouncementPriority.INFORMATION) }
    var annInstitution by remember { mutableStateOf("ALL") }
    var annLevel by remember { mutableStateOf("ALL") }
    var annSpecialty by remember { mutableStateOf("ALL") }
    var annPinned by remember { mutableStateOf(true) }

    var pushTitle by remember { mutableStateOf("") }
    var pushBody by remember { mutableStateOf("") }
    var topicType by remember { mutableStateOf("GLOBAL") }
    var topicValue by remember { mutableStateOf("ALL") }

    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "announcement_composer") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publier un communiqué officiel", style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                    }

                    OutlinedTextField(
                        value = annTitle,
                        onValueChange = { annTitle = it },
                        label = { Text("Titre") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_announcement_title")
                    )
                    OutlinedTextField(
                        value = annBody,
                        onValueChange = { annBody = it },
                        label = { Text("Contenu du communiqué") },
                        colors = fieldColors,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("admin_announcement_body")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnnouncementPriority.entries.forEach { p ->
                            FilterChip(
                                selected = annPriority == p,
                                onClick = { annPriority = p },
                                label = { Text(p.label) }
                            )
                        }
                    }

                    AdminDropdownSelector("Établissement", annInstitution, "Tous", institutionOptions) { annInstitution = it }
                    AdminDropdownSelector("Niveau", annLevel, "Tous", AcademicLevel.entries.map { it.displayName }) { annLevel = it }
                    AdminDropdownSelector("Filière", annSpecialty, "Toutes", AcademicSpecialty.entries.map { it.displayName }) { annSpecialty = it }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Épingler à l'accueil", style = MaterialTheme.typography.bodyMedium, color = YfuaTheme.colors.headline)
                        Switch(
                            checked = annPinned,
                            onCheckedChange = { annPinned = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary)
                        )
                    }

                    Button(
                        onClick = {
                            if (annTitle.isNotBlank() && annBody.isNotBlank()) {
                                onPublishAnnouncement(
                                    annTitle.trim(),
                                    annBody.trim(),
                                    annPriority,
                                    annInstitution,
                                    annLevel,
                                    annSpecialty,
                                    annPinned
                                )
                                annTitle = ""
                                annBody = ""
                            }
                        },
                        enabled = annTitle.isNotBlank() && annBody.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_publish_announcement_button")
                    ) {
                        Text("Publier le communiqué", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        when (announcementsState) {
            is UiState.Loading -> item(key = "ann_loading") { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            is UiState.Error -> item(key = "ann_error") { Text(announcementsState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                items(announcementsState.data, key = { "ann_${it.id}" }) { ann ->
                    val prio = AnnouncementPriority.safeValueOf(ann.priority)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    ann.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = YfuaTheme.colors.headline,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (ann.archived) "Archivé" else prio.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ann.archived) YfuaTheme.colors.bodySlate else MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(ann.body, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                IconButton(
                                    onClick = {
                                        onUpdateAnnouncement(ann.id, ann.title, ann.body, prio, !ann.pinned, ann.archived)
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        contentDescription = if (ann.pinned) "Désépingler" else "Épingler",
                                        tint = if (ann.pinned) MaterialTheme.colorScheme.primary else YfuaTheme.colors.bodySlate
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onUpdateAnnouncement(ann.id, ann.title, ann.body, prio, ann.pinned, !ann.archived)
                                    }
                                ) {
                                    Icon(
                                        if (ann.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                        contentDescription = if (ann.archived) "Restaurer" else "Archiver",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(onClick = { onDeleteAnnouncement(ann.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "push_composer") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Centre de Notifications Push (FCM)", style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("GLOBAL" to "Global", "INSTITUT" to "Institut", "NIVEAU" to "Niveau", "SPECIALITE" to "Filière").forEach { (key, label) ->
                            FilterChip(
                                selected = topicType == key,
                                onClick = {
                                    topicType = key
                                    topicValue = when (key) {
                                        "GLOBAL" -> "ALL"
                                        "INSTITUT" -> institutionOptions.firstOrNull() ?: "ALL"
                                        "NIVEAU" -> AcademicLevel.BTS_1.displayName
                                        else -> AcademicSpecialty.MECATRONIQUE.displayName
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    when (topicType) {
                        "INSTITUT" -> AdminDropdownSelector("Institut", topicValue, "Tous", institutionOptions) { topicValue = it }
                        "NIVEAU" -> AdminDropdownSelector("Niveau", topicValue, "Tous", AcademicLevel.entries.map { it.displayName }) { topicValue = it }
                        "SPECIALITE" -> AdminDropdownSelector("Filière", topicValue, "Toutes", AcademicSpecialty.entries.map { it.displayName }) { topicValue = it }
                    }

                    OutlinedTextField(
                        value = pushTitle,
                        onValueChange = { pushTitle = it },
                        label = { Text("Titre de la notification") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pushBody,
                        onValueChange = { pushBody = it },
                        label = { Text("Message") },
                        colors = fieldColors,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (pushTitle.isNotBlank() && pushBody.isNotBlank()) {
                                onSendPushBroadcast(pushTitle.trim(), pushBody.trim(), topicType, topicValue)
                                pushTitle = ""
                                pushBody = ""
                            }
                        },
                        enabled = pushTitle.isNotBlank() && pushBody.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Diffuser la notification", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (pushBroadcastsState is UiState.Success && pushBroadcastsState.data.isNotEmpty()) {
            item(key = "push_history_header") {
                Text("Diffusions récentes", style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline)
            }
            items(pushBroadcastsState.data.take(10), key = { "push_${it.id}" }) { b ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(b.title, style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline, fontWeight = FontWeight.SemiBold)
                        Text(b.body, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                        Text(
                            "${b.topicType} (${b.topicValue}) • ${dateFormat.format(Date(b.sentAtEpochMs))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSupportTicketsTab(
    ticketsState: UiState<List<RemoteSupportTicket>>,
    onReplyTicket: (ticketId: String, reply: String, status: TicketStatus) -> Unit
) {
    var statusFilter by remember { mutableStateOf<TicketStatus?>(null) }
    var replyingTicket by remember { mutableStateOf<RemoteSupportTicket?>(null) }
    var replyText by remember { mutableStateOf("") }
    var replyStatus by remember { mutableStateOf(TicketStatus.REPONDU) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { statusFilter = null },
                label = { Text("Tous") }
            )
            TicketStatus.entries.forEach { st ->
                FilterChip(
                    selected = statusFilter == st,
                    onClick = { statusFilter = st },
                    label = { Text(st.label) }
                )
            }
        }

        when (ticketsState) {
            is UiState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is UiState.Error -> Text(
                ticketsState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )

            is UiState.Success -> {
                val filtered by remember(ticketsState.data, statusFilter) {
                    derivedStateOf {
                        ticketsState.data.filter { t ->
                            statusFilter == null || TicketStatus.safeValueOf(t.status) == statusFilter
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.SupportAgent,
                            title = "Aucun ticket",
                            description = "Aucune demande étudiante ne correspond au filtre sélectionné."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { ticket ->
                            val st = TicketStatus.safeValueOf(ticket.status)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        Text(
                                            st.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "${ticket.studentName} (${ticket.studentEmail}) • ${ticket.institution}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(ticket.message, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                                    Text(
                                        dateFormat.format(Date(ticket.updatedAtEpochMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = YfuaTheme.colors.bodySlate
                                    )
                                    if (ticket.adminReply.isNotBlank()) {
                                        Text(
                                            "Réponse : ${ticket.adminReply}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            replyingTicket = ticket
                                            replyText = ticket.adminReply
                                            replyStatus = if (st == TicketStatus.RESOLU) TicketStatus.RESOLU else TicketStatus.REPONDU
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Répondre au ticket")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val currentTicket = replyingTicket
    if (currentTicket != null) {
        val fieldColors = adminFieldColors()
        AlertDialog(
            onDismissRequest = { replyingTicket = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Répondre à ${currentTicket.studentName}", color = YfuaTheme.colors.headline) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(currentTicket.subject, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Votre réponse") },
                        colors = fieldColors,
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TicketStatus.REPONDU, TicketStatus.RESOLU).forEach { st ->
                            FilterChip(
                                selected = replyStatus == st,
                                onClick = { replyStatus = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReplyTicket(currentTicket.id, replyText.trim(), replyStatus)
                            replyingTicket = null
                        }
                    }
                ) {
                    Text("Envoyer", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingTicket = null }) {
                    Text("Annuler", color = YfuaTheme.colors.bodySlate)
                }
            }
        )
    }
}
