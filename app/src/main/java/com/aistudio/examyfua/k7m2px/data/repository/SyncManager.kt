package com.aistudio.examyfua.k7m2px.data.repository

import android.content.Context
import com.aistudio.examyfua.k7m2px.data.local.AnnouncementCacheEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.data.local.LocalPreferences
import com.aistudio.examyfua.k7m2px.data.local.OfficialScheduleEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.data.local.StudentProfileEntity
import com.aistudio.examyfua.k7m2px.data.local.SupportTicketEntity
import com.aistudio.examyfua.k7m2px.data.local.SyncLogEntity
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.data.local.YFUAAppDatabase
import com.aistudio.examyfua.k7m2px.data.remote.FirestoreService
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.domain.model.SyncProgressState
import com.aistudio.examyfua.k7m2px.domain.model.SyncState
import com.aistudio.examyfua.k7m2px.worker.NotificationHelper
import com.aistudio.examyfua.k7m2px.worker.SyncWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer
import kotlin.math.max

class SyncManager(
    private val context: Context,
    private val database: YFUAAppDatabase,
    private val firestoreService: FirestoreService,
    private val preferences: LocalPreferences,
    private val networkMonitor: NetworkMonitor
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var downwardJob: Job? = null
    private var lastSeenBroadcastMs: Long = System.currentTimeMillis()

    private val _syncState = MutableStateFlow<SyncProgressState>(SyncProgressState.Idle)
    val syncState: StateFlow<SyncProgressState> = _syncState.asStateFlow()

    val pendingDirtyCount: Flow<Int> = combine(
        database.taskDao().observeDirtyTaskCount(),
        database.flashcardDao().observeDirtyDeckCount(),
        database.flashcardDao().observeDirtyCardCount(),
        database.pomodoroDao().observeDirtySessionCount()
    ) { t, d, c, p -> t + d + c + p }

    val recentSyncLogs: Flow<List<SyncLogEntity>> = database.syncLogDao().observeRecentLogs()

    init {
        syncScope.launch {
            var wasOnline = networkMonitor.isOnline.value
            networkMonitor.isOnline.collect { online ->
                if (online && !wasOnline && Firebase.auth.currentUser != null) {
                    syncNow(reason = "Reconnexion réseau détectée")
                } else if (!online) {
                    val dirty = countDirtyItems()
                    if (dirty > 0) {
                        _syncState.value = SyncProgressState.OfflineQueued(dirty)
                    }
                }
                wasOnline = online
            }
        }
    }

    fun startRealtimeDownwardSync(
        scope: CoroutineScope,
        uid: String?,
        profileProvider: () -> StudentProfile?
    ) {
        downwardJob?.cancel()
        if (uid == null) return

        downwardJob = scope.launch(Dispatchers.IO) {
            launch {
                firestoreService.observeInstitutions()
                    .catch { }
                    .collect { remoteList ->
                        val mapped = remoteList.map { r ->
                            InstitutionEntity(
                                id = r.id,
                                name = r.name,
                                city = r.city,
                                isActive = r.active,
                                updatedAtEpochMs = r.updatedAtEpochMs
                            )
                        }
                        if (mapped.isNotEmpty()) {
                            database.institutionDao().replaceAll(mapped)
                        }
                    }
            }

            launch {
                firestoreService.observeOfficialSchedules()
                    .catch { }
                    .collect { remoteSchedules ->
                        val mapped = remoteSchedules.map { s ->
                            OfficialScheduleEntity(
                                id = s.id,
                                title = s.title,
                                subject = s.subject,
                                category = s.category,
                                dueDateEpochMs = s.dueDateEpochMs,
                                priority = s.priority,
                                targetInstitution = s.targetInstitution,
                                targetLevel = s.targetLevel,
                                targetSpecialty = s.targetSpecialty,
                                roomLocation = s.roomLocation,
                                instructor = s.instructor,
                                updatedAtEpochMs = s.updatedAtEpochMs
                            )
                        }
                        database.officialScheduleDao().replaceAll(mapped)
                    }
            }

            launch {
                firestoreService.observeAnnouncements()
                    .catch { }
                    .collect { remoteAnnouncements ->
                        val mapped = remoteAnnouncements.map { a ->
                            AnnouncementCacheEntity(
                                id = a.id,
                                title = a.title,
                                body = a.body,
                                priority = a.priority,
                                targetInstitution = a.targetInstitution,
                                targetLevel = a.targetLevel,
                                targetSpecialty = a.targetSpecialty,
                                pinned = a.pinned,
                                archived = a.archived,
                                updatedAtEpochMs = a.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        }
                        database.announcementDao().replaceAll(mapped)
                    }
            }

            launch {
                firestoreService.observeUserSupportTickets(uid)
                    .catch { }
                    .collect { remoteTickets ->
                        val mapped = remoteTickets.map { t ->
                            SupportTicketEntity(
                                id = t.id,
                                userId = t.userId,
                                studentName = t.studentName,
                                studentEmail = t.studentEmail,
                                institution = t.institution,
                                subject = t.subject,
                                message = t.message,
                                status = t.status,
                                adminReply = t.adminReply,
                                createdAtEpochMs = t.createdAtEpochMs,
                                updatedAtEpochMs = t.updatedAtEpochMs
                            )
                        }
                        database.supportTicketDao().replaceForUser(uid, mapped)
                    }
            }

            launch {
                firestoreService.observePushBroadcasts()
                    .catch { }
                    .collect { broadcasts ->
                        val student = profileProvider()
                        val newest = broadcasts.firstOrNull { it.sentAtEpochMs > lastSeenBroadcastMs }
                        if (newest != null) {
                            val matches = when (newest.topicType) {
                                "GLOBAL" -> true
                                "INSTITUT" -> student?.institution.equals(newest.topicValue, ignoreCase = true)
                                "NIVEAU" -> student?.level?.displayName.equals(newest.topicValue, ignoreCase = true)
                                "SPECIALITE" -> student?.specialty?.displayName.equals(newest.topicValue, ignoreCase = true)
                                else -> false
                            }
                            if (matches) {
                                lastSeenBroadcastMs = newest.sentAtEpochMs
                                NotificationHelper.showNotification(
                                    context = context,
                                    channelId = NotificationHelper.CHANNEL_PEER,
                                    notificationId = newest.id.hashCode(),
                                    title = newest.title,
                                    body = newest.body
                                )
                            }
                        }
                    }
            }
        }
    }

    fun syncFcmTopicSubscriptions(profile: StudentProfile?) {
        if (profile == null) return
        try {
            val messaging = FirebaseMessaging.getInstance()
            messaging.subscribeToTopic("global")
            messaging.subscribeToTopic("inst_${toTopicSlug(profile.institution)}")
            messaging.subscribeToTopic("level_${toTopicSlug(profile.level.displayName)}")
            messaging.subscribeToTopic("spec_${toTopicSlug(profile.specialty.displayName)}")
        } catch (_: Exception) {
        }
    }

    private fun toTopicSlug(raw: String): String {
        val normalized = Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return normalized.ifBlank { "default" }.take(64)
    }

    fun scheduleDeferredSyncWork() {
        SyncWorker.enqueueOneTimeWhenConnected(context)
        syncScope.launch {
            val dirty = countDirtyItems()
            if (!networkMonitor.checkCurrentConnectivity() || Firebase.auth.currentUser == null) {
                if (dirty > 0) _syncState.value = SyncProgressState.OfflineQueued(dirty)
            }
        }
    }

    private suspend fun countDirtyItems(): Int {
        return database.taskDao().getDirtyTasks().size +
            database.flashcardDao().getDirtyDecks().size +
            database.flashcardDao().getDirtyFlashcards().size +
            database.pomodoroDao().getDirtySessions().size
    }

    suspend fun syncNow(reason: String = "Synchronisation manuelle"): Result<Int> = syncMutex.withLock {
        val currentUser = Firebase.auth.currentUser
            ?: return Result.failure(IllegalStateException("Mode hors-ligne actif : connectez-vous pour synchroniser."))

        if (!networkMonitor.checkCurrentConnectivity()) {
            val dirty = countDirtyItems()
            _syncState.value = SyncProgressState.OfflineQueued(dirty)
            return Result.failure(IllegalStateException("Réseau indisponible. $dirty modification(s) en attente."))
        }

        val uid = currentUser.uid
        var conflictsResolved = 0
        return try {
            val dirtyTasks = database.taskDao().getDirtyTasks()
            val dirtyDecks = database.flashcardDao().getDirtyDecks()
            val dirtyCards = database.flashcardDao().getDirtyFlashcards()
            val dirtySessions = database.pomodoroDao().getDirtySessions()
            val totalPush = dirtyTasks.size + dirtyDecks.size + dirtyCards.size + dirtySessions.size + 1
            var processed = 0

            _syncState.value = SyncProgressState.Syncing(
                stepLabel = "Synchronisation ($reason)…",
                processedCount = 0,
                totalCount = totalPush + 4
            )

            for (task in dirtyTasks) {
                val pushRes = firestoreService.pushTask(task)
                if (pushRes.isSuccess) {
                    database.taskDao().upsertTask(task.copy(syncState = SyncState.SYNCED.name))
                }
                processed++
                _syncState.value = SyncProgressState.Syncing("Envoi des tâches…", processed, totalPush + 4)
            }

            for (deck in dirtyDecks) {
                val pushRes = firestoreService.pushDeck(deck)
                if (pushRes.isSuccess) {
                    database.flashcardDao().upsertDeck(deck.copy(syncState = SyncState.SYNCED.name))
                }
                processed++
                _syncState.value = SyncProgressState.Syncing("Envoi des paquets…", processed, totalPush + 4)
            }

            for (card in dirtyCards) {
                val pushRes = firestoreService.pushFlashcard(card)
                if (pushRes.isSuccess) {
                    database.flashcardDao().upsertFlashcard(card.copy(syncState = SyncState.SYNCED.name))
                }
                processed++
                _syncState.value = SyncProgressState.Syncing("Envoi des flashcards SM-2…", processed, totalPush + 4)
            }

            for (session in dirtySessions) {
                val pushRes = firestoreService.pushPomodoro(session)
                if (pushRes.isSuccess) {
                    database.pomodoroDao().upsertSession(session.copy(syncState = SyncState.SYNCED.name))
                }
                processed++
                _syncState.value = SyncProgressState.Syncing("Envoi Pomodoro…", processed, totalPush + 4)
            }

            _syncState.value = SyncProgressState.Syncing("Réception des tâches…", processed + 1, totalPush + 4)
            val remoteTasks = firestoreService.fetchRemoteTasks(uid).getOrElse { emptyList() }
            for (rt in remoteTasks) {
                val local = database.taskDao().getTaskById(rt.id)
                if (local == null) {
                    database.taskDao().upsertTask(
                        TaskEntity(
                            localId = rt.id,
                            title = rt.title,
                            subject = rt.subject,
                            category = rt.category,
                            dueDateEpochMs = rt.dueDateEpochMs,
                            priority = rt.priority,
                            isCompleted = rt.completed,
                            isDeleted = rt.deleted,
                            createdAtEpochMs = rt.createdAt?.toDate()?.time ?: rt.updatedAtEpochMs,
                            updatedAtEpochMs = rt.updatedAtEpochMs,
                            syncVersion = rt.syncVersion.toInt(),
                            syncState = SyncState.SYNCED.name
                        )
                    )
                } else if (rt.updatedAtEpochMs > local.updatedAtEpochMs) {
                    val hadConflict = local.syncState != SyncState.SYNCED.name
                    val mergedCompletion = if (hadConflict) (local.isCompleted || rt.completed) else rt.completed
                    database.taskDao().upsertTask(
                        local.copy(
                            title = rt.title,
                            subject = rt.subject,
                            category = rt.category,
                            dueDateEpochMs = rt.dueDateEpochMs,
                            priority = rt.priority,
                            isCompleted = mergedCompletion,
                            isDeleted = rt.deleted,
                            updatedAtEpochMs = rt.updatedAtEpochMs,
                            syncVersion = max(local.syncVersion, rt.syncVersion.toInt()),
                            syncState = SyncState.SYNCED.name
                        )
                    )
                    if (hadConflict) {
                        conflictsResolved++
                        logSync("Tâche", rt.id, "CONFLIT_LWW", "Version distante appliquée")
                    }
                }
            }

            _syncState.value = SyncProgressState.Syncing("Fusion SM-2…", processed + 2, totalPush + 4)
            val remoteDecks = firestoreService.fetchRemoteDecks(uid).getOrElse { emptyList() }
            for (rd in remoteDecks) {
                val local = database.flashcardDao().getDeckById(rd.id)
                if (local == null || rd.updatedAtEpochMs > local.updatedAtEpochMs) {
                    database.flashcardDao().upsertDeck(
                        FlashcardDeckEntity(
                            localId = rd.id,
                            title = rd.title,
                            subject = rd.subject,
                            description = rd.description,
                            isDeleted = rd.deleted,
                            createdAtEpochMs = local?.createdAtEpochMs ?: rd.updatedAtEpochMs,
                            updatedAtEpochMs = rd.updatedAtEpochMs,
                            syncState = SyncState.SYNCED.name
                        )
                    )
                }
            }

            val remoteCards = firestoreService.fetchRemoteFlashcards(uid).getOrElse { emptyList() }
            for (rc in remoteCards) {
                val local = database.flashcardDao().getFlashcardById(rc.id)
                if (local == null) {
                    database.flashcardDao().upsertFlashcard(
                        FlashcardEntity(
                            localId = rc.id,
                            deckId = rc.deckId,
                            question = rc.question,
                            answer = rc.answer,
                            hint = rc.hint,
                            easeFactor = rc.easeFactor,
                            intervalDays = rc.intervalDays.toInt(),
                            repetitions = rc.repetitions.toInt(),
                            nextReviewAtEpochMs = rc.nextReviewAtEpochMs,
                            lastReviewedAtEpochMs = rc.lastReviewedAtEpochMs,
                            isDeleted = rc.deleted,
                            updatedAtEpochMs = rc.updatedAtEpochMs,
                            syncState = SyncState.SYNCED.name
                        )
                    )
                } else {
                    val preferRemoteSm2 = rc.lastReviewedAtEpochMs > local.lastReviewedAtEpochMs ||
                        (rc.lastReviewedAtEpochMs == local.lastReviewedAtEpochMs && rc.repetitions.toInt() > local.repetitions)
                    val preferRemoteContent = rc.updatedAtEpochMs > local.updatedAtEpochMs

                    if (preferRemoteSm2 || preferRemoteContent) {
                        if (local.syncState != SyncState.SYNCED.name) {
                            conflictsResolved++
                            logSync("Flashcard", rc.id, "CONFLIT_SM2", "Progression SM-2 conservée")
                        }
                        database.flashcardDao().upsertFlashcard(
                            local.copy(
                                question = if (preferRemoteContent) rc.question else local.question,
                                answer = if (preferRemoteContent) rc.answer else local.answer,
                                hint = if (preferRemoteContent) rc.hint else local.hint,
                                isDeleted = if (preferRemoteContent) rc.deleted else local.isDeleted,
                                easeFactor = if (preferRemoteSm2) rc.easeFactor else local.easeFactor,
                                intervalDays = if (preferRemoteSm2) rc.intervalDays.toInt() else local.intervalDays,
                                repetitions = max(local.repetitions, rc.repetitions.toInt()),
                                nextReviewAtEpochMs = if (preferRemoteSm2) rc.nextReviewAtEpochMs else local.nextReviewAtEpochMs,
                                lastReviewedAtEpochMs = max(local.lastReviewedAtEpochMs, rc.lastReviewedAtEpochMs),
                                updatedAtEpochMs = max(local.updatedAtEpochMs, rc.updatedAtEpochMs),
                                syncState = SyncState.SYNCED.name
                            )
                        )
                    }
                }
            }

            _syncState.value = SyncProgressState.Syncing("Historique Pomodoro…", processed + 3, totalPush + 4)
            val remotePomodoros = firestoreService.fetchRemotePomodoros(uid).getOrElse { emptyList() }
            for (rp in remotePomodoros) {
                val local = database.pomodoroDao().getSessionById(rp.id)
                if (local == null) {
                    database.pomodoroDao().upsertSession(
                        PomodoroSessionEntity(
                            localId = rp.id,
                            subject = rp.subject,
                            mode = rp.mode,
                            focusMinutes = rp.focusMinutes.toInt(),
                            breakMinutes = rp.breakMinutes.toInt(),
                            xpEarned = rp.xpEarned.toInt(),
                            completedAtEpochMs = rp.completedAtEpochMs,
                            isDeleted = rp.deleted,
                            updatedAtEpochMs = rp.updatedAtEpochMs,
                            syncState = SyncState.SYNCED.name
                        )
                    )
                }
            }

            _syncState.value = SyncProgressState.Syncing("Profil & XP…", totalPush + 4, totalPush + 4)
            val localProfileEntity = database.profileDao().getProfileOnce() ?: StudentProfileEntity(
                fullName = currentUser.displayName ?: "Étudiant YFUA",
                email = currentUser.email ?: "",
                institution = DEFAULT_INSTITUTION
            )
            val remoteProfile = firestoreService.fetchRemoteUserProfile(uid).getOrNull()
            val totalMinutes = database.pomodoroDao().getAllActiveSessionsOnce().sumOf { it.focusMinutes }
            val mergedProfileEntity = if (remoteProfile != null) {
                val takeRemoteMeta = remoteProfile.updatedAtEpochMs > localProfileEntity.updatedAtEpochMs &&
                    remoteProfile.fullName.isNotBlank()
                localProfileEntity.copy(
                    remoteUid = uid,
                    fullName = if (takeRemoteMeta) remoteProfile.fullName else localProfileEntity.fullName.ifBlank { remoteProfile.fullName },
                    email = currentUser.email ?: localProfileEntity.email,
                    phoneNumber = if (takeRemoteMeta && remoteProfile.phoneNumber.isNotBlank()) {
                        remoteProfile.phoneNumber
                    } else {
                        localProfileEntity.phoneNumber.ifBlank { remoteProfile.phoneNumber }
                    },
                    institution = if (takeRemoteMeta && remoteProfile.institution.isNotBlank()) {
                        remoteProfile.institution
                    } else {
                        localProfileEntity.institution.ifBlank { DEFAULT_INSTITUTION }
                    },
                    specialty = if (takeRemoteMeta) AcademicSpecialty.fromDisplayName(remoteProfile.specialty).displayName else localProfileEntity.specialty,
                    level = if (takeRemoteMeta) AcademicLevel.fromDisplayName(remoteProfile.level).displayName else localProfileEntity.level,
                    xpPoints = max(localProfileEntity.xpPoints, remoteProfile.xpPoints.toInt()),
                    streakDays = max(localProfileEntity.streakDays, remoteProfile.streakDays.toInt()),
                    isSuspended = remoteProfile.suspended,
                    updatedAtEpochMs = max(localProfileEntity.updatedAtEpochMs, remoteProfile.updatedAtEpochMs),
                    syncState = SyncState.SYNCED.name
                )
            } else {
                localProfileEntity.copy(
                    remoteUid = uid,
                    email = currentUser.email ?: localProfileEntity.email,
                    institution = localProfileEntity.institution.ifBlank { DEFAULT_INSTITUTION },
                    syncState = SyncState.SYNCED.name
                )
            }
            database.profileDao().upsertProfile(mergedProfileEntity)
            syncFcmTopicSubscriptions(mergedProfileEntity.toDomain())

            val fcmToken = preferences.fcmToken.first()
            firestoreService.upsertUserProfileAndLeaderboard(
                profile = mergedProfileEntity.toDomain(),
                totalStudyMinutes = totalMinutes,
                fcmToken = fcmToken
            )

            val now = System.currentTimeMillis()
            preferences.setLastSyncEpochMs(now)
            logSync("Global", uid, "SYNC_OK", "$totalPush élément(s), $conflictsResolved conflit(s) résolu(s)")
            _syncState.value = SyncProgressState.Synced(now, conflictsResolved)
            Result.success(conflictsResolved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val dirty = countDirtyItems()
            logSync("Global", uid, "SYNC_ERR", e.localizedMessage ?: "Erreur réseau")
            _syncState.value = SyncProgressState.Error(
                message = e.localizedMessage ?: "Échec de synchronisation",
                pendingCount = dirty
            )
            Result.failure(e)
        }
    }

    suspend fun restoreFromBackupSnapshot(snapshot: BackupSnapshot): Result<Int> = syncMutex.withLock {
        var conflicts = 0
        try {
            snapshot.profile?.let { rp ->
                val local = database.profileDao().getProfileOnce()
                val merged = if (local == null) {
                    rp
                } else {
                    local.copy(
                        fullName = if (rp.updatedAtEpochMs > local.updatedAtEpochMs && rp.fullName.isNotBlank()) rp.fullName else local.fullName,
                        phoneNumber = if (rp.updatedAtEpochMs > local.updatedAtEpochMs && rp.phoneNumber.isNotBlank()) rp.phoneNumber else local.phoneNumber,
                        photoUri = local.photoUri.ifBlank { rp.photoUri },
                        institution = if (rp.updatedAtEpochMs > local.updatedAtEpochMs && rp.institution.isNotBlank()) rp.institution else local.institution,
                        specialty = if (rp.updatedAtEpochMs > local.updatedAtEpochMs) rp.specialty else local.specialty,
                        level = if (rp.updatedAtEpochMs > local.updatedAtEpochMs) rp.level else local.level,
                        xpPoints = max(local.xpPoints, rp.xpPoints),
                        streakDays = max(local.streakDays, rp.streakDays),
                        updatedAtEpochMs = max(local.updatedAtEpochMs, rp.updatedAtEpochMs),
                        syncState = SyncState.PENDING_UPLOAD.name
                    )
                }
                database.profileDao().upsertProfile(merged)
            }

            snapshot.tasks.forEach { rt ->
                val local = database.taskDao().getTaskById(rt.localId)
                if (local == null || rt.updatedAtEpochMs > local.updatedAtEpochMs) {
                    if (local != null) conflicts++
                    database.taskDao().upsertTask(rt.copy(syncState = SyncState.PENDING_UPLOAD.name))
                }
            }

            snapshot.decks.forEach { rd ->
                val local = database.flashcardDao().getDeckById(rd.localId)
                if (local == null || rd.updatedAtEpochMs > local.updatedAtEpochMs) {
                    database.flashcardDao().upsertDeck(rd.copy(syncState = SyncState.PENDING_UPLOAD.name))
                }
            }

            snapshot.flashcards.forEach { rc ->
                val local = database.flashcardDao().getFlashcardById(rc.localId)
                if (local == null || rc.lastReviewedAtEpochMs > local.lastReviewedAtEpochMs || rc.updatedAtEpochMs > local.updatedAtEpochMs) {
                    if (local != null) conflicts++
                    database.flashcardDao().upsertFlashcard(rc.copy(syncState = SyncState.PENDING_UPLOAD.name))
                }
            }

            snapshot.pomodoros.forEach { rp ->
                val local = database.pomodoroDao().getSessionById(rp.localId)
                if (local == null) {
                    database.pomodoroDao().upsertSession(rp.copy(syncState = SyncState.PENDING_UPLOAD.name))
                }
            }

            logSync("GoogleDrive", "appDataFolder", "RESTORE_OK", "Restauration ($conflicts conflit(s))")
            Result.success(conflicts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun logSync(entityType: String, entityId: String, action: String, details: String) {
        database.syncLogDao().insertLog(
            SyncLogEntity(
                entityType = entityType,
                entityId = entityId,
                action = action,
                details = details
            )
        )
    }
}
