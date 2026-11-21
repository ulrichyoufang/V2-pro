package com.aistudio.examyfua.k7m2px.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.examyfua.k7m2px.data.remote.GoogleDriveBackupClient
import com.aistudio.examyfua.k7m2px.ui.components.ConnectivityStatusBanner
import com.aistudio.examyfua.k7m2px.ui.components.ProfileSetupBottomSheet
import com.aistudio.examyfua.k7m2px.ui.components.SyncDetailsDialog
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.aistudio.examyfua.k7m2px.ui.viewmodel.AppEvent
import com.aistudio.examyfua.k7m2px.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class YfuaTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ACCUEIL("Accueil", Icons.Filled.Home, Icons.Outlined.Home),
    PLANNING("Planning", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    ASSISTANT_IA("Assistant IA", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    CLASSEMENT("Classement", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    PROFIL("Profil", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun YfuaAppShell(
    viewModel: MainViewModel,
    currentUid: String?,
    currentEmail: String?,
    driveClient: GoogleDriveBackupClient,
    onRequestSwitchToAuth: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(YfuaTab.ACCUEIL) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showSupport by rememberSaveable { mutableStateOf(false) }
    var showAdminDashboard by rememberSaveable { mutableStateOf(false) }

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val planningItems by viewModel.planningItems.collectAsStateWithLifecycle()
    val planningSearchInput by viewModel.planningSearchInput.collectAsStateWithLifecycle()
    val debouncedPlanningQuery by viewModel.debouncedPlanningQuery.collectAsStateWithLifecycle()
    val officialSchedules by viewModel.officialSchedules.collectAsStateWithLifecycle()
    val activeInstitutions by viewModel.activeInstitutions.collectAsStateWithLifecycle()
    val allInstitutions by viewModel.allInstitutions.collectAsStateWithLifecycle()
    val cachedAnnouncements by viewModel.cachedAnnouncements.collectAsStateWithLifecycle()
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcards.collectAsStateWithLifecycle()
    val pomodoroSessions by viewModel.pomodoroSessions.collectAsStateWithLifecycle()
    val subjectStats by viewModel.subjectStats.collectAsStateWithLifecycle()
    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val pendingDirtyCount by viewModel.pendingDirtyCount.collectAsStateWithLifecycle()
    val recentSyncLogs by viewModel.recentSyncLogs.collectAsStateWithLifecycle()
    val lastSyncEpochMs by viewModel.lastSyncEpochMs.collectAsStateWithLifecycle()
    val lastDriveBackupEpochMs by viewModel.lastDriveBackupEpochMs.collectAsStateWithLifecycle()
    val isDriveBusy by viewModel.isDriveBusy.collectAsStateWithLifecycle()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsStateWithLifecycle()
    val dailyReminderTime by viewModel.dailyReminderTime.collectAsStateWithLifecycle()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()

    val appConfigState by viewModel.appConfigState.collectAsStateWithLifecycle()
    val adminAiSecretsState by viewModel.adminAiSecretsState.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiGenerationState by viewModel.aiGenerationState.collectAsStateWithLifecycle()
    val aiDailyRequestsUsed by viewModel.aiDailyRequestsUsed.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val receivedNudgesState by viewModel.receivedNudgesState.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val adminSupportTicketsState by viewModel.adminSupportTicketsState.collectAsStateWithLifecycle()
    val adminPushBroadcastsState by viewModel.adminPushBroadcastsState.collectAsStateWithLifecycle()
    val adminAnnouncementsState by viewModel.adminAnnouncementsState.collectAsStateWithLifecycle()
    val adminStats by viewModel.adminStats.collectAsStateWithLifecycle()

    var initialSetupDismissed by rememberSaveable { mutableStateOf(false) }
    val needsMandatorySetup = profile == null || (!profile!!.isConfigured && !initialSetupDismissed)

    LaunchedEffect(soundEnabled, hapticsEnabled) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }

                is AppEvent.TaskDeletedWithUndo -> {
                    val res = snackbarHostState.showSnackbar(
                        message = "Tâche « ${event.task.title} » supprimée.",
                        actionLabel = "Annuler",
                        duration = SnackbarDuration.Long
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteTask(event.task)
                    }
                }

                is AppEvent.DeckDeletedWithUndo -> {
                    val res = snackbarHostState.showSnackbar(
                        message = "Paquet « ${event.deck.title} » supprimé.",
                        actionLabel = "Annuler",
                        duration = SnackbarDuration.Long
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteDeck(event.deck)
                    }
                }

                is AppEvent.PomodoroCycleCompleted -> {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    triggerCycleCompletionFeedback(context, soundEnabled, hapticsEnabled)
                    snackbarHostState.showSnackbar(
                        message = "Cycle Pomodoro terminé ! +25 XP gagnés.",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    if (showAdminDashboard && viewModel.isSuperAdmin) {
        AdminDashboardScreen(
            isSuperAdmin = viewModel.isSuperAdmin,
            stats = adminStats,
            appConfig = appConfigState,
            adminAiSecrets = adminAiSecretsState,
            usersState = adminUsersState,
            institutions = allInstitutions,
            officialSchedules = officialSchedules,
            announcementsState = adminAnnouncementsState,
            pushBroadcastsState = adminPushBroadcastsState,
            supportTicketsState = adminSupportTicketsState,
            onUpdateStudent = viewModel::adminUpdateStudent,
            onDeleteStudent = viewModel::adminDeleteStudent,
            onUpdateAppConfig = viewModel::adminUpdateAppConfig,
            onToggleAiEnabled = viewModel::adminToggleAiEnabled,
            onUpdateAiSecrets = viewModel::adminUpdateAiSecrets,
            onUpsertInstitution = viewModel::adminUpsertInstitution,
            onDeleteInstitution = viewModel::adminDeleteInstitution,
            onUpsertOfficialSchedule = viewModel::adminUpsertOfficialSchedule,
            onDeleteOfficialSchedule = viewModel::adminDeleteOfficialSchedule,
            onPublishAnnouncement = viewModel::adminPublishAnnouncement,
            onUpdateAnnouncement = viewModel::adminUpdateAnnouncement,
            onDeleteAnnouncement = viewModel::adminDeleteAnnouncement,
            onSendPushBroadcast = viewModel::adminSendPushBroadcast,
            onReplySupportTicket = viewModel::adminReplySupportTicket,
            onBack = { showAdminDashboard = false }
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            darkModeEnabled = darkModeEnabled,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled,
            dailyReminderEnabled = dailyReminderEnabled,
            dailyReminderTime = dailyReminderTime,
            onToggleDarkMode = viewModel::setDarkMode,
            onToggleSound = viewModel::setSoundEnabled,
            onToggleHaptics = viewModel::setHapticsEnabled,
            onToggleDailyReminder = viewModel::setDailyReminder,
            onExportJson = viewModel::getExportJson,
            onExportCsv = viewModel::getExportCsv,
            onClearCache = viewModel::clearLocalCache,
            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            onBack = { showSettings = false }
        )
        return
    }

    if (showSupport) {
        SupportTicketsScreen(
            currentUid = currentUid,
            tickets = supportTickets,
            onSubmitTicket = viewModel::submitSupportTicket,
            onMarkResolved = viewModel::markTicketResolved,
            onRequestSignIn = {
                showSupport = false
                onRequestSwitchToAuth()
            },
            onBack = { showSupport = false }
        )
        return
    }

    if (currentTab != YfuaTab.ACCUEIL) {
        BackHandler { currentTab = YfuaTab.ACCUEIL }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    Column {
                        HorizontalDivider(color = YfuaTheme.colors.cardBorder, thickness = 1.dp)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = YfuaTheme.colors.headline
                        ) {
                            YfuaTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = YfuaTheme.colors.bodySlate,
                                        unselectedTextColor = YfuaTheme.colors.bodySlate
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = YfuaTheme.colors.headline,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        YfuaTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = YfuaTheme.colors.bodySlate,
                                    unselectedTextColor = YfuaTheme.colors.bodySlate
                                )
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    ConnectivityStatusBanner(
                        isOnline = isOnline,
                        isAuthenticated = currentUid != null,
                        syncState = syncProgress,
                        pendingCount = pendingDirtyCount,
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (appConfigState.maintenanceMode && !viewModel.isSuperAdmin) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Build, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appConfigState.maintenanceMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            YfuaTab.ACCUEIL -> HomeScreen(
                                profile = profile,
                                tasks = tasks,
                                flashcards = flashcards,
                                pomodoroSessions = pomodoroSessions,
                                subjectStats = subjectStats,
                                announcements = cachedAnnouncements,
                                isRefreshing = isRefreshing,
                                onRefresh = viewModel::refreshAll,
                                onNavigateToPlanning = { currentTab = YfuaTab.PLANNING },
                                onNavigateToAi = { currentTab = YfuaTab.ASSISTANT_IA },
                                onShareSummary = viewModel::getShareSummary
                            )

                            YfuaTab.PLANNING -> PlanningScreen(
                                planningItems = planningItems,
                                tasks = tasks,
                                searchQuery = planningSearchInput,
                                debouncedQuery = debouncedPlanningQuery,
                                isRefreshing = isRefreshing,
                                hapticsEnabled = hapticsEnabled,
                                onSearchQueryChange = viewModel::setPlanningSearchQuery,
                                onRefresh = viewModel::refreshAll,
                                onSaveTask = viewModel::saveTask,
                                onToggleComplete = viewModel::toggleTaskCompleted,
                                onDeleteTask = viewModel::deleteTask
                            )

                            YfuaTab.ASSISTANT_IA -> AiAssistantScreen(
                                profile = profile,
                                appConfig = appConfigState,
                                isOnline = isOnline,
                                isSuperAdmin = viewModel.isSuperAdmin,
                                messages = aiMessages,
                                generationState = aiGenerationState,
                                dailyRequestsUsed = aiDailyRequestsUsed,
                                onSendPrompt = viewModel::sendAiPrompt,
                                onCancelGeneration = viewModel::cancelAiGeneration,
                                onClearConversation = viewModel::clearAiConversation,
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )

                            YfuaTab.CLASSEMENT -> LeaderboardScreen(
                                currentUid = currentUid,
                                profile = profile,
                                leaderboardState = leaderboardState,
                                receivedNudgesState = receivedNudgesState,
                                isRefreshing = isRefreshing,
                                onRefresh = viewModel::refreshAll,
                                onSendPeerNudge = viewModel::sendPeerNudge,
                                onMarkNudgeRead = viewModel::markNudgeRead,
                                onRequestSignIn = onRequestSwitchToAuth
                            )

                            YfuaTab.PROFIL -> ProfileScreen(
                                profile = profile,
                                currentUid = currentUid,
                                currentEmail = currentEmail,
                                isOnline = isOnline,
                                pendingDirtyCount = pendingDirtyCount,
                                lastSyncEpochMs = lastSyncEpochMs,
                                lastDriveBackupEpochMs = lastDriveBackupEpochMs,
                                isDriveBusy = isDriveBusy,
                                isSuperAdmin = viewModel.isSuperAdmin,
                                driveClient = driveClient,
                                onEditProfile = { showProfileEditor = true },
                                onUpdateProfilePhoto = viewModel::updateProfilePhoto,
                                onTriggerSync = viewModel::triggerManualSync,
                                onOpenSyncLogs = { showSyncDialog = true },
                                onUploadDriveBackup = viewModel::uploadDriveBackup,
                                onRestoreDriveBackup = viewModel::restoreDriveBackup,
                                onOpenSettings = { showSettings = true },
                                onOpenSupport = { showSupport = true },
                                onAccountLinked = { viewModel.triggerManualSync() },
                                onSignedOut = onSignedOut,
                                onOpenAdminPanel = { showAdminDashboard = true },
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfileEditor || needsMandatorySetup) {
        ProfileSetupBottomSheet(
            initialProfile = profile,
            institutions = activeInstitutions,
            isMandatory = needsMandatorySetup && profile?.isConfigured != true,
            onSave = { name, phone, photoUri, institution, specialty, level ->
                viewModel.saveProfile(name, phone, photoUri, institution, specialty, level)
                initialSetupDismissed = true
                showProfileEditor = false
            },
            onDismiss = {
                initialSetupDismissed = true
                showProfileEditor = false
            }
        )
    }

    if (showSyncDialog) {
        SyncDetailsDialog(
            isOnline = isOnline,
            isAuthenticated = currentUid != null,
            pendingCount = pendingDirtyCount,
            lastSyncEpochMs = lastSyncEpochMs,
            syncLogs = recentSyncLogs,
            onSyncNow = viewModel::triggerManualSync,
            onDismiss = { showSyncDialog = false }
        )
    }
}

private fun triggerCycleCompletionFeedback(
    context: Context,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean
) {
    if (soundEnabled) {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).apply {
                startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            }
        } catch (_: Exception) {
        }
    }
    if (hapticsEnabled) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(250L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(250L)
            }
        } catch (_: Exception) {
        }
    }
}
