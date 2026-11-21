package com.aistudio.examyfua.k7m2px.domain.model

import androidx.compose.runtime.Immutable

enum class AcademicSpecialty(val displayName: String) {
    MECATRONIQUE("Mécatronique"),
    ELECTRONIQUE("Électronique"),
    ELECTROTECHNIQUE("Électrotechnique"),
    MAINTENANCE_AUTOMOBILE("Maintenance Après-Vente Automobile"),
    FROID_CLIMATISATION("Froid et Climatisation"),
    FABRICATION_MECANIQUE("Fabrication Mécanique"),
    GENIE_CIVIL("Génie Civil"),
    INFORMATIQUE_INDUSTRIELLE("Informatique Industrielle");

    companion object {
        fun fromDisplayName(value: String?): AcademicSpecialty =
            entries.firstOrNull { it.displayName == value || it.name == value } ?: MECATRONIQUE
    }
}

enum class AcademicLevel(val displayName: String) {
    BTS_1("Brevet de Technicien Supérieur - Niveau 1"),
    BTS_2("Brevet de Technicien Supérieur - Niveau 2"),
    LICENCE_PRO("Licence Professionnelle"),
    MASTER_PRO("Master Professionnel"),
    CYCLE_INGENIEUR("Cycle Ingénieur");

    companion object {
        fun fromDisplayName(value: String?): AcademicLevel =
            entries.firstOrNull { it.displayName == value || it.name == value } ?: BTS_1
    }
}

enum class TaskCategory(val label: String) {
    COURS("Cours"),
    TD("Travaux Dirigés"),
    TP("Travaux Pratiques"),
    EXAMEN("Examen");

    companion object {
        fun safeValueOf(raw: String?): TaskCategory =
            entries.firstOrNull { it.name == raw || it.label == raw } ?: COURS
    }
}

enum class TaskPriority(val label: String) {
    HAUTE("Haute"),
    MOYENNE("Moyenne"),
    BASSE("Basse");

    companion object {
        fun safeValueOf(raw: String?): TaskPriority =
            entries.firstOrNull { it.name == raw || it.label == raw } ?: MOYENNE
    }
}

enum class AnnouncementPriority(val label: String) {
    INFORMATION("Information"),
    IMPORTANT("Important"),
    URGENT("Urgent");

    companion object {
        fun safeValueOf(raw: String?): AnnouncementPriority =
            entries.firstOrNull { it.name == raw || it.label.equals(raw, ignoreCase = true) } ?: INFORMATION
    }
}

enum class TicketStatus(val label: String) {
    OUVERT("Ouvert"),
    REPONDU("Répondu"),
    RESOLU("Résolu");

    companion object {
        fun safeValueOf(raw: String?): TicketStatus =
            entries.firstOrNull { it.name == raw || it.label.equals(raw, ignoreCase = true) } ?: OUVERT
    }
}

enum class PomodoroMode(
    val label: String,
    val defaultFocusMinutes: Int,
    val defaultBreakMinutes: Int
) {
    CLASSIQUE("Classique (25/5)", 25, 5),
    INTENSIF("Intensif (50/10)", 50, 10),
    PERSONNALISE("Personnalisé", 30, 5);

    companion object {
        fun safeValueOf(raw: String?): PomodoroMode =
            entries.firstOrNull { it.name == raw } ?: CLASSIQUE
    }
}

enum class Sm2Quality(val q: Int, val label: String) {
    A_REVOIR(1, "À revoir"),
    DIFFICILE(3, "Difficile"),
    BIEN(4, "Bien"),
    FACILE(5, "Facile")
}

enum class SyncState {
    LOCAL_ONLY,
    PENDING_UPLOAD,
    SYNCED,
    CONFLICT,
    PENDING_DELETE
}

@Immutable
sealed interface SyncProgressState {
    data object Idle : SyncProgressState
    data class OfflineQueued(val pendingCount: Int) : SyncProgressState
    data class Syncing(
        val stepLabel: String,
        val processedCount: Int,
        val totalCount: Int
    ) : SyncProgressState
    data class Synced(
        val syncedAtEpochMs: Long,
        val conflictsResolved: Int = 0
    ) : SyncProgressState
    data class Error(
        val message: String,
        val pendingCount: Int
    ) : SyncProgressState
}

