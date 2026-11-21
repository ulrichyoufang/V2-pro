package com.aistudio.examyfua.k7m2px.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.examyfua.k7m2px.data.local.AnnouncementCacheEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.data.local.LocalPreferences
import com.aistudio.examyfua.k7m2px.data.local.OfficialScheduleEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.data.local.SupportTicketEntity
import com.aistudio.examyfua.k7m2px.data.local.SyncLogEntity
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.data.remote.AiAssistantRepository
import com.aistudio.examyfua.k7m2px.data.remote.FirestoreService
import com.aistudio.examyfua.k7m2px.data.remote.GoogleDriveBackupClient
import com.aistudio.examyfua.k7m2px.data.remote.RemoteAnnouncement
import com.aistudio.examyfua.k7m2px.data.remote.RemoteLeaderboardEntry
import com.aistudio.examyfua.k7m2px.data.remote.RemotePeerNudge
import com.aistudio.examyfua.k7m2px.data.remote.RemotePushBroadcast
import com.aistudio.examyfua.k7m2px.data.remote.RemoteSupportTicket
import com.aistudio.examyfua.k7m2px.data.remote.RemoteUserProfile
import com.aistudio.examyfua.k7m2px.data.repository.BackupJsonSerializer
import com.aistudio.examyfua.k7m2px.data.repository.NetworkMonitor
import com.aistudio.examyfua.k7m2px.data.repository.SyncManager
import com.aistudio.examyfua.k7m2px.data.repository.YfuaRepository
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.AdminAiSecretsState
import com.aistudio.examyfua.k7m2px.domain.model.AdminDashboardStats
import com.aistudio.examyfua.k7m2px.domain.model.AiChatMessage
import com.aistudio.examyfua.k7m2px.domain.model.AiGenerationState
import com.aistudio.examyfua.k7m2px.domain.model.AiProviderMode
import com.aistudio.examyfua.k7m2px.domain.model.AnnouncementPriority
import com.aistudio.examyfua.k7m2px.domain.model.AppConfigState
import com.aistudio.examyfua.k7m2px.domain.model.PlanningItem
import com.aistudio.examyfua.k7m2px.domain.model.PomodoroMode
import com.aistudio.examyfua.k7m2px.domain.model.SUPER_ADMIN_EMAIL
import com.aistudio.examyfua.k7m2px.domain.model.Sm2Quality
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.domain.model.SubjectStudyStat
import com.aistudio.examyfua.k7m2px.domain.model.SyncProgressState
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.domain.model.TicketStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Immutable
data class PomodoroTimerState(
    val mode: PomodoroMode = PomodoroMode.CLASSIQUE,
    val subject: String = "Mécatronique",
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val isBreakPhase: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalPhaseSeconds: Int = 25 * 60,
    val isRunning: Boolean = false
)

sealed interface AppEvent {
    data class ShowMessage(val message: String) : AppEvent
    data class TaskDeletedWithUndo(val task: TaskEntity) : AppEvent
    data class DeckDeletedWithUndo(val deck: FlashcardDeckEntity) : AppEvent
    data object PomodoroCycleCompleted : AppEvent
}

