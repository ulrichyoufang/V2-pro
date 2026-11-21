package com.aistudio.examyfua.k7m2px.data.repository

import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.data.local.StudentProfileEntity
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.SyncState
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSnapshot(
    val exportedAtEpochMs: Long,
    val profile: StudentProfileEntity?,
    val tasks: List<TaskEntity>,
    val decks: List<FlashcardDeckEntity>,
    val flashcards: List<FlashcardEntity>,
    val pomodoros: List<PomodoroSessionEntity>
)

object BackupJsonSerializer {
    private const val SCHEMA_VERSION = 2

    fun serializeToJson(
        profile: StudentProfileEntity?,
        tasks: List<TaskEntity>,
        decks: List<FlashcardDeckEntity>,
        flashcards: List<FlashcardEntity>,
        pomodoros: List<PomodoroSessionEntity>
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("app", "YFUA")
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        if (profile != null) {
            root.put(
                "profile",
                JSONObject().apply {
                    put("fullName", profile.fullName)
                    put("email", profile.email)
                    put("phoneNumber", profile.phoneNumber)
                    put("photoUri", profile.photoUri)
                    put("institution", profile.institution)
                    put("specialty", profile.specialty)
                    put("level", profile.level)
                    put("xpPoints", profile.xpPoints)
                    put("streakDays", profile.streakDays)
                    put("lastStudyDateEpochDay", profile.lastStudyDateEpochDay)
                    put("updatedAtEpochMs", profile.updatedAtEpochMs)
                }
            )
        }

        val tasksArr = JSONArray()
        tasks.forEach { t ->
            tasksArr.put(
                JSONObject().apply {
                    put("localId", t.localId)
                    put("title", t.title)
                    put("subject", t.subject)
                    put("category", t.category)
                    put("dueDateEpochMs", t.dueDateEpochMs)
                    put("priority", t.priority)
                    put("isCompleted", t.isCompleted)
                    put("isDeleted", t.isDeleted)
                    put("isOfficial", t.isOfficial)
                    put("createdAtEpochMs", t.createdAtEpochMs)
                    put("updatedAtEpochMs", t.updatedAtEpochMs)
                    put("syncVersion", t.syncVersion)
                }
            )
        }
        root.put("tasks", tasksArr)

        val decksArr = JSONArray()
        decks.forEach { d ->
            decksArr.put(
                JSONObject().apply {
                    put("localId", d.localId)
                    put("title", d.title)
                    put("subject", d.subject)
                    put("description", d.description)
                    put("isDeleted", d.isDeleted)
                    put("createdAtEpochMs", d.createdAtEpochMs)
                    put("updatedAtEpochMs", d.updatedAtEpochMs)
                }
            )
        }
        root.put("decks", decksArr)

        val cardsArr = JSONArray()
        flashcards.forEach { c ->
            cardsArr.put(
                JSONObject().apply {
                    put("localId", c.localId)
                    put("deckId", c.deckId)
                    put("question", c.question)
                    put("answer", c.answer)
                    put("hint", c.hint)
                    put("easeFactor", c.easeFactor)
                    put("intervalDays", c.intervalDays)
                    put("repetitions", c.repetitions)
                    put("nextReviewAtEpochMs", c.nextReviewAtEpochMs)
                    put("lastReviewedAtEpochMs", c.lastReviewedAtEpochMs)
                    put("isDeleted", c.isDeleted)
                    put("updatedAtEpochMs", c.updatedAtEpochMs)
                }
            )
        }
        root.put("flashcards", cardsArr)

        val pomosArr = JSONArray()
        pomodoros.forEach { p ->
            pomosArr.put(
                JSONObject().apply {
                    put("localId", p.localId)
                    put("subject", p.subject)
                    put("mode", p.mode)
                    put("focusMinutes", p.focusMinutes)
                    put("breakMinutes", p.breakMinutes)
                    put("xpEarned", p.xpEarned)
                    put("completedAtEpochMs", p.completedAtEpochMs)
                    put("isDeleted", p.isDeleted)
                    put("updatedAtEpochMs", p.updatedAtEpochMs)
                }
            )
        }
        root.put("pomodoros", pomosArr)

        return root.toString(2)
    }

