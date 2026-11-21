package com.aistudio.examyfua.k7m2px.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.AnnouncementPriority
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.PomodoroMode
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.domain.model.SyncState
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.domain.model.TicketStatus
import java.util.UUID

@Immutable
@Entity(tableName = "student_profile")
data class StudentProfileEntity(
    @PrimaryKey val id: String = "local_student",
    val remoteUid: String? = null,
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUri: String = "",
    val institution: String = DEFAULT_INSTITUTION,
    val specialty: String = AcademicSpecialty.MECATRONIQUE.displayName,
    val level: String = AcademicLevel.BTS_1.displayName,
    val xpPoints: Int = 0,
    val streakDays: Int = 0,
    val lastStudyDateEpochDay: Long = 0L,
    val isSuspended: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncState: String = SyncState.LOCAL_ONLY.name
) {
    fun toDomain(): StudentProfile = StudentProfile(
        id = id,
        remoteUid = remoteUid,
        fullName = fullName,
        email = email,
        phoneNumber = phoneNumber,
        photoUri = photoUri,
        institution = institution.ifBlank { DEFAULT_INSTITUTION },
        specialty = AcademicSpecialty.fromDisplayName(specialty),
        level = AcademicLevel.fromDisplayName(level),
        xpPoints = xpPoints,
        streakDays = streakDays,
        lastStudyDateEpochDay = lastStudyDateEpochDay,
        isSuspended = isSuspended,
        updatedAtEpochMs = updatedAtEpochMs,
        syncState = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.LOCAL_ONLY)
    )
}

@Immutable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val category: String = TaskCategory.COURS.name,
    val dueDateEpochMs: Long,
    val priority: String = TaskPriority.MOYENNE.name,
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false,
    val isOfficial: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncVersion: Int = 1,
    val syncState: String = SyncState.LOCAL_ONLY.name
)

@Immutable
@Entity(tableName = "official_schedules")
data class OfficialScheduleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val category: String = TaskCategory.COURS.name,
    val dueDateEpochMs: Long,
    val priority: String = TaskPriority.HAUTE.name,
    val targetInstitution: String = "ALL",
    val targetLevel: String = "ALL",
    val targetSpecialty: String = "ALL",
    val roomLocation: String = "",
    val instructor: String = "",
    val isOfficial: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "institutions")
data class InstitutionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val city: String = "",
    val isActive: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "announcements_cache")
data class AnnouncementCacheEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val priority: String = AnnouncementPriority.INFORMATION.name,
    val targetInstitution: String = "ALL",
    val targetLevel: String = "ALL",
    val targetSpecialty: String = "ALL",
    val pinned: Boolean = true,
    val archived: Boolean = false,
    val isOfficial: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val studentName: String,
    val studentEmail: String,
    val institution: String,
    val subject: String,
    val message: String,
    val status: String = TicketStatus.OUVERT.name,
    val adminReply: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "flashcard_decks")
data class FlashcardDeckEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val description: String = "",
    val isDeleted: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncState: String = SyncState.LOCAL_ONLY.name
)

@Immutable
@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val deckId: String,
    val question: String,
    val answer: String,
    val hint: String = "",
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val nextReviewAtEpochMs: Long = System.currentTimeMillis(),
    val lastReviewedAtEpochMs: Long = 0L,
    val isDeleted: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncState: String = SyncState.LOCAL_ONLY.name
)

@Immutable
@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val subject: String,
    val mode: String = PomodoroMode.CLASSIQUE.name,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val xpEarned: Int,
    val completedAtEpochMs: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncState: String = SyncState.LOCAL_ONLY.name
)

@Immutable
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val action: String,
    val details: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