@OptIn(FlowPreview::class)
class MainViewModel(
    private val repository: YfuaRepository,
    private val firestoreService: FirestoreService,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
    private val preferences: LocalPreferences,
    private val driveClient: GoogleDriveBackupClient,
    private val aiAssistantRepository: AiAssistantRepository = AiAssistantRepository(),
    private val currentUid: String?,
    private val currentEmail: String?,
    private val currentDisplayName: String?,
    private val currentEmailVerified: Boolean
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val syncProgress: StateFlow<SyncProgressState> = syncManager.syncState
    val pendingDirtyCount: StateFlow<Int> = syncManager.pendingDirtyCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0)
    val recentSyncLogs: StateFlow<List<SyncLogEntity>> = syncManager.recentSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val profile: StateFlow<StudentProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val officialSchedules: StateFlow<List<OfficialScheduleEntity>> = repository.officialSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val activeInstitutions: StateFlow<List<InstitutionEntity>> = repository.activeInstitutions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val allInstitutions: StateFlow<List<InstitutionEntity>> = repository.allInstitutions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val cachedAnnouncements: StateFlow<List<AnnouncementCacheEntity>> = repository.cachedAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val decks: StateFlow<List<FlashcardDeckEntity>> = repository.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val flashcards: StateFlow<List<FlashcardEntity>> = repository.flashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val pomodoroSessions: StateFlow<List<PomodoroSessionEntity>> = repository.pomodoroSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val supportTickets: StateFlow<List<SupportTicketEntity>> =
        if (currentUid == null) {
            MutableStateFlow(emptyList())
        } else {
            repository.observeSupportTickets(currentUid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())
        }

    val planningItems: StateFlow<List<PlanningItem>> = combine(
        tasks,
        officialSchedules,
        profile
    ) { localTasks, officialList, student ->
        val personalItems = localTasks.map { t ->
            PlanningItem(
                id = t.localId,
                title = t.title,
                subject = t.subject,
                category = TaskCategory.safeValueOf(t.category),
                dueDateEpochMs = t.dueDateEpochMs,
                priority = TaskPriority.safeValueOf(t.priority),
                isCompleted = t.isCompleted,
                isOfficial = false
            )
        }
        val filteredOfficial = officialList.filter { s ->
            val instMatch = s.targetInstitution == "ALL" ||
                student?.institution?.equals(s.targetInstitution, ignoreCase = true) == true
            val levelMatch = s.targetLevel == "ALL" ||
                student?.level?.displayName == s.targetLevel
            val specMatch = s.targetSpecialty == "ALL" ||
                student?.specialty?.displayName == s.targetSpecialty
            instMatch && levelMatch && specMatch
        }.map { s ->
            PlanningItem(
                id = "official_${s.id}",
                title = s.title,
                subject = s.subject,
                category = TaskCategory.safeValueOf(s.category),
                dueDateEpochMs = s.dueDateEpochMs,
                priority = TaskPriority.safeValueOf(s.priority),
                isCompleted = false,
                isOfficial = true,
                institution = s.targetInstitution,
                roomLocation = s.roomLocation,
                instructor = s.instructor
            )
        }
        (personalItems + filteredOfficial).sortedWith(
            compareBy<PlanningItem> { it.isCompleted }.thenBy { it.dueDateEpochMs }
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _planningSearchInput = MutableStateFlow("")
    val planningSearchInput: StateFlow<String> = _planningSearchInput.asStateFlow()
    val debouncedPlanningQuery: StateFlow<String> = _planningSearchInput
        .debounce(300L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), "")

    val subjectStats: StateFlow<List<SubjectStudyStat>> = pomodoroSessions.map { list ->
        list.groupBy { it.subject }
            .map { (subject, items) ->
                SubjectStudyStat(
                    subject = subject,
                    totalMinutes = items.sumOf { it.focusMinutes },
                    sessionCount = items.size
                )
            }
            .sortedByDescending { it.totalMinutes }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val lastSyncEpochMs: StateFlow<Long> = preferences.lastSyncEpochMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0L)
    val lastDriveBackupEpochMs: StateFlow<Long> = preferences.lastDriveBackupEpochMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0L)
    val dailyReminderEnabled: StateFlow<Boolean> = preferences.dailyReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)
    val dailyReminderTime: StateFlow<Pair<Int, Int>> = preferences.dailyReminderTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 19 to 0)
    val darkModeEnabled: StateFlow<Boolean> = preferences.darkModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)
    val soundEnabled: StateFlow<Boolean> = preferences.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)
    val hapticsEnabled: StateFlow<Boolean> = preferences.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    val isSuperAdmin: Boolean =
        currentUid != null && currentEmail == SUPER_ADMIN_EMAIL && currentEmailVerified

    val appConfigState: StateFlow<AppConfigState> =
        if (currentUid == null) {
            MutableStateFlow(AppConfigState())
        } else {
            firestoreService.observeAppConfig()
                .catch { emit(AppConfigState()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AppConfigState())
        }

    val adminAiSecretsState: StateFlow<AdminAiSecretsState> =
        if (!isSuperAdmin) {
            MutableStateFlow(AdminAiSecretsState())
        } else {
            firestoreService.adminObserveAiSecrets()
                .catch { emit(AdminAiSecretsState()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AdminAiSecretsState())
        }

    val leaderboardState: StateFlow<UiState<List<RemoteLeaderboardEntry>>> =
        if (currentUid == null) {
            MutableStateFlow(UiState.Error("Mode hors-ligne actif"))
        } else {
            firestoreService.observeLeaderboard()
                .map<List<RemoteLeaderboardEntry>, UiState<List<RemoteLeaderboardEntry>>> { UiState.Success(it) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val root = fsError ?: error
                    Log.w(TAG, "Leaderboard stream error", root)
                    emit(UiState.Error(root.localizedMessage ?: "Erreur de chargement du classement"))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)
        }

    val receivedNudgesState: StateFlow<UiState<List<RemotePeerNudge>>> =
        if (currentUid == null) {
            MutableStateFlow(UiState.Success(emptyList()))
        } else {
            firestoreService.observeReceivedPeerNudges(currentUid)
                .map<List<RemotePeerNudge>, UiState<List<RemotePeerNudge>>> { UiState.Success(it) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    emit(UiState.Error(fsError?.localizedMessage ?: error.localizedMessage ?: "Erreur entraide"))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)
        }

    val adminUsersState: StateFlow<UiState<List<RemoteUserProfile>>> =
        if (!isSuperAdmin) {
            MutableStateFlow(UiState.Success(emptyList()))
        } else {
            firestoreService.observeAllUsersForAdmin()
                .map<List<RemoteUserProfile>, UiState<List<RemoteUserProfile>>> { UiState.Success(it) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    emit(UiState.Error(fsError?.localizedMessage ?: error.localizedMessage ?: "Erreur admin"))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)
        }

    val adminSupportTicketsState: StateFlow<UiState<List<RemoteSupportTicket>>> =
        if (!isSuperAdmin) {
            MutableStateFlow(UiState.Success(emptyList()))
        } else {
            firestoreService.adminObserveAllSupportTickets()
                .map<List<RemoteSupportTicket>, UiState<List<RemoteSupportTicket>>> { UiState.Success(it) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    emit(UiState.Error(fsError?.localizedMessage ?: error.localizedMessage ?: "Erreur tickets"))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)
        }

    val adminPushBroadcastsState: StateFlow<UiState<List<RemotePushBroadcast>>> =
        if (!isSuperAdmin) {
            MutableStateFlow(UiState.Success(emptyList()))
        } else {
            firestoreService.observePushBroadcasts()
                .map<List<RemotePushBroadcast>, UiState<List<RemotePushBroadcast>>> { UiState.Success(it) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    emit(UiState.Error(fsError?.localizedMessage ?: error.localizedMessage ?: "Erreur notifications"))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)
        }

    val adminAnnouncementsState: StateFlow<UiState<List<RemoteAnnouncement>>> =
        if (!isSuperAdmin) {
            MutableStateFlow(UiState.Success(emptyList()))
        } else {
            firestoreService.observeAnnouncements()
                .map<List<RemoteAnnouncement>, UiState<List<RemoteAnnouncement>>> { UiState.Success(it) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    emit(UiState.Error(fsError?.localizedMessage ?: error.localizedMessage ?: "Erreur communiqués"))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)
        }

    val adminStats: StateFlow<AdminDashboardStats> = combine(
        adminUsersState,
        adminSupportTicketsState
    ) { usersState, ticketsState ->
        val openTickets = if (ticketsState is UiState.Success) {
            ticketsState.data.count { it.status != TicketStatus.RESOLU.name }
        } else 0
        if (usersState is UiState.Success) {
            val users = usersState.data
            val bySpecialty = AcademicSpecialty.entries.associateWith { spec ->
                users.count { it.specialty == spec.displayName }
            }
            val byInstitution = users
                .groupBy { it.institution.ifBlank { "Non renseigné" } }
                .mapValues { it.value.size }
            AdminDashboardStats(
                totalStudents = users.size,
                totalStudyMinutes = users.sumOf { it.totalStudyMinutes },
                totalXp = users.sumOf { it.xpPoints },
                openTicketsCount = openTickets,
                breakdownBySpecialty = bySpecialty,
                breakdownByInstitution = byInstitution
            )
        } else {
            AdminDashboardStats(openTicketsCount = openTickets)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AdminDashboardStats())

    private val _pomodoroState = MutableStateFlow(PomodoroTimerState())
    val pomodoroState: StateFlow<PomodoroTimerState> = _pomodoroState.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _aiGenerationState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiGenerationState: StateFlow<AiGenerationState> = _aiGenerationState.asStateFlow()

    private val _aiDailyRequestsUsed = MutableStateFlow(0)
    val aiDailyRequestsUsed: StateFlow<Int> = _aiDailyRequestsUsed.asStateFlow()

    private val _isDriveBusy = MutableStateFlow(false)
    val isDriveBusy: StateFlow<Boolean> = _isDriveBusy.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultInstitution()
            if (currentUid != null) {
                repository.ensureInitialProfileFromAuth(currentUid, currentDisplayName, currentEmail)
                syncManager.startRealtimeDownwardSync(viewModelScope, currentUid) { profile.value }
                syncManager.syncNow("Ouverture de session")
            }
        }
    }

    fun setPlanningSearchQuery(query: String) {
        _planningSearchInput.value = query
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (currentUid != null && isOnline.value) {
                    syncManager.syncNow("Actualisation manuelle")
                } else {
                    delay(350L)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun saveProfile(
        fullName: String,
        phoneNumber: String,
        photoUri: String,
        institution: String,
        specialty: AcademicSpecialty,
        level: AcademicLevel
    ) {
        viewModelScope.launch {
            repository.saveProfile(
                fullName = fullName,
                email = currentEmail.orEmpty(),
                phoneNumber = phoneNumber,
                photoUri = photoUri,
                institution = institution,
                specialty = specialty,
                level = level,
                remoteUid = currentUid
            )
            _events.tryEmit(AppEvent.ShowMessage("Profil enregistré."))
            if (currentUid != null && isOnline.value) {
                syncManager.syncNow("Mise à jour profil")
            }
        }
    }

    fun updateProfilePhoto(sourceUri: Uri?) {
        if (sourceUri == null) return
        viewModelScope.launch {
            try {
                repository.persistProfilePhoto(sourceUri)
                _events.tryEmit(AppEvent.ShowMessage("Photo de profil mise à jour."))
            } catch (e: Exception) {
                _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Impossible d'importer la photo."))
            }
        }
    }

    fun clearProfilePhoto() {
        viewModelScope.launch {
            repository.clearProfilePhoto()
            _events.tryEmit(AppEvent.ShowMessage("Photo de profil retirée."))
        }
    }

    fun saveTask(
        localId: String? = null,
        title: String,
        subject: String,
        category: TaskCategory,
        dueDateEpochMs: Long,
        priority: TaskPriority,
        scheduleNotification: Boolean
    ) {
        viewModelScope.launch {
            repository.saveTask(localId, title, subject, category, dueDateEpochMs, priority, scheduleNotification)
            _events.tryEmit(
                AppEvent.ShowMessage(if (localId == null) "Tâche ajoutée." else "Tâche mise à jour.")
            )
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch { repository.toggleTaskCompleted(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task.localId)
            _events.tryEmit(AppEvent.TaskDeletedWithUndo(task))
        }
    }

    fun undoDeleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.restoreTask(task)
            _events.tryEmit(AppEvent.ShowMessage("Tâche restaurée."))
        }
    }

    fun selectPomodoroMode(mode: PomodoroMode, customFocus: Int = 35, customBreak: Int = 7) {
        timerJob?.cancel()
        val focus = if (mode == PomodoroMode.PERSONNALISE) customFocus.coerceIn(5, 120) else mode.defaultFocusMinutes
        val brk = if (mode == PomodoroMode.PERSONNALISE) customBreak.coerceIn(1, 45) else mode.defaultBreakMinutes
        _pomodoroState.value = _pomodoroState.value.copy(
            mode = mode,
            focusMinutes = focus,
            breakMinutes = brk,
            isBreakPhase = false,
            remainingSeconds = focus * 60,
            totalPhaseSeconds = focus * 60,
            isRunning = false
        )
        if (mode == PomodoroMode.PERSONNALISE) {
            viewModelScope.launch { preferences.setCustomPomodoro(focus, brk) }
        }
    }

    fun setPomodoroSubject(subject: String) {
        _pomodoroState.value = _pomodoroState.value.copy(subject = subject)
    }

    fun togglePomodoroRunning() {
        val current = _pomodoroState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _pomodoroState.value = current.copy(isRunning = false)
        } else {
            _pomodoroState.value = current.copy(isRunning = true)
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1_000L)
                    val s = _pomodoroState.value
                    if (!s.isRunning) break
                    if (s.remainingSeconds > 1) {
                        _pomodoroState.value = s.copy(remainingSeconds = s.remainingSeconds - 1)
                    } else {
                        completeCurrentPomodoroPhase()
                        break
                    }
                }
            }
        }
    }

    fun resetPomodoro() {
        timerJob?.cancel()
        val s = _pomodoroState.value
        val secs = if (s.isBreakPhase) s.breakMinutes * 60 else s.focusMinutes * 60
        _pomodoroState.value = s.copy(
            remainingSeconds = secs,
            totalPhaseSeconds = secs,
            isRunning = false
        )
    }

    fun completeFocusCycleImmediately() {
        timerJob?.cancel()
        viewModelScope.launch { completeCurrentPomodoroPhase() }
    }

    private suspend fun completeCurrentPomodoroPhase() {
        val s = _pomodoroState.value
        if (!s.isBreakPhase) {
            repository.recordCompletedPomodoro(
                subject = s.subject,
                mode = s.mode,
                focusMinutes = s.focusMinutes,
                breakMinutes = s.breakMinutes
            )
            _events.tryEmit(AppEvent.PomodoroCycleCompleted)
            val breakSecs = s.breakMinutes * 60
            _pomodoroState.value = s.copy(
                isBreakPhase = true,
                remainingSeconds = breakSecs,
                totalPhaseSeconds = breakSecs.coerceAtLeast(1),
                isRunning = false
            )
        } else {
            val focusSecs = s.focusMinutes * 60
            _pomodoroState.value = s.copy(
                isBreakPhase = false,
                remainingSeconds = focusSecs,
                totalPhaseSeconds = focusSecs,
                isRunning = false
            )
            _events.tryEmit(AppEvent.ShowMessage("Pause terminée."))
        }
    }

    fun createDeck(title: String, subject: String, description: String) {
        viewModelScope.launch {
            repository.createDeck(title, subject, description)
            _events.tryEmit(AppEvent.ShowMessage("Paquet créé."))
        }
    }

    fun deleteDeck(deck: FlashcardDeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(deck.localId)
            _events.tryEmit(AppEvent.DeckDeletedWithUndo(deck))
        }
    }

    fun undoDeleteDeck(deck: FlashcardDeckEntity) {
        viewModelScope.launch { repository.restoreDeck(deck) }
    }

    fun addFlashcard(deckId: String, question: String, answer: String, hint: String) {
        viewModelScope.launch {
            repository.addFlashcard(deckId, question, answer, hint)
            _events.tryEmit(AppEvent.ShowMessage("Carte ajoutée."))
        }
    }

    fun reviewFlashcard(card: FlashcardEntity, quality: Sm2Quality, onNext: (FlashcardEntity) -> Unit) {
        viewModelScope.launch {
            val updated = repository.reviewFlashcard(card, quality)
            onNext(updated)
        }
    }

    fun finishFlashcardStudySession() {
        viewModelScope.launch {
            repository.awardFlashcardSessionXp()
            _events.tryEmit(AppEvent.ShowMessage("Session SM-2 terminée : +15 XP !"))
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val res = syncManager.syncNow("Synchronisation manuelle")
            res.onSuccess { conflicts ->
                val msg = if (conflicts > 0) {
                    "Synchronisé ($conflicts conflit(s) résolu(s))."
                } else {
                    "Données synchronisées."
                }
                _events.tryEmit(AppEvent.ShowMessage(msg))
            }.onFailure { err ->
                _events.tryEmit(AppEvent.ShowMessage(err.localizedMessage ?: "Synchronisation différée."))
            }
        }
    }

    fun uploadDriveBackup(accessToken: String) {
        viewModelScope.launch {
            _isDriveBusy.value = true
            try {
                val json = repository.exportFullBackupJson()
                val res = driveClient.uploadBackupJson(accessToken, json)
                res.onSuccess {
                    preferences.setLastDriveBackupEpochMs(System.currentTimeMillis())
                    _events.tryEmit(AppEvent.ShowMessage("Sauvegarde envoyée sur Google Drive AppData."))
                }.onFailure { e ->
                    _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Échec sauvegarde Drive."))
                }
            } finally {
                _isDriveBusy.value = false
            }
        }
    }

    fun restoreDriveBackup(accessToken: String) {
        viewModelScope.launch {
            _isDriveBusy.value = true
            try {
                val meta = driveClient.getLatestBackupMetadata(accessToken).getOrElse { e ->
                    _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur Google Drive."))
                    return@launch
                }
                if (meta == null) {
                    _events.tryEmit(AppEvent.ShowMessage("Aucune sauvegarde trouvée dans Google Drive."))
                    return@launch
                }
                val json = driveClient.downloadBackupJson(accessToken, meta.fileId).getOrElse { e ->
                    _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Échec du téléchargement."))
                    return@launch
                }
                val snapshot = BackupJsonSerializer.deserializeFromJson(json)
                val mergeRes = syncManager.restoreFromBackupSnapshot(snapshot)
                mergeRes.onSuccess { conflicts ->
                    _events.tryEmit(AppEvent.ShowMessage("Sauvegarde restaurée ($conflicts élément(s))."))
                }.onFailure { e ->
                    _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur de restauration."))
                }
            } finally {
                _isDriveBusy.value = false
            }
        }
    }

    suspend fun getExportJson(): String = repository.exportFullBackupJson()
    suspend fun getExportCsv(): String = repository.exportCsvSummary()
    suspend fun getShareSummary(): String = repository.buildShareableSummaryText()

    fun sendPeerNudge(recipientUid: String, recipientName: String, specialty: String, isReminder: Boolean) {
        if (currentUid == null) return
        viewModelScope.launch {
            val senderName = profile.value?.fullName?.ifBlank { currentDisplayName } ?: "Camarade YFUA"
            val type = if (isReminder) "REMINDER" else "ENCOURAGEMENT"
            val msg = if (isReminder) {
                "$senderName vous envoie un rappel d'étude en $specialty !"
            } else {
                "$senderName vous encourage pour vos révisions en $specialty !"
            }
            firestoreService.sendPeerNudge(senderName, recipientUid, specialty, type, msg)
                .onSuccess {
                    _events.tryEmit(
                        AppEvent.ShowMessage(
                            if (isReminder) "Rappel envoyé à $recipientName."
                            else "Encouragement envoyé à $recipientName !"
                        )
                    )
                }.onFailure {
                    _events.tryEmit(AppEvent.ShowMessage("Envoi impossible pour le moment."))
                }
        }
    }

    fun markNudgeRead(nudgeId: String) {
        viewModelScope.launch { firestoreService.markNudgeRead(nudgeId) }
    }

    fun submitSupportTicket(subject: String, message: String) {
        if (currentUid == null) {
            _events.tryEmit(AppEvent.ShowMessage("Connectez-vous pour envoyer une demande de support."))
            return
        }
        viewModelScope.launch {
            val student = profile.value
            val res = firestoreService.submitSupportTicket(
                studentName = student?.fullName?.ifBlank { currentDisplayName } ?: "Étudiant YFUA",
                studentEmail = currentEmail.orEmpty(),
                institution = student?.institution ?: "Institut Universitaire de la Pointe",
                subject = subject,
                message = message
            )
            res.onSuccess {
                _events.tryEmit(AppEvent.ShowMessage("Ticket envoyé au support."))
            }.onFailure { e ->
                _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Échec d'envoi du ticket."))
            }
        }
    }

    fun markTicketResolved(ticketId: String) {
        viewModelScope.launch {
            firestoreService.markSupportTicketResolved(ticketId)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Ticket marqué comme résolu.")) }
        }
    }

    fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch { preferences.setDailyReminder(enabled, hour, minute) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkModeEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSoundEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setHapticsEnabled(enabled) }
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            repository.clearLocalCache()
            _events.tryEmit(AppEvent.ShowMessage("Cache local vidé."))
        }
    }

    fun adminUpdateStudent(targetUid: String, newXp: Long, suspended: Boolean) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminAdjustStudent(targetUid, newXp, suspended)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Compte mis à jour.")) }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur admin.")) }
        }
    }

    fun adminDeleteStudent(targetUid: String) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminDeleteStudent(targetUid)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Profil supprimé.")) }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur suppression.")) }
        }
    }

    fun adminUpsertInstitution(id: String?, name: String, city: String, active: Boolean) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminUpsertInstitution(id, name, city, active)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Établissement enregistré.")) }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur établissement.")) }
        }
    }

    fun adminDeleteInstitution(id: String) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminDeleteInstitution(id)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Établissement supprimé.")) }
        }
    }

    fun adminUpsertOfficialSchedule(
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
    ) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminUpsertOfficialSchedule(
                id, title, subject, category, dueDateEpochMs, priority,
                targetInstitution, targetLevel, targetSpecialty, roomLocation, instructor
            ).onSuccess {
                _events.tryEmit(AppEvent.ShowMessage("Emploi du temps officiel publié."))
            }.onFailure { e ->
                _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur publication."))
            }
        }
    }

    fun adminDeleteOfficialSchedule(id: String) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminDeleteOfficialSchedule(id)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Séance officielle supprimée.")) }
        }
    }

    fun adminPublishAnnouncement(
        title: String,
        body: String,
        priority: AnnouncementPriority,
        targetInstitution: String,
        targetLevel: String,
        targetSpecialty: String,
        pinned: Boolean = true
    ) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminPublishAnnouncement(
                title, body, priority, targetInstitution, targetLevel, targetSpecialty, pinned
            ).onSuccess {
                _events.tryEmit(AppEvent.ShowMessage("Communiqué publié."))
            }.onFailure { e ->
                _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Échec de publication."))
            }
        }
    }

    fun adminUpdateAnnouncement(
        id: String,
        title: String,
        body: String,
        priority: AnnouncementPriority,
        pinned: Boolean,
        archived: Boolean
    ) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminUpdateAnnouncement(id, title, body, priority, pinned, archived)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Communiqué mis à jour.")) }
        }
    }

    fun adminDeleteAnnouncement(id: String) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminDeleteAnnouncement(id)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Communiqué supprimé.")) }
        }
    }

    fun adminReplySupportTicket(ticketId: String, reply: String, status: TicketStatus) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminReplySupportTicket(ticketId, reply, status)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Réponse envoyée à l'étudiant.")) }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur réponse.")) }
        }
    }

    fun sendAiPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isBlank()) return

        val config = appConfigState.value
        if (!config.aiEnabled) {
            _aiGenerationState.value = AiGenerationState.Error(
                "L'assistant IA a été désactivé par l'administrateur."
            )
            return
        }
        if (!isOnline.value) {
            _aiGenerationState.value = AiGenerationState.Error(
                "Connexion Internet requise pour interroger l'assistant IA."
            )
            return
        }
        if (!isSuperAdmin && _aiDailyRequestsUsed.value >= config.aiDailyLimit) {
            _aiGenerationState.value = AiGenerationState.Error(
                "Quota journalier atteint (${config.aiDailyLimit} requêtes/jour)."
            )
            return
        }

        val previousHistory = _aiMessages.value
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = true,
            text = prompt
        )
        _aiMessages.value = previousHistory + userMsg
        _aiGenerationState.value = AiGenerationState.Loading

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            var latestReply = ""
            try {
                aiAssistantRepository.streamReply(
                    prompt = prompt,
                    history = previousHistory,
                    config = config,
                    adminSecrets = if (isSuperAdmin) adminAiSecretsState.value else null,
                    studentProfile = profile.value
                ).collect { accumulated ->
                    latestReply = accumulated
                    _aiGenerationState.value = AiGenerationState.Streaming(accumulated)
                }

                if (latestReply.isNotBlank()) {
                    val modelLabel = if (config.aiProvider == AiProviderMode.CUSTOM_PROXY && config.customApiModel.isNotBlank()) {
                        config.customApiModel
                    } else {
                        config.aiModelName
                    }
                    _aiMessages.value = _aiMessages.value + AiChatMessage(
                        id = UUID.randomUUID().toString(),
                        isFromUser = false,
                        text = latestReply,
                        modelLabel = modelLabel
                    )
                    _aiDailyRequestsUsed.value = _aiDailyRequestsUsed.value + 1
                    _aiGenerationState.value = AiGenerationState.Idle
                } else {
                    _aiGenerationState.value = AiGenerationState.Error("Réponse IA vide.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI inference timed out", e)
                _aiGenerationState.value = AiGenerationState.Error(
                    "Délai d'attente dépassé (60 s). Veuillez réessayer."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _aiGenerationState.value = AiGenerationState.Error(
                    e.localizedMessage ?: "Erreur lors de la génération IA."
                )
            }
        }
    }

    fun cancelAiGeneration() {
        aiJob?.cancel()
        aiJob = null
        val streaming = _aiGenerationState.value as? AiGenerationState.Streaming
        if (streaming != null && streaming.partialText.isNotBlank()) {
            _aiMessages.value = _aiMessages.value + AiChatMessage(
                id = UUID.randomUUID().toString(),
                isFromUser = false,
                text = streaming.partialText,
                modelLabel = appConfigState.value.aiModelName
            )
        }
        _aiGenerationState.value = AiGenerationState.Idle
    }

    fun clearAiConversation() {
        aiJob?.cancel()
        aiJob = null
        _aiMessages.value = emptyList()
        _aiGenerationState.value = AiGenerationState.Idle
    }

    fun adminUpdateAppConfig(config: AppConfigState) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminUpdateAppConfig(config)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Configuration plateforme et IA enregistrée.")) }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur config.")) }
        }
    }

    fun adminToggleAiEnabled(enabled: Boolean) {
        if (!isSuperAdmin) return
        val updated = appConfigState.value.copy(aiEnabled = enabled)
        viewModelScope.launch {
            firestoreService.adminUpdateAppConfig(updated)
                .onSuccess {
                    _events.tryEmit(
                        AppEvent.ShowMessage(if (enabled) "Assistant IA activé pour tous." else "Assistant IA désactivé.")
                    )
                }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur activation IA.")) }
        }
    }

    fun adminUpdateAiSecrets(secrets: AdminAiSecretsState) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminUpdateAiSecrets(secrets, appConfigState.value)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Clés API et webhooks enregistrés en sécurité.")) }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur sauvegarde API.")) }
        }
    }

    fun adminSendPushBroadcast(title: String, body: String, topicType: String, topicValue: String) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            firestoreService.adminSendPushBroadcast(title, body, topicType, topicValue)
                .onSuccess { _events.tryEmit(AppEvent.ShowMessage("Notification Push diffusée.")) }
                .onFailure { e -> _events.tryEmit(AppEvent.ShowMessage(e.localizedMessage ?: "Erreur diffusion Push.")) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        aiJob?.cancel()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
