package com.aistudio.examyfua.k7m2px.data.remote

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val institution: String = "",
    val specialty: String = "",
    val level: String = "",
    val xpPoints: Long = 0L,
    val streakDays: Long = 0L,
    val totalStudyMinutes: Long = 0L,
    val suspended: Boolean = false,
    val fcmToken: String? = null,
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val category: String = "COURS",
    val dueDateEpochMs: Long = 0L,
    val priority: String = "MOYENNE",
    val completed: Boolean = false,
    val deleted: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
    val syncVersion: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteDeck(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val description: String = "",
    val deleted: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteFlashcard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deckId: String = "",
    val question: String = "",
    val answer: String = "",
    val hint: String = "",
    val easeFactor: Double = 2.5,
    val intervalDays: Long = 0L,
    val repetitions: Long = 0L,
    val nextReviewAtEpochMs: Long = 0L,
    val lastReviewedAtEpochMs: Long = 0L,
    val deleted: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemotePomodoroSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val mode: String = "CLASSIQUE",
    val focusMinutes: Long = 25L,
    val breakMinutes: Long = 5L,
    val xpEarned: Long = 25L,
    val completedAtEpochMs: Long = 0L,
    val deleted: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val initials: String = "YF",
    val displayName: String = "",
    val specialty: String = "",
    val level: String = "",
    val xpPoints: Long = 0L,
    val totalStudyMinutes: Long = 0L,
    val suspended: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteInstitution(
    @DocumentId val id: String = "",
    val name: String = "",
    val city: String = "",
    val active: Boolean = true,
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteOfficialSchedule(
    @DocumentId val id: String = "",
    val title: String = "",
    val subject: String = "",
    val category: String = "COURS",
    val dueDateEpochMs: Long = 0L,
    val priority: String = "HAUTE",
    val targetInstitution: String = "ALL",
    val targetLevel: String = "ALL",
    val targetSpecialty: String = "ALL",
    val roomLocation: String = "",
    val instructor: String = "",
    val authorEmail: String = "",
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteAnnouncement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val priority: String = "INFORMATION",
    val targetInstitution: String = "ALL",
    val targetLevel: String = "ALL",
    val targetSpecialty: String = "ALL",
    val authorEmail: String = "",
    val pinned: Boolean = true,
    val archived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteSupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val institution: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OUVERT",
    val adminReply: String = "",
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteAppConfig(
    @DocumentId val id: String = "",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val registrationEnabled: Boolean = true,
    val aiEnabled: Boolean = true,
    val aiProvider: String = "FIREBASE_AI_LOGIC",
    val aiModelName: String = "gemini-3.8-flash",
    val aiSystemPrompt: String = "",
    val aiTemperature: Double = 0.7,
    val aiDailyLimit: Long = 30L,
    val customApiBaseUrl: String = "",
    val customApiModel: String = "",
    val hasCustomApiKey: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemoteAdminSecrets(
    @DocumentId val id: String = "",
    val customAiApiKey: String = "",
    val platformWebhookUrl: String = "",
    val platformApiSecret: String = "",
    val extraApiHeadersJson: String = "",
    val updatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemotePushBroadcast(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val topicType: String = "GLOBAL",
    val topicValue: String = "ALL",
    val authorEmail: String = "",
    val sentAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@Keep
@IgnoreExtraProperties
data class RemotePeerNudge(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val recipientUid: String = "",
    val specialty: String = "",
    val type: String = "ENCOURAGEMENT",
    val message: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