@Immutable
data class StudentProfile(
    val id: String = "local_student",
    val remoteUid: String? = null,
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUri: String = "",
    val institution: String = DEFAULT_INSTITUTION,
    val specialty: AcademicSpecialty = AcademicSpecialty.MECATRONIQUE,
    val level: AcademicLevel = AcademicLevel.BTS_1,
    val xpPoints: Int = 0,
    val streakDays: Int = 0,
    val lastStudyDateEpochDay: Long = 0L,
    val isSuspended: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.LOCAL_ONLY
) {
    val initials: String
        get() {
            val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "YF"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts.first().first()}${parts.last().first()}".uppercase()
            }
        }

    val isConfigured: Boolean
        get() = fullName.isNotBlank()
}

@Immutable
data class SubjectStudyStat(
    val subject: String,
    val totalMinutes: Int,
    val sessionCount: Int
)

@Immutable
data class DriveBackupMetadata(
    val fileId: String,
    val modifiedTimeIso: String,
    val sizeBytes: Long
)

@Immutable
data class AdminDashboardStats(
    val totalStudents: Int = 0,
    val totalStudyMinutes: Long = 0L,
    val totalXp: Long = 0L,
    val openTicketsCount: Int = 0,
    val breakdownBySpecialty: Map<AcademicSpecialty, Int> = emptyMap(),
    val breakdownByInstitution: Map<String, Int> = emptyMap()
)

enum class AiProviderMode(val label: String) {
    FIREBASE_AI_LOGIC("Firebase AI Logic (Gemini)"),
    CUSTOM_PROXY("Endpoint API personnalisé");

    companion object {
        fun safeValueOf(raw: String?): AiProviderMode =
            entries.firstOrNull { it.name == raw } ?: FIREBASE_AI_LOGIC
    }
}

enum class AiGeminiModel(val modelId: String, val label: String) {
    GEMINI_3_8_FLASH("gemini-3.8-flash", "Gemini 3.8 Flash (Recommandé)"),
    GEMINI_3_5_FLASH_LITE("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite (Rapide)"),
    GEMINI_4_ARGON("gemini-4-argon", "Gemini 4 Argon (Raisonnement avancé)");

    companion object {
        fun safeFromId(raw: String?): AiGeminiModel =
            entries.firstOrNull { it.modelId == raw || it.name == raw } ?: GEMINI_3_8_FLASH
    }
}

const val DEFAULT_AI_SYSTEM_PROMPT =
    "Tu es l'assistant académique officiel de YFUA. Réponds en français de façon claire, structurée et pédagogique, adaptée aux filières technologiques et universitaires."

@Immutable
data class AppConfigState(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Maintenance planifiée en cours. Les fonctionnalités hors-ligne restent accessibles.",
    val registrationEnabled: Boolean = true,
    val aiEnabled: Boolean = true,
    val aiProvider: AiProviderMode = AiProviderMode.FIREBASE_AI_LOGIC,
    val aiModelName: String = AiGeminiModel.GEMINI_3_8_FLASH.modelId,
    val aiSystemPrompt: String = DEFAULT_AI_SYSTEM_PROMPT,
    val aiTemperature: Double = 0.7,
    val aiDailyLimit: Int = 30,
    val customApiBaseUrl: String = "",
    val customApiModel: String = "",
    val hasCustomApiKey: Boolean = false,
    val updatedAtEpochMs: Long = 0L
)

@Immutable
data class AdminAiSecretsState(
    val customAiApiKey: String = "",
    val platformWebhookUrl: String = "",
    val platformApiSecret: String = "",
    val extraApiHeadersJson: String = "",
    val updatedAtEpochMs: Long = 0L
)

@Immutable
data class AiChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val modelLabel: String = ""
)

@Immutable
sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data object Loading : AiGenerationState
    data class Streaming(val partialText: String) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

@Immutable
data class PlanningItem(
    val id: String,
    val title: String,
    val subject: String,
    val category: TaskCategory,
    val dueDateEpochMs: Long,
    val priority: TaskPriority,
    val isCompleted: Boolean,
    val isOfficial: Boolean,
    val institution: String = "",
    val roomLocation: String = "",
    val instructor: String = ""
)

const val DEFAULT_INSTITUTION = "Institut Universitaire de la Pointe"
const val SUPER_ADMIN_EMAIL = "ulrichyoufang388@gmail.com"
const val XP_POMODORO_REWARD = 25
const val XP_FLASHCARD_SESSION_REWARD = 15
