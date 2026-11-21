package com.aistudio.examyfua.k7m2px.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.data.local.OfficialScheduleEntity
import com.aistudio.examyfua.k7m2px.data.remote.RemoteAnnouncement
import com.aistudio.examyfua.k7m2px.data.remote.RemotePushBroadcast
import com.aistudio.examyfua.k7m2px.data.remote.RemoteSupportTicket
import com.aistudio.examyfua.k7m2px.data.remote.RemoteUserProfile
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.AdminAiSecretsState
import com.aistudio.examyfua.k7m2px.domain.model.AdminDashboardStats
import com.aistudio.examyfua.k7m2px.domain.model.AnnouncementPriority
import com.aistudio.examyfua.k7m2px.domain.model.AppConfigState
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.domain.model.TicketStatus
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.aistudio.examyfua.k7m2px.ui.viewmodel.UiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

private enum class AdminSectionTab(val label: String) {
    KPIS_CONFIG("KPIs & Config"),
    IA_API("IA & API"),
    PLANNING("Emploi du temps"),
    COMMUNICATIONS("Communiqués & Push"),
    INSTITUTS("Instituts"),
    MEMBRES("Membres"),
    SUPPORT("Support")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isSuperAdmin: Boolean,
    stats: AdminDashboardStats,
    appConfig: AppConfigState,
    adminAiSecrets: AdminAiSecretsState,
    usersState: UiState<List<RemoteUserProfile>>,
    institutions: List<InstitutionEntity>,
    officialSchedules: List<OfficialScheduleEntity>,
    announcementsState: UiState<List<RemoteAnnouncement>>,
    pushBroadcastsState: UiState<List<RemotePushBroadcast>>,
    supportTicketsState: UiState<List<RemoteSupportTicket>>,
    onUpdateStudent: (targetUid: String, newXp: Long, suspended: Boolean) -> Unit,
    onDeleteStudent: (targetUid: String) -> Unit,
    onUpdateAppConfig: (AppConfigState) -> Unit,
    onToggleAiEnabled: (Boolean) -> Unit,
    onUpdateAiSecrets: (AdminAiSecretsState) -> Unit,
    onUpsertInstitution: (id: String?, name: String, city: String, active: Boolean) -> Unit,
    onDeleteInstitution: (id: String) -> Unit,
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
    onDeleteOfficialSchedule: (id: String) -> Unit,
    onPublishAnnouncement: (String, String, AnnouncementPriority, String, String, String, Boolean) -> Unit,
    onUpdateAnnouncement: (String, String, String, AnnouncementPriority, Boolean, Boolean) -> Unit,
    onDeleteAnnouncement: (id: String) -> Unit,
    onSendPushBroadcast: (title: String, body: String, topicType: String, topicValue: String) -> Unit,
    onReplySupportTicket: (ticketId: String, reply: String, status: TicketStatus) -> Unit,
    onBack: () -> Unit
) {
    if (!isSuperAdmin) return
    BackHandler { onBack() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTab = AdminSectionTab.entries[selectedTabIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Espace Super-Administrateur", color = YfuaTheme.colors.headline) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                AdminSectionTab.entries.forEachIndexed { idx, tab ->
                    Tab(
                        selected = selectedTabIndex == idx,
                        onClick = { selectedTabIndex = idx },
                        text = { Text(tab.label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                label = "admin_tab_transition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    AdminSectionTab.KPIS_CONFIG -> AdminKpisAndConfigTab(
                        stats = stats,
                        appConfig = appConfig,
                        onUpdatePlatformConfig = { maintenance, message, registration ->
                            onUpdateAppConfig(
                                appConfig.copy(
                                    maintenanceMode = maintenance,
                                    maintenanceMessage = message,
                                    registrationEnabled = registration
                                )
                            )
                        }
                    )

                    AdminSectionTab.IA_API -> AdminAiApiTab(
                        appConfig = appConfig,
                        adminSecrets = adminAiSecrets,
                        onToggleAiEnabled = onToggleAiEnabled,
                        onUpdateAppConfig = onUpdateAppConfig,
                        onUpdateAiSecrets = onUpdateAiSecrets
                    )

                    AdminSectionTab.PLANNING -> AdminSchedulesTab(
                        institutions = institutions,
                        officialSchedules = officialSchedules,
                        onUpsertOfficialSchedule = onUpsertOfficialSchedule,
                        onDeleteOfficialSchedule = onDeleteOfficialSchedule
                    )

                    AdminSectionTab.COMMUNICATIONS -> AdminAnnouncementsAndPushTab(
                        institutions = institutions,
                        announcementsState = announcementsState,
                        pushBroadcastsState = pushBroadcastsState,
                        onPublishAnnouncement = onPublishAnnouncement,
                        onUpdateAnnouncement = onUpdateAnnouncement,
                        onDeleteAnnouncement = onDeleteAnnouncement,
                        onSendPushBroadcast = onSendPushBroadcast
                    )

                    AdminSectionTab.INSTITUTS -> AdminInstitutionsTab(
                        institutions = institutions,
                        onUpsertInstitution = onUpsertInstitution,
                        onDeleteInstitution = onDeleteInstitution
                    )

                    AdminSectionTab.MEMBRES -> AdminMembersTab(
                        usersState = usersState,
                        institutions = institutions,
                        onUpdateStudent = onUpdateStudent,
                        onDeleteStudent = onDeleteStudent
                    )

                    AdminSectionTab.SUPPORT -> AdminSupportTicketsTab(
                        ticketsState = supportTicketsState,
                        onReplyTicket = onReplySupportTicket
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpisAndConfigTab(
    stats: AdminDashboardStats,
    appConfig: AppConfigState,
    onUpdatePlatformConfig: (Boolean, String, Boolean) -> Unit
) {
    val fieldColors = adminFieldColors()
    var maintenanceMode by remember(appConfig.maintenanceMode) { mutableStateOf(appConfig.maintenanceMode) }
    var maintenanceMessage by remember(appConfig.maintenanceMessage) { mutableStateOf(appConfig.maintenanceMessage) }
    var registrationEnabled by remember(appConfig.registrationEnabled) { mutableStateOf(appConfig.registrationEnabled) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "kpis_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiTile("Étudiants", "${stats.totalStudents}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    AdminKpiTile("Heures d'étude", "${stats.totalStudyMinutes / 60}h", YfuaTheme.colors.success, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiTile("XP Global", "${stats.totalXp}", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    AdminKpiTile("Tickets ouverts", "${stats.openTicketsCount}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
        }

        item(key = "breakdown_specialty") {
            val maxCount = (stats.breakdownBySpecialty.values.maxOrNull() ?: 1).coerceAtLeast(1)
            val trackColor = MaterialTheme.colorScheme.primaryContainer
            val barColor = MaterialTheme.colorScheme.primary
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Répartition par filière", style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                    AcademicSpecialty.entries.forEach { spec ->
                        val count = stats.breakdownBySpecialty[spec] ?: 0
                        AdminBarRow(spec.displayName, count, maxCount, trackColor, barColor)
                    }
                }
            }
        }

        if (stats.breakdownByInstitution.isNotEmpty()) {
            item(key = "breakdown_institution") {
                val maxInst = (stats.breakdownByInstitution.values.maxOrNull() ?: 1).coerceAtLeast(1)
                val trackColor = MaterialTheme.colorScheme.secondaryContainer
                val barColor = MaterialTheme.colorScheme.secondary
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Répartition par établissement", style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                        stats.breakdownByInstitution.entries
                            .sortedByDescending { it.value }
                            .forEach { (inst, count) ->
                                AdminBarRow(inst, count, maxInst, trackColor, barColor)
                            }
                    }
                }
            }
        }

        item(key = "platform_config") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Configuration de la plateforme", style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mode maintenance", style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline)
                            Text("Affiche un bandeau aux étudiants", style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                        }
                        Switch(
                            checked = maintenanceMode,
                            onCheckedChange = { maintenanceMode = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.error)
                        )
                    }

                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = { Text("Message de maintenance") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Inscriptions ouvertes", style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline)
                            Text("Autoriser de nouveaux comptes e-mail", style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                        }
                        Switch(
                            checked = registrationEnabled,
                            onCheckedChange = { registrationEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Button(
                        onClick = { onUpdatePlatformConfig(maintenanceMode, maintenanceMessage.trim(), registrationEnabled) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Enregistrer la configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminInstitutionsTab(
    institutions: List<InstitutionEntity>,
    onUpsertInstitution: (id: String?, name: String, city: String, active: Boolean) -> Unit,
    onDeleteInstitution: (String) -> Unit
) {
    val fieldColors = adminFieldColors()
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "inst_form") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (editingId == null) "Ajouter un établissement" else "Modifier l'établissement",
                            style = MaterialTheme.typography.titleMedium,
                            color = YfuaTheme.colors.headline
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom de l'établissement") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_institution_name")
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Ville") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Établissement actif", style = MaterialTheme.typography.bodyMedium, color = YfuaTheme.colors.headline)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (editingId != null) {
                            OutlinedButton(
                                onClick = { editingId = null; name = ""; city = ""; active = true },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text("Annuler") }
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onUpsertInstitution(editingId, name.trim(), city.trim(), active)
                                    editingId = null
                                    name = ""
                                    city = ""
                                    active = true
                                }
                            },
                            enabled = name.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("admin_save_institution_button")
                        ) {
                            Text("Enregistrer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(institutions, key = { it.id }) { inst ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(inst.name, style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline, fontWeight = FontWeight.Bold)
                        Text(
                            "${inst.city.ifBlank { "Cameroun" }} • ${if (inst.isActive) "Actif" else "Inactif"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (inst.isActive) YfuaTheme.colors.success else YfuaTheme.colors.bodySlate
                        )
                    }
                    Switch(
                        checked = inst.isActive,
                        onCheckedChange = { onUpsertInstitution(inst.id, inst.name, inst.city, it) }
                    )
                    IconButton(
                        onClick = {
                            editingId = inst.id
                            name = inst.name
                            city = inst.city
                            active = inst.isActive
                        }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteInstitution(inst.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun AdminMembersTab(
    usersState: UiState<List<RemoteUserProfile>>,
    institutions: List<InstitutionEntity>,
    onUpdateStudent: (String, Long, Boolean) -> Unit,
    onDeleteStudent: (String) -> Unit
) {
    val fieldColors = adminFieldColors()
    var rawSearch by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }
    var selectedInstitution by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        snapshotFlow { rawSearch }
            .debounce(300L)
            .collect { debouncedSearch = it }
    }

    val filteredUsers by remember(usersState, debouncedSearch, selectedInstitution) {
        derivedStateOf {
            if (usersState !is UiState.Success) emptyList()
            else usersState.data.filter { user ->
                val matchesInst = selectedInstitution == "ALL" || user.institution == selectedInstitution
                val matchesSearch = debouncedSearch.isBlank() ||
                    user.fullName.contains(debouncedSearch, ignoreCase = true) ||
                    user.email.contains(debouncedSearch, ignoreCase = true) ||
                    user.specialty.contains(debouncedSearch, ignoreCase = true)
                matchesInst && matchesSearch
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "members_search") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rawSearch,
                    onValueChange = { rawSearch = it },
                    placeholder = { Text("Rechercher par nom, e-mail ou filière…", color = YfuaTheme.colors.bodySlate) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    colors = fieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_search_users_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedInstitution == "ALL",
                        onClick = { selectedInstitution = "ALL" },
                        label = { Text("Tous les instituts") }
                    )
                    institutions.forEach { inst ->
                        FilterChip(
                            selected = selectedInstitution == inst.name,
                            onClick = { selectedInstitution = inst.name },
                            label = { Text(inst.name) }
                        )
                    }
                }
            }
        }

        when (usersState) {
            is UiState.Loading -> item(key = "members_loading") { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            is UiState.Error -> item(key = "members_error") { Text(usersState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                items(filteredUsers, key = { it.userId }) { user ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (user.suspended) MaterialTheme.colorScheme.error else YfuaTheme.colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.fullName, style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                                    val contactLine = listOfNotNull(
                                        user.email.takeIf { it.isNotBlank() },
                                        user.phoneNumber.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ").ifBlank { user.userId }
                                    Text(contactLine, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                                    Text("${user.institution} • ${user.specialty} • ${user.level}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text("${user.xpPoints} XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onUpdateStudent(user.userId, user.xpPoints + 50L, user.suspended) },
                                    border = BorderStroke(1.dp, YfuaTheme.colors.success),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("+50 XP", color = YfuaTheme.colors.success) }
                                OutlinedButton(
                                    onClick = { onUpdateStudent(user.userId, 0L, user.suspended) },
                                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) { Text("Reset XP", color = YfuaTheme.colors.headline) }
                                IconButton(
                                    onClick = { onUpdateStudent(user.userId, user.xpPoints, !user.suspended) }
                                ) {
                                    Icon(
                                        imageVector = if (user.suspended) Icons.Filled.CheckCircle else Icons.Filled.Block,
                                        contentDescription = if (user.suspended) "Réactiver" else "Suspendre",
                                        tint = if (user.suspended) YfuaTheme.colors.success else YfuaTheme.colors.warning
                                    )
                                }
                                IconButton(onClick = { onDeleteStudent(user.userId) }) {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBarRow(
    label: String,
    count: Int,
    maxCount: Int,
    trackColor: Color,
    barColor: Color
) {
    val ratio = count.toFloat() / maxCount.toFloat()
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count", style = MaterialTheme.typography.labelSmall, color = barColor, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(trackColor, size = size, cornerRadius = CornerRadius(4.dp.toPx()))
                if (count > 0) {
                    drawRoundRect(
                        barColor,
                        size = Size(size.width * ratio.coerceIn(0.06f, 1f), size.height),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiTile(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = YfuaTheme.colors.bodySlate)
        }
    }
}
