package com.aistudio.examyfua.k7m2px.data.remote

import android.content.Context
import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.domain.model.AdminAiSecretsState
import com.aistudio.examyfua.k7m2px.domain.model.AiGeminiModel
import com.aistudio.examyfua.k7m2px.domain.model.AiProviderMode
import com.aistudio.examyfua.k7m2px.domain.model.AnnouncementPriority
import com.aistudio.examyfua.k7m2px.domain.model.AppConfigState
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_AI_SYSTEM_PROMPT
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.SUPER_ADMIN_EMAIL
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.domain.model.TicketStatus
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirestoreService(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Connexion requise pour accéder aux services cloud.")

    private fun requireAdmin(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Authentification requise.")
        if (user.email != SUPER_ADMIN_EMAIL || !user.isEmailVerified) {
            throw SecurityException("Accès refusé : privilèges Super-Administrateur requis.")
        }
        return user.uid
    }

    suspend fun upsertUserProfileAndLeaderboard(
        profile: StudentProfile,
        totalStudyMinutes: Int,
        fcmToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var currentPath = "users/${auth.currentUser?.uid ?: "none"}"
        try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            currentPath = userRef.path
            val snapshot = userRef.get().await()

            val displayName = profile.fullName.ifBlank {
                auth.currentUser?.displayName ?: "Étudiant YFUA"
            }.take(100)
            val safeEmail = (profile.email.ifBlank { auth.currentUser?.email ?: "" }).take(150)
            val safePhone = profile.phoneNumber.trim().take(32)
            val safeInstitution = profile.institution.ifBlank { DEFAULT_INSTITUTION }.trim().take(150)

            if (!snapshot.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "fullName" to displayName,
                    "email" to safeEmail.ifBlank { null },
                    "phoneNumber" to safePhone,
                    "institution" to safeInstitution,
                    "specialty" to profile.specialty.displayName,
                    "level" to profile.level.displayName,
                    "xpPoints" to profile.xpPoints.toLong(),
                    "streakDays" to profile.streakDays.toLong(),
                    "totalStudyMinutes" to totalStudyMinutes.toLong(),
                    "suspended" to false,
                    "fcmToken" to fcmToken?.take(512),
                    "updatedAtEpochMs" to profile.updatedAtEpochMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "fullName" to displayName,
                    "phoneNumber" to safePhone,
                    "institution" to safeInstitution,
                    "specialty" to profile.specialty.displayName,
                    "level" to profile.level.displayName,
                    "xpPoints" to profile.xpPoints.toLong(),
                    "streakDays" to profile.streakDays.toLong(),
                    "totalStudyMinutes" to totalStudyMinutes.toLong(),
                    "updatedAtEpochMs" to profile.updatedAtEpochMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeEmail.isNotBlank()) updatePayload["email"] = safeEmail
                if (!fcmToken.isNullOrBlank()) updatePayload["fcmToken"] = fcmToken.take(512)
                userRef.update(updatePayload).await()
            }

            val lbRef = db.collection("leaderboard").document(uid)
            currentPath = lbRef.path
            val lbSnap = lbRef.get().await()
            val initials = profile.initials.take(4).ifBlank { "YF" }
            if (!lbSnap.exists()) {
                lbRef.set(
                    mapOf(
                        "userId" to uid,
                        "initials" to initials,
                        "displayName" to displayName,
                        "specialty" to profile.specialty.displayName,
                        "level" to profile.level.displayName,
                        "xpPoints" to profile.xpPoints.toLong(),
                        "totalStudyMinutes" to totalStudyMinutes.toLong(),
                        "suspended" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                lbRef.update(
                    mapOf(
                        "initials" to initials,
                        "displayName" to displayName,
                        "specialty" to profile.specialty.displayName,
                        "level" to profile.level.displayName,
                        "xpPoints" to profile.xpPoints.toLong(),
                        "totalStudyMinutes" to totalStudyMinutes.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteUserProfile(uid: String): Result<RemoteUserProfile?> = withContext(Dispatchers.IO) {
        val path = "users/$uid"
        try {
            requireUserId()
            val snap = db.collection("users").document(uid).get().await()
            val profile = snap.toObject(
                RemoteUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun pushTask(task: TaskEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.failure(IllegalStateException("Non connecté"))
        val path = "users/$uid/tasks/${task.localId}"
        try {
            val docRef = db.collection("users").document(uid).collection("tasks").document(task.localId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to task.title.take(200).ifBlank { "Tâche" },
                        "subject" to task.subject.take(120).ifBlank { "Général" },
                        "category" to task.category,
                        "dueDateEpochMs" to task.dueDateEpochMs.coerceAtLeast(0L),
                        "priority" to task.priority,
                        "completed" to task.isCompleted,
                        "deleted" to task.isDeleted,
                        "updatedAtEpochMs" to task.updatedAtEpochMs,
                        "syncVersion" to task.syncVersion.toLong().coerceAtLeast(1L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to task.title.take(200).ifBlank { "Tâche" },
                        "subject" to task.subject.take(120).ifBlank { "Général" },
                        "category" to task.category,
                        "dueDateEpochMs" to task.dueDateEpochMs.coerceAtLeast(0L),
                        "priority" to task.priority,
                        "completed" to task.isCompleted,
                        "deleted" to task.isDeleted,
                        "updatedAtEpochMs" to task.updatedAtEpochMs,
                        "syncVersion" to task.syncVersion.toLong().coerceAtLeast(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteTasks(uid: String): Result<List<RemoteTask>> = withContext(Dispatchers.IO) {
        val path = "users/$uid/tasks"
        try {
            requireUserId()
            val snap = db.collection("users").document(uid).collection("tasks").get().await()
            Result.success(snap.toObjects(RemoteTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun pushDeck(deck: FlashcardDeckEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.failure(IllegalStateException("Non connecté"))
        val path = "users/$uid/decks/${deck.localId}"
        try {
            val docRef = db.collection("users").document(uid).collection("decks").document(deck.localId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to deck.title.take(150).ifBlank { "Paquet" },
                        "subject" to deck.subject.take(120).ifBlank { "Général" },
                        "description" to deck.description.take(400),
                        "deleted" to deck.isDeleted,
                        "updatedAtEpochMs" to deck.updatedAtEpochMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to deck.title.take(150).ifBlank { "Paquet" },
                        "subject" to deck.subject.take(120).ifBlank { "Général" },
                        "description" to deck.description.take(400),
                        "deleted" to deck.isDeleted,
                        "updatedAtEpochMs" to deck.updatedAtEpochMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteDecks(uid: String): Result<List<RemoteDeck>> = withContext(Dispatchers.IO) {
        val path = "users/$uid/decks"
        try {
            requireUserId()
            val snap = db.collection("users").document(uid).collection("decks").get().await()
            Result.success(snap.toObjects(RemoteDeck::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun pushFlashcard(card: FlashcardEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.failure(IllegalStateException("Non connecté"))
        val path = "users/$uid/flashcards/${card.localId}"
        try {
            val docRef = db.collection("users").document(uid).collection("flashcards").document(card.localId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "deckId" to card.deckId,
                        "question" to card.question.take(600).ifBlank { "Question" },
                        "answer" to card.answer.take(1200).ifBlank { "Réponse" },
                        "hint" to card.hint.take(400),
                        "easeFactor" to card.easeFactor.coerceIn(1.3, 5.0),
                        "intervalDays" to card.intervalDays.toLong().coerceIn(0L, 36500L),
                        "repetitions" to card.repetitions.toLong().coerceIn(0L, 10000L),
                        "nextReviewAtEpochMs" to card.nextReviewAtEpochMs.coerceAtLeast(0L),
                        "lastReviewedAtEpochMs" to card.lastReviewedAtEpochMs.coerceAtLeast(0L),
                        "deleted" to card.isDeleted,
                        "updatedAtEpochMs" to card.updatedAtEpochMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "deckId" to card.deckId,
                        "question" to card.question.take(600).ifBlank { "Question" },
                        "answer" to card.answer.take(1200).ifBlank { "Réponse" },
                        "hint" to card.hint.take(400),
                        "easeFactor" to card.easeFactor.coerceIn(1.3, 5.0),
                        "intervalDays" to card.intervalDays.toLong().coerceIn(0L, 36500L),
                        "repetitions" to card.repetitions.toLong().coerceIn(0L, 10000L),
                        "nextReviewAtEpochMs" to card.nextReviewAtEpochMs.coerceAtLeast(0L),
                        "lastReviewedAtEpochMs" to card.lastReviewedAtEpochMs.coerceAtLeast(0L),
                        "deleted" to card.isDeleted,
                        "updatedAtEpochMs" to card.updatedAtEpochMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteFlashcards(uid: String): Result<List<RemoteFlashcard>> = withContext(Dispatchers.IO) {
        val path = "users/$uid/flashcards"
        try {
            requireUserId()
            val snap = db.collection("users").document(uid).collection("flashcards").get().await()
            Result.success(snap.toObjects(RemoteFlashcard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun pushPomodoro(session: PomodoroSessionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.failure(IllegalStateException("Non connecté"))
        val path = "users/$uid/pomodoros/${session.localId}"
        try {
            val docRef = db.collection("users").document(uid).collection("pomodoros").document(session.localId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "subject" to session.subject.take(120).ifBlank { "Étude" },
                        "mode" to session.mode,
                        "focusMinutes" to session.focusMinutes.toLong().coerceIn(1L, 240L),
                        "breakMinutes" to session.breakMinutes.toLong().coerceIn(0L, 120L),
                        "xpEarned" to session.xpEarned.toLong().coerceIn(0L, 500L),
                        "completedAtEpochMs" to session.completedAtEpochMs.coerceAtLeast(0L),
                        "deleted" to session.isDeleted,
                        "updatedAtEpochMs" to session.updatedAtEpochMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "deleted" to session.isDeleted,
                        "updatedAtEpochMs" to session.updatedAtEpochMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchRemotePomodoros(uid: String): Result<List<RemotePomodoroSession>> = withContext(Dispatchers.IO) {
        val path = "users/$uid/pomodoros"
        try {
            requireUserId()
            val snap = db.collection("users").document(uid).collection("pomodoros").get().await()
            Result.success(snap.toObjects(RemotePomodoroSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeLeaderboard(): Flow<List<RemoteLeaderboardEntry>> = flow {
        requireUserId()
        emitAll(
            db.collection("leaderboard")
                .orderBy("xpPoints", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemoteLeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { !it.suspended }
                }
                .catch { error -> handleStreamError(error, "leaderboard") }
        )
    }.flowOn(Dispatchers.IO)

    fun observeInstitutions(): Flow<List<RemoteInstitution>> = flow {
        requireUserId()
        emitAll(
            db.collection("institutions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemoteInstitution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error -> handleStreamError(error, "institutions") }
        )
    }.flowOn(Dispatchers.IO)

    fun observeOfficialSchedules(): Flow<List<RemoteOfficialSchedule>> = flow {
        requireUserId()
        emitAll(
            db.collection("official_schedules")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemoteOfficialSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.dueDateEpochMs }
                }
                .catch { error -> handleStreamError(error, "official_schedules") }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAnnouncements(): Flow<List<RemoteAnnouncement>> = flow {
        requireUserId()
        emitAll(
            db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemoteAnnouncement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, "announcements") }
        )
    }.flowOn(Dispatchers.IO)

    fun observeAppConfig(): Flow<AppConfigState> = flow {
        requireUserId()
        emitAll(
            db.collection("app_config").document("settings")
                .snapshots()
                .map { snap ->
                    val remote = snap.toObject(RemoteAppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (remote == null) {
                        AppConfigState()
                    } else {
                        AppConfigState(
                            maintenanceMode = remote.maintenanceMode,
                            maintenanceMessage = remote.maintenanceMessage.ifBlank {
                                "Maintenance planifiée en cours. Les fonctionnalités hors-ligne restent accessibles."
                            },
                            registrationEnabled = remote.registrationEnabled,
                            aiEnabled = remote.aiEnabled,
                            aiProvider = AiProviderMode.safeValueOf(remote.aiProvider),
                            aiModelName = AiGeminiModel.safeFromId(remote.aiModelName).modelId,
                            aiSystemPrompt = remote.aiSystemPrompt.ifBlank { DEFAULT_AI_SYSTEM_PROMPT },
                            aiTemperature = remote.aiTemperature.coerceIn(0.0, 2.0),
                            aiDailyLimit = remote.aiDailyLimit.toInt().coerceIn(1, 500),
                            customApiBaseUrl = remote.customApiBaseUrl,
                            customApiModel = remote.customApiModel,
                            hasCustomApiKey = remote.hasCustomApiKey,
                            updatedAtEpochMs = remote.updatedAtEpochMs
                        )
                    }
                }
                .catch { error -> handleStreamError(error, "app_config/settings") }
        )
    }.flowOn(Dispatchers.IO)

    fun adminObserveAiSecrets(): Flow<AdminAiSecretsState> = flow {
        requireAdmin()
        emitAll(
            db.collection("admin_secrets").document("ai_config")
                .snapshots()
                .map { snap ->
                    val remote = snap.toObject(RemoteAdminSecrets::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (remote == null) {
                        AdminAiSecretsState()
                    } else {
                        AdminAiSecretsState(
                            customAiApiKey = remote.customAiApiKey,
                            platformWebhookUrl = remote.platformWebhookUrl,
                            platformApiSecret = remote.platformApiSecret,
                            extraApiHeadersJson = remote.extraApiHeadersJson,
                            updatedAtEpochMs = remote.updatedAtEpochMs
                        )
                    }
                }
                .catch { error -> handleStreamError(error, "admin_secrets/ai_config") }
        )
    }.flowOn(Dispatchers.IO)

    fun observeUserSupportTickets(uid: String): Flow<List<RemoteSupportTicket>> = flow {
        requireUserId()
        emitAll(
            db.collection("support_tickets")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemoteSupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAtEpochMs }
                }
                .catch { error -> handleStreamError(error, "support_tickets") }
        )
    }.flowOn(Dispatchers.IO)

    fun observePushBroadcasts(): Flow<List<RemotePushBroadcast>> = flow {
        requireUserId()
        emitAll(
            db.collection("push_broadcasts")
                .orderBy("sentAtEpochMs", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemotePushBroadcast::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, "push_broadcasts") }
        )
    }.flowOn(Dispatchers.IO)

    fun observeReceivedPeerNudges(uid: String): Flow<List<RemotePeerNudge>> = flow {
        requireUserId()
        emitAll(
            db.collection("peer_nudges")
                .whereEqualTo("recipientUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemotePeerNudge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error -> handleStreamError(error, "peer_nudges") }
        )
    }.flowOn(Dispatchers.IO)

    suspend fun submitSupportTicket(
        studentName: String,
        studentEmail: String,
        institution: String,
        subject: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val path = "support_tickets/$id"
        try {
            val uid = requireUserId()
            val nowMs = System.currentTimeMillis()
            db.collection("support_tickets").document(id).set(
                mapOf(
                    "userId" to uid,
                    "studentName" to studentName.trim().take(100).ifBlank { "Étudiant YFUA" },
                    "studentEmail" to studentEmail.trim().take(150),
                    "institution" to institution.trim().take(150).ifBlank { DEFAULT_INSTITUTION },
                    "subject" to subject.trim().take(160),
                    "message" to message.trim().take(2000),
                    "status" to TicketStatus.OUVERT.name,
                    "adminReply" to "",
                    "createdAtEpochMs" to nowMs,
                    "updatedAtEpochMs" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markSupportTicketResolved(ticketId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "support_tickets/$ticketId"
        try {
            requireUserId()
            db.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "status" to TicketStatus.RESOLU.name,
                    "updatedAtEpochMs" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendPeerNudge(
        senderName: String,
        recipientUid: String,
        specialty: String,
        type: String,
        message: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val nudgeId = UUID.randomUUID().toString()
        val path = "peer_nudges/$nudgeId"
        try {
            val uid = requireUserId()
            require(uid != recipientUid) { "Impossible d'envoyer un rappel à soi-même." }
            db.collection("peer_nudges").document(nudgeId).set(
                mapOf(
                    "senderUid" to uid,
                    "senderName" to senderName.take(100).ifBlank { "Camarade YFUA" },
                    "recipientUid" to recipientUid,
                    "specialty" to specialty,
                    "type" to type,
                    "message" to message.take(300).ifBlank { "Courage pour tes révisions !" },
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNudgeRead(nudgeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "peer_nudges/$nudgeId"
        try {
            requireUserId()
            db.collection("peer_nudges").document(nudgeId).update(
                mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsersForAdmin(): Flow<List<RemoteUserProfile>> = flow {
        requireAdmin()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemoteUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, "users") }
        )
    }.flowOn(Dispatchers.IO)

    fun adminObserveAllSupportTickets(): Flow<List<RemoteSupportTicket>> = flow {
        requireAdmin()
        emitAll(
            db.collection("support_tickets")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RemoteSupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAtEpochMs }
                }
                .catch { error -> handleStreamError(error, "support_tickets") }
        )
    }.flowOn(Dispatchers.IO)

    suspend fun adminAdjustStudent(
        targetUid: String,
        newXpPoints: Long,
        suspended: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "users/$targetUid"
        try {
            requireAdmin()
            val nowMs = System.currentTimeMillis()
            db.collection("users").document(targetUid).update(
                mapOf(
                    "xpPoints" to newXpPoints.coerceAtLeast(0L),
                    "suspended" to suspended,
                    "updatedAtEpochMs" to nowMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val lbDoc = db.collection("leaderboard").document(targetUid)
            if (lbDoc.get().await().exists()) {
                lbDoc.update(
                    mapOf(
                        "xpPoints" to newXpPoints.coerceAtLeast(0L),
                        "suspended" to suspended,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteStudent(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "users/$targetUid"
        try {
            requireAdmin()
            db.collection("users").document(targetUid).delete().await()
            db.collection("leaderboard").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpsertInstitution(
        id: String?,
        name: String,
        city: String,
        active: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "institutions/$docId"
        try {
            requireAdmin()
            val docRef = db.collection("institutions").document(docId)
            val exists = docRef.get().await().exists()
            val nowMs = System.currentTimeMillis()
            if (!exists) {
                docRef.set(
                    mapOf(
                        "name" to name.trim().take(150),
                        "city" to city.trim().take(100),
                        "active" to active,
                        "updatedAtEpochMs" to nowMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(150),
                        "city" to city.trim().take(100),
                        "active" to active,
                        "updatedAtEpochMs" to nowMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteInstitution(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "institutions/$id"
        try {
            requireAdmin()
            db.collection("institutions").document(id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpsertOfficialSchedule(
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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "official_schedules/$docId"
        try {
            requireAdmin()
            val docRef = db.collection("official_schedules").document(docId)
            val exists = docRef.get().await().exists()
            val nowMs = System.currentTimeMillis()
            if (!exists) {
                docRef.set(
                    mapOf(
                        "title" to title.trim().take(200),
                        "subject" to subject.trim().take(120),
                        "category" to category.name,
                        "dueDateEpochMs" to dueDateEpochMs.coerceAtLeast(0L),
                        "priority" to priority.name,
                        "targetInstitution" to targetInstitution.ifBlank { "ALL" }.take(150),
                        "targetLevel" to targetLevel.ifBlank { "ALL" },
                        "targetSpecialty" to targetSpecialty.ifBlank { "ALL" },
                        "roomLocation" to roomLocation.trim().take(120),
                        "instructor" to instructor.trim().take(120),
                        "authorEmail" to SUPER_ADMIN_EMAIL,
                        "updatedAtEpochMs" to nowMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to title.trim().take(200),
                        "subject" to subject.trim().take(120),
                        "category" to category.name,
                        "dueDateEpochMs" to dueDateEpochMs.coerceAtLeast(0L),
                        "priority" to priority.name,
                        "targetInstitution" to targetInstitution.ifBlank { "ALL" }.take(150),
                        "targetLevel" to targetLevel.ifBlank { "ALL" },
                        "targetSpecialty" to targetSpecialty.ifBlank { "ALL" },
                        "roomLocation" to roomLocation.trim().take(120),
                        "instructor" to instructor.trim().take(120),
                        "updatedAtEpochMs" to nowMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteOfficialSchedule(scheduleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "official_schedules/$scheduleId"
        try {
            requireAdmin()
            db.collection("official_schedules").document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adminPublishAnnouncement(
        title: String,
        body: String,
        priority: AnnouncementPriority,
        targetInstitution: String,
        targetLevel: String,
        targetSpecialty: String,
        pinned: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val path = "announcements/$id"
        try {
            requireAdmin()
            db.collection("announcements").document(id).set(
                mapOf(
                    "title" to title.trim().take(160),
                    "body" to body.trim().take(1500),
                    "priority" to priority.name,
                    "targetInstitution" to targetInstitution.ifBlank { "ALL" }.take(150),
                    "targetLevel" to targetLevel.ifBlank { "ALL" },
                    "targetSpecialty" to targetSpecialty.ifBlank { "ALL" },
                    "authorEmail" to SUPER_ADMIN_EMAIL,
                    "pinned" to pinned,
                    "archived" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateAnnouncement(
        announcementId: String,
        title: String,
        body: String,
        priority: AnnouncementPriority,
        pinned: Boolean,
        archived: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "announcements/$announcementId"
        try {
            requireAdmin()
            db.collection("announcements").document(announcementId).update(
                mapOf(
                    "title" to title.trim().take(160),
                    "body" to body.trim().take(1500),
                    "priority" to priority.name,
                    "pinned" to pinned,
                    "archived" to archived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteAnnouncement(announcementId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "announcements/$announcementId"
        try {
            requireAdmin()
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adminReplySupportTicket(
        ticketId: String,
        adminReply: String,
        status: TicketStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "support_tickets/$ticketId"
        try {
            requireAdmin()
            db.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "adminReply" to adminReply.trim().take(2000),
                    "status" to status.name,
                    "updatedAtEpochMs" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateAppConfig(config: AppConfigState): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "app_config/settings"
        try {
            requireAdmin()
            val docRef = db.collection("app_config").document("settings")
            val exists = docRef.get().await().exists()
            val nowMs = System.currentTimeMillis()
            val validModel = AiGeminiModel.safeFromId(config.aiModelName).modelId
            val basePayload = mapOf(
                "maintenanceMode" to config.maintenanceMode,
                "maintenanceMessage" to config.maintenanceMessage.trim().take(500),
                "registrationEnabled" to config.registrationEnabled,
                "aiEnabled" to config.aiEnabled,
                "aiProvider" to config.aiProvider.name,
                "aiModelName" to validModel,
                "aiSystemPrompt" to config.aiSystemPrompt.trim().take(2000),
                "aiTemperature" to config.aiTemperature.coerceIn(0.0, 2.0),
                "aiDailyLimit" to config.aiDailyLimit.toLong().coerceIn(1L, 500L),
                "customApiBaseUrl" to config.customApiBaseUrl.trim().take(500),
                "customApiModel" to config.customApiModel.trim().take(120),
                "hasCustomApiKey" to config.hasCustomApiKey,
                "updatedAtEpochMs" to nowMs,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                docRef.set(basePayload + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(basePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateAiSecrets(
        secrets: AdminAiSecretsState,
        currentConfig: AppConfigState
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "admin_secrets/ai_config"
        try {
            requireAdmin()
            val docRef = db.collection("admin_secrets").document("ai_config")
            val exists = docRef.get().await().exists()
            val nowMs = System.currentTimeMillis()
            val cleanKey = secrets.customAiApiKey.trim().take(512)
            val basePayload = mapOf(
                "customAiApiKey" to cleanKey,
                "platformWebhookUrl" to secrets.platformWebhookUrl.trim().take(500),
                "platformApiSecret" to secrets.platformApiSecret.trim().take(512),
                "extraApiHeadersJson" to secrets.extraApiHeadersJson.trim().take(1000),
                "updatedAtEpochMs" to nowMs,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                docRef.set(basePayload + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(basePayload).await()
            }
            adminUpdateAppConfig(currentConfig.copy(hasCustomApiKey = cleanKey.isNotBlank()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminSendPushBroadcast(
        title: String,
        body: String,
        topicType: String,
        topicValue: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val path = "push_broadcasts/$id"
        try {
            requireAdmin()
            val nowMs = System.currentTimeMillis()
            db.collection("push_broadcasts").document(id).set(
                mapOf(
                    "title" to title.trim().take(160),
                    "body" to body.trim().take(500),
                    "topicType" to topicType,
                    "topicValue" to topicValue.trim().take(150).ifBlank { "ALL" },
                    "authorEmail" to SUPER_ADMIN_EMAIL,
                    "sentAtEpochMs" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun handleStreamError(error: Throwable, path: String) {
        val fsError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
            handleFirestoreError(fsError ?: error, OperationType.LIST, path)
        }
        throw error
    }
}
