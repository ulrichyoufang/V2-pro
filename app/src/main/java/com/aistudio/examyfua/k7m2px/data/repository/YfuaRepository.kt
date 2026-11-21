package com.aistudio.examyfua.k7m2px.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.aistudio.examyfua.k7m2px.data.local.AnnouncementCacheEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.data.local.LocalPreferences
import com.aistudio.examyfua.k7m2px.data.local.OfficialScheduleEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.data.local.StudentProfileEntity
import com.aistudio.examyfua.k7m2px.data.local.SupportTicketEntity
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.data.local.YFUAAppDatabase
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.PomodoroMode
import com.aistudio.examyfua.k7m2px.domain.model.Sm2Algorithm
import com.aistudio.examyfua.k7m2px.domain.model.Sm2Quality
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.domain.model.SyncState
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.domain.model.XP_FLASHCARD_SESSION_REWARD
import com.aistudio.examyfua.k7m2px.domain.model.XP_POMODORO_REWARD
import com.aistudio.examyfua.k7m2px.worker.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class YfuaRepository(
    private val context: Context,
    private val database: YFUAAppDatabase,
    private val preferences: LocalPreferences,
    private val syncManager: SyncManager
) {
    val profile: Flow<StudentProfile?> = database.profileDao().observeProfile().map { it?.toDomain() }
    val tasks: Flow<List<TaskEntity>> = database.taskDao().observeActiveTasks()
    val officialSchedules: Flow<List<OfficialScheduleEntity>> = database.officialScheduleDao().observeAll()
    val activeInstitutions: Flow<List<InstitutionEntity>> = database.institutionDao().observeActiveInstitutions()
    val allInstitutions: Flow<List<InstitutionEntity>> = database.institutionDao().observeAllInstitutions()
    val cachedAnnouncements: Flow<List<AnnouncementCacheEntity>> = database.announcementDao().observeActiveAnnouncements()
    val decks: Flow<List<FlashcardDeckEntity>> = database.flashcardDao().observeActiveDecks()
    val flashcards: Flow<List<FlashcardEntity>> = database.flashcardDao().observeAllActiveFlashcards()
    val pomodoroSessions: Flow<List<PomodoroSessionEntity>> = database.pomodoroDao().observeSessions()

    fun observeSupportTickets(uid: String): Flow<List<SupportTicketEntity>> =
        database.supportTicketDao().observeUserTickets(uid)

    suspend fun ensureDefaultInstitution() = withContext(Dispatchers.IO) {
        val current = database.institutionDao().getAllOnce()
        if (current.isEmpty()) {
            database.institutionDao().upsert(
                InstitutionEntity(
                    id = "iup_default",
                    name = DEFAULT_INSTITUTION,
                    city = "Douala",
                    isActive = true,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun saveProfile(
        fullName: String,
        phoneNumber: String,
        photoUri: String,
        institution: String,
        specialty: AcademicSpecialty,
        level: AcademicLevel,
        remoteUid: String?
    ) = withContext(Dispatchers.IO) {
        val existing = database.profileDao().getProfileOnce()
        val now = System.currentTimeMillis()
        val resolvedInstitution = institution.trim().ifBlank {
            existing?.institution?.ifBlank { DEFAULT_INSTITUTION } ?: DEFAULT_INSTITUTION
        }
        val cleanPhone = phoneNumber.trim().take(32)
        val resolvedPhoto = photoUri.trim()
        if (resolvedPhoto.isBlank()) {
            deleteStoredAvatarFiles()
        }
        val updated = (existing ?: StudentProfileEntity()).copy(
            remoteUid = remoteUid ?: existing?.remoteUid,
            fullName = fullName.trim(),
            email = existing?.email ?: "",
            phoneNumber = cleanPhone,
            photoUri = resolvedPhoto,
            institution = resolvedInstitution,
            specialty = specialty.displayName,
            level = level.displayName,
            updatedAtEpochMs = now,
            syncState = SyncState.PENDING_UPLOAD.name
        )
        database.profileDao().upsertProfile(updated)
        syncManager.scheduleDeferredSyncWork()
    }

    suspend fun saveProfile(
        fullName: String,
        email: String,
        phoneNumber: String = "",
        photoUri: String? = null,
        institution: String,
        specialty: AcademicSpecialty,
        level: AcademicLevel,
        remoteUid: String?
    ) = withContext(Dispatchers.IO) {
        val existing = database.profileDao().getProfileOnce()
        val now = System.currentTimeMillis()
        val resolvedInstitution = institution.trim().ifBlank {
            existing?.institution?.ifBlank { DEFAULT_INSTITUTION } ?: DEFAULT_INSTITUTION
        }
        val cleanPhone = phoneNumber.trim().take(32)
        val rawPhoto = photoUri?.trim()
        val resolvedPhoto = when {
            rawPhoto == null -> existing?.photoUri.orEmpty()
            rawPhoto.isBlank() -> {
                deleteStoredAvatarFiles()
                ""
            }
            rawPhoto.startsWith("content://") -> persistProfilePhoto(Uri.parse(rawPhoto))
            else -> rawPhoto
        }
        val updated = (existing ?: StudentProfileEntity()).copy(
            remoteUid = remoteUid ?: existing?.remoteUid,
            fullName = fullName.trim(),
            email = email.trim().ifBlank { existing?.email ?: "" },
            phoneNumber = cleanPhone,
            photoUri = resolvedPhoto,
            institution = resolvedInstitution,
            specialty = specialty.displayName,
            level = level.displayName,
            updatedAtEpochMs = now,
            syncState = SyncState.PENDING_UPLOAD.name
        )
        database.profileDao().upsertProfile(updated)
        syncManager.scheduleDeferredSyncWork()
    }

    suspend fun persistProfilePhoto(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val decoded = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalStateException("Impossible de lire l'image sélectionnée.")

        val maxSide = 400f
        val largest = max(decoded.width, decoded.height).coerceAtLeast(1)
        val scaled = if (largest > maxSide) {
            val scale = maxSide / largest
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        deleteStoredAvatarFiles()
        val destFile = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()

        val existing = database.profileDao().getProfileOnce()
        if (existing != null) {
            database.profileDao().upsertProfile(
                existing.copy(
                    photoUri = destFile.absolutePath,
                    updatedAtEpochMs = System.currentTimeMillis(),
                    syncState = SyncState.PENDING_UPLOAD.name
                )
            )
        }
        destFile.absolutePath
    }

    suspend fun clearProfilePhoto() = withContext(Dispatchers.IO) {
        deleteStoredAvatarFiles()
        val existing = database.profileDao().getProfileOnce() ?: return@withContext
        database.profileDao().upsertProfile(
            existing.copy(
                photoUri = "",
                updatedAtEpochMs = System.currentTimeMillis(),
                syncState = SyncState.PENDING_UPLOAD.name
            )
        )
    }

    private fun deleteStoredAvatarFiles() {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("profile_avatar_") && file.name.endsWith(".jpg")) {
                file.delete()
            }
        }
    }

    suspend fun ensureInitialProfileFromAuth(uid: String, displayName: String?, email: String?) =
        withContext(Dispatchers.IO) {
            val existing = database.profileDao().getProfileOnce()
            val now = System.currentTimeMillis()
            if (existing == null) {
                database.profileDao().upsertProfile(
                    StudentProfileEntity(
                        remoteUid = uid,
                        fullName = displayName?.takeIf { it.isNotBlank() } ?: "",
                        email = email.orEmpty(),
                        institution = DEFAULT_INSTITUTION,
                        updatedAtEpochMs = now,
                        syncState = SyncState.PENDING_UPLOAD.name
                    )
                )
            } else {
                database.profileDao().upsertProfile(
                    existing.copy(
                        remoteUid = uid,
                        email = email?.takeIf { it.isNotBlank() } ?: existing.email,
                        fullName = existing.fullName.ifBlank { displayName.orEmpty() },
                        institution = existing.institution.ifBlank { DEFAULT_INSTITUTION },
                        syncState = SyncState.PENDING_UPLOAD.name
                    )
                )
            }
        }

    suspend fun saveTask(
        localId: String? = null,
        title: String,
        subject: String,
        category: TaskCategory,
        dueDateEpochMs: Long,
        priority: TaskPriority,
        scheduleNotification: Boolean = false
    ): TaskEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = localId?.let { database.taskDao().getTaskById(it) }
        val entity = TaskEntity(
            localId = existing?.localId ?: UUID.randomUUID().toString(),
            title = title.trim(),
            subject = subject.trim(),
            category = category.name,
            dueDateEpochMs = dueDateEpochMs,
            priority = priority.name,
            isCompleted = existing?.isCompleted ?: false,
            isDeleted = false,
            isOfficial = false,
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
            syncVersion = (existing?.syncVersion ?: 0) + 1,
            syncState = SyncState.PENDING_UPLOAD.name
        )
        database.taskDao().upsertTask(entity)
        if (scheduleNotification && category == TaskCategory.EXAMEN && dueDateEpochMs > now) {
            val oneHourBefore = (dueDateEpochMs - 3_600_000L).coerceAtLeast(now + 10_000L)
            NotificationHelper.scheduleExamAlarm(
                context = context,
                taskId = entity.localId,
                title = entity.title,
                subject = entity.subject,
                triggerAtEpochMs = oneHourBefore
            )
        }
        syncManager.scheduleDeferredSyncWork()
        entity
    }

    suspend fun toggleTaskCompleted(task: TaskEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        database.taskDao().upsertTask(
            task.copy(
                isCompleted = !task.isCompleted,
                updatedAtEpochMs = now,
                syncVersion = task.syncVersion + 1,
                syncState = SyncState.PENDING_UPLOAD.name
            )
        )
        syncManager.scheduleDeferredSyncWork()
    }

    suspend fun deleteTask(localId: String) = withContext(Dispatchers.IO) {
        database.taskDao().softDeleteTask(
            localId = localId,
            syncState = SyncState.PENDING_DELETE.name,
            updatedAt = System.currentTimeMillis()
        )
        syncManager.scheduleDeferredSyncWork()
    }

    suspend fun restoreTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        database.taskDao().upsertTask(
            task.copy(
                isDeleted = false,
                updatedAtEpochMs = System.currentTimeMillis(),
                syncVersion = task.syncVersion + 1,
                syncState = SyncState.PENDING_UPLOAD.name
            )
        )
        syncManager.scheduleDeferredSyncWork()
    }

    suspend fun createDeck(title: String, subject: String, description: String) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            database.flashcardDao().upsertDeck(
                FlashcardDeckEntity(
                    title = title.trim(),
                    subject = subject.trim(),
                    description = description.trim(),
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                    syncState = SyncState.PENDING_UPLOAD.name
                )
            )
            syncManager.scheduleDeferredSyncWork()
        }

    suspend fun deleteDeck(deckId: String) = withContext(Dispatchers.IO) {
        database.flashcardDao().softDeleteDeck(
            localId = deckId,
            syncState = SyncState.PENDING_DELETE.name,
            updatedAt = System.currentTimeMillis()
        )
        syncManager.scheduleDeferredSyncWork()
    }

    suspend fun restoreDeck(deck: FlashcardDeckEntity) = withContext(Dispatchers.IO) {
        database.flashcardDao().upsertDeck(
            deck.copy(
                isDeleted = false,
                updatedAtEpochMs = System.currentTimeMillis(),
                syncState = SyncState.PENDING_UPLOAD.name
            )
        )
        syncManager.scheduleDeferredSyncWork()
    }

    suspend fun addFlashcard(deckId: String, question: String, answer: String, hint: String) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            database.flashcardDao().upsertFlashcard(
                FlashcardEntity(
                    deckId = deckId,
                    question = question.trim(),
                    answer = answer.trim(),
                    hint = hint.trim(),
                    nextReviewAtEpochMs = now,
                    updatedAtEpochMs = now,
                    syncState = SyncState.PENDING_UPLOAD.name
                )
            )
            syncManager.scheduleDeferredSyncWork()
        }

    suspend fun reviewFlashcard(card: FlashcardEntity, quality: Sm2Quality): FlashcardEntity =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val sm2 = Sm2Algorithm.calculateNextReview(
                currentEaseFactor = card.easeFactor,
                currentIntervalDays = card.intervalDays,
                currentRepetitions = card.repetitions,
                quality = quality,
                nowEpochMs = now
            )
            val updated = card.copy(
                easeFactor = sm2.easeFactor,
                intervalDays = sm2.intervalDays,
                repetitions = sm2.repetitions,
                nextReviewAtEpochMs = sm2.nextReviewAtEpochMs,
                lastReviewedAtEpochMs = now,
                updatedAtEpochMs = now,
                syncState = SyncState.PENDING_UPLOAD.name
            )
            database.flashcardDao().upsertFlashcard(updated)
            syncManager.scheduleDeferredSyncWork()
            updated
        }

    suspend fun awardFlashcardSessionXp() = withContext(Dispatchers.IO) {
        awardXpAndUpdateStreak(XP_FLASHCARD_SESSION_REWARD)
    }

    suspend fun recordCompletedPomodoro(
        subject: String,
        mode: PomodoroMode,
        focusMinutes: Int,
        breakMinutes: Int
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        database.pomodoroDao().upsertSession(
            PomodoroSessionEntity(
                subject = subject.trim().ifBlank { "Révision générale" },
                mode = mode.name,
                focusMinutes = focusMinutes,
                breakMinutes = breakMinutes,
                xpEarned = XP_POMODORO_REWARD,
                completedAtEpochMs = now,
                updatedAtEpochMs = now,
                syncState = SyncState.PENDING_UPLOAD.name
            )
        )
        awardXpAndUpdateStreak(XP_POMODORO_REWARD)
        syncManager.scheduleDeferredSyncWork()
    }

    private suspend fun awardXpAndUpdateStreak(xpDelta: Int) {
        val now = System.currentTimeMillis()
        val todayEpochDay = now / 86_400_000L
        val existing = database.profileDao().getProfileOnce() ?: StudentProfileEntity()
        val newStreak = when {
            existing.lastStudyDateEpochDay == todayEpochDay -> existing.streakDays.coerceAtLeast(1)
            existing.lastStudyDateEpochDay == todayEpochDay - 1L -> existing.streakDays + 1
            else -> 1
        }
        database.profileDao().upsertProfile(
            existing.copy(
                xpPoints = existing.xpPoints + xpDelta,
                streakDays = newStreak,
                lastStudyDateEpochDay = todayEpochDay,
                updatedAtEpochMs = now,
                syncState = SyncState.PENDING_UPLOAD.name
            )
        )
    }

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val profile = database.profileDao().getProfileOnce()
        val tasks = database.taskDao().getAllActiveTasksOnce()
        val decks = database.flashcardDao().getAllActiveDecksOnce()
        val cards = database.flashcardDao().getAllActiveFlashcardsOnce()
        val pomos = database.pomodoroDao().getAllActiveSessionsOnce()
        BackupJsonSerializer.serializeToJson(profile, tasks, decks, cards, pomos)
    }

    suspend fun exportCsvSummary(): String = withContext(Dispatchers.IO) {
        val tasks = database.taskDao().getAllActiveTasksOnce()
        val pomos = database.pomodoroDao().getAllActiveSessionsOnce()
        BackupJsonSerializer.serializeTasksAndSessionsToCsv(tasks, pomos)
    }

    suspend fun buildShareableSummaryText(): String = withContext(Dispatchers.IO) {
        val profile = database.profileDao().getProfileOnce()?.toDomain()
        val tasks = database.taskDao().getAllActiveTasksOnce()
        val pomos = database.pomodoroDao().getAllActiveSessionsOnce()
        val totalMinutes = pomos.sumOf { it.focusMinutes }
        val pendingTasks = tasks.filter { !it.isCompleted }

        buildString {
            appendLine("=== Bilan Académique YFUA ===")
            if (profile != null && profile.fullName.isNotBlank()) {
                appendLine("Étudiant : ${profile.fullName}")
                if (profile.phoneNumber.isNotBlank()) {
                    appendLine("Téléphone : ${profile.phoneNumber}")
                }
                appendLine("Établissement : ${profile.institution}")
                appendLine("Filière : ${profile.specialty.displayName}")
                appendLine("Niveau : ${profile.level.displayName}")
                appendLine("XP : ${profile.xpPoints} XP • Série : ${profile.streakDays} jour(s)")
            }
            appendLine("Temps de concentration cumulé : ${totalMinutes / 60}h ${totalMinutes % 60}min (${pomos.size} sessions)")
            appendLine("Tâches en cours : ${pendingTasks.size}")
            pendingTasks.take(10).forEach { t ->
                appendLine("  • [${TaskCategory.safeValueOf(t.category).label}] ${t.title} (${t.subject})")
            }
        }
    }

    suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
        database.syncLogDao().clearAllLogs()
        database.taskDao().purgeSyncedDeletedTasks()
        database.announcementDao().clearAll()
        database.officialScheduleDao().clearAll()
        preferences.setLastSyncEpochMs(0L)
    }
}