    fun serializeTasksAndSessionsToCsv(
        tasks: List<TaskEntity>,
        pomodoros: List<PomodoroSessionEntity>
    ): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.FRANCE)
        fun esc(s: String): String = "\"${s.replace("\"", "\"\"")}\""
        return buildString {
            appendLine("Type;Titre_ou_Matiere;Discipline;Categorie_ou_Mode;Date;Statut_ou_Duree;Priorite_ou_XP")
            tasks.forEach { t ->
                val cat = TaskCategory.safeValueOf(t.category).label
                val status = if (t.isCompleted) "Terminé" else "En cours"
                appendLine(
                    listOf(
                        esc("Tâche"),
                        esc(t.title),
                        esc(t.subject),
                        esc(cat),
                        esc(fmt.format(Date(t.dueDateEpochMs))),
                        esc(status),
                        esc(t.priority)
                    ).joinToString(";")
                )
            }
            pomodoros.forEach { p ->
                appendLine(
                    listOf(
                        esc("Pomodoro"),
                        esc("Session d'étude"),
                        esc(p.subject),
                        esc(p.mode),
                        esc(fmt.format(Date(p.completedAtEpochMs))),
                        esc("${p.focusMinutes} min"),
                        esc("+${p.xpEarned} XP")
                    ).joinToString(";")
                )
            }
        }
    }

    fun deserializeFromJson(jsonString: String): BackupSnapshot {
        val root = JSONObject(jsonString)
        val exportedAt = root.optLong("exportedAtEpochMs", System.currentTimeMillis())

        val profileObj = root.optJSONObject("profile")
        val profile = profileObj?.let { p ->
            StudentProfileEntity(
                id = "local_student",
                fullName = p.optString("fullName", ""),
                email = p.optString("email", ""),
                phoneNumber = p.optString("phoneNumber", ""),
                photoUri = p.optString("photoUri", ""),
                institution = p.optString("institution", DEFAULT_INSTITUTION).ifBlank { DEFAULT_INSTITUTION },
                specialty = AcademicSpecialty.fromDisplayName(p.optString("specialty")).displayName,
                level = AcademicLevel.fromDisplayName(p.optString("level")).displayName,
                xpPoints = p.optInt("xpPoints", 0),
                streakDays = p.optInt("streakDays", 0),
                lastStudyDateEpochDay = p.optLong("lastStudyDateEpochDay", 0L),
                updatedAtEpochMs = p.optLong("updatedAtEpochMs", exportedAt),
                syncState = SyncState.PENDING_UPLOAD.name
            )
        }

        val tasks = mutableListOf<TaskEntity>()
        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until tasksArr.length()) {
            val o = tasksArr.getJSONObject(i)
            tasks.add(
                TaskEntity(
                    localId = o.getString("localId"),
                    title = o.getString("title"),
                    subject = o.getString("subject"),
                    category = o.optString("category", "COURS"),
                    dueDateEpochMs = o.optLong("dueDateEpochMs", 0L),
                    priority = o.optString("priority", "MOYENNE"),
                    isCompleted = o.optBoolean("isCompleted", false),
                    isDeleted = o.optBoolean("isDeleted", false),
                    isOfficial = o.optBoolean("isOfficial", false),
                    createdAtEpochMs = o.optLong("createdAtEpochMs", exportedAt),
                    updatedAtEpochMs = o.optLong("updatedAtEpochMs", exportedAt),
                    syncVersion = o.optInt("syncVersion", 1),
                    syncState = SyncState.PENDING_UPLOAD.name
                )
            )
        }

        val decks = mutableListOf<FlashcardDeckEntity>()
        val decksArr = root.optJSONArray("decks") ?: JSONArray()
        for (i in 0 until decksArr.length()) {
            val o = decksArr.getJSONObject(i)
            decks.add(
                FlashcardDeckEntity(
                    localId = o.getString("localId"),
                    title = o.getString("title"),
                    subject = o.getString("subject"),
                    description = o.optString("description", ""),
                    isDeleted = o.optBoolean("isDeleted", false),
                    createdAtEpochMs = o.optLong("createdAtEpochMs", exportedAt),
                    updatedAtEpochMs = o.optLong("updatedAtEpochMs", exportedAt),
                    syncState = SyncState.PENDING_UPLOAD.name
                )
            )
        }

        val flashcards = mutableListOf<FlashcardEntity>()
        val cardsArr = root.optJSONArray("flashcards") ?: JSONArray()
        for (i in 0 until cardsArr.length()) {
            val o = cardsArr.getJSONObject(i)
            flashcards.add(
                FlashcardEntity(
                    localId = o.getString("localId"),
                    deckId = o.getString("deckId"),
                    question = o.getString("question"),
                    answer = o.getString("answer"),
                    hint = o.optString("hint", ""),
                    easeFactor = o.optDouble("easeFactor", 2.5),
                    intervalDays = o.optInt("intervalDays", 0),
                    repetitions = o.optInt("repetitions", 0),
                    nextReviewAtEpochMs = o.optLong("nextReviewAtEpochMs", exportedAt),
                    lastReviewedAtEpochMs = o.optLong("lastReviewedAtEpochMs", 0L),
                    isDeleted = o.optBoolean("isDeleted", false),
                    updatedAtEpochMs = o.optLong("updatedAtEpochMs", exportedAt),
                    syncState = SyncState.PENDING_UPLOAD.name
                )
            )
        }

        val pomodoros = mutableListOf<PomodoroSessionEntity>()
        val pomosArr = root.optJSONArray("pomodoros") ?: JSONArray()
        for (i in 0 until pomosArr.length()) {
            val o = pomosArr.getJSONObject(i)
            pomodoros.add(
                PomodoroSessionEntity(
                    localId = o.getString("localId"),
                    subject = o.getString("subject"),
                    mode = o.optString("mode", "CLASSIQUE"),
                    focusMinutes = o.optInt("focusMinutes", 25),
                    breakMinutes = o.optInt("breakMinutes", 5),
                    xpEarned = o.optInt("xpEarned", 25),
                    completedAtEpochMs = o.optLong("completedAtEpochMs", exportedAt),
                    isDeleted = o.optBoolean("isDeleted", false),
                    updatedAtEpochMs = o.optLong("updatedAtEpochMs", exportedAt),
                    syncState = SyncState.PENDING_UPLOAD.name
                )
            )
        }

        return BackupSnapshot(
            exportedAtEpochMs = exportedAt,
            profile = profile,
            tasks = tasks,
            decks = decks,
            flashcards = flashcards,
            pomodoros = pomodoros
        )
    }
}
