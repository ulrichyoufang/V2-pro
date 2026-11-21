package com.aistudio.examyfua.k7m2px

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.examyfua.k7m2px.data.local.AnnouncementCacheEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardDeckEntity
import com.aistudio.examyfua.k7m2px.data.local.FlashcardEntity
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.data.local.OfficialScheduleEntity
import com.aistudio.examyfua.k7m2px.data.local.PomodoroSessionEntity
import com.aistudio.examyfua.k7m2px.data.local.StudentProfileEntity
import com.aistudio.examyfua.k7m2px.data.local.SupportTicketEntity
import com.aistudio.examyfua.k7m2px.data.local.SyncLogEntity
import com.aistudio.examyfua.k7m2px.data.local.TaskEntity
import com.aistudio.examyfua.k7m2px.data.local.YFUAAppDatabase
import com.aistudio.examyfua.k7m2px.data.repository.BackupJsonSerializer
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.AnnouncementPriority
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.PomodoroMode
import com.aistudio.examyfua.k7m2px.domain.model.SUPER_ADMIN_EMAIL
import com.aistudio.examyfua.k7m2px.domain.model.Sm2Algorithm
import com.aistudio.examyfua.k7m2px.domain.model.Sm2Quality
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.domain.model.SyncState
import com.aistudio.examyfua.k7m2px.domain.model.TaskCategory
import com.aistudio.examyfua.k7m2px.domain.model.TaskPriority
import com.aistudio.examyfua.k7m2px.domain.model.TicketStatus
import com.aistudio.examyfua.k7m2px.domain.model.XP_FLASHCARD_SESSION_REWARD
import com.aistudio.examyfua.k7m2px.domain.model.XP_POMODORO_REWARD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YfuaRoomAndDomainTest {

    private lateinit var db: YFUAAppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, YFUAAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `profile dao upserts student with default IUP institution`() = runTest {
        val entity = StudentProfileEntity(
            fullName = "Ulrich Youfang",
            email = SUPER_ADMIN_EMAIL,
            specialty = AcademicSpecialty.MECATRONIQUE.displayName,
            level = AcademicLevel.CYCLE_INGENIEUR.displayName,
            xpPoints = 120,
            streakDays = 5,
            syncState = SyncState.PENDING_UPLOAD.name
        )
        db.profileDao().upsertProfile(entity)

        val loaded = db.profileDao().getProfileOnce()
        assertNotNull(loaded)
        assertEquals(DEFAULT_INSTITUTION, loaded!!.institution)
        assertEquals("Ulrich Youfang", loaded.fullName)
        assertEquals("UY", loaded.toDomain().initials)
    }

    @Test
    fun `task dao supports CRUD dirty queue and soft delete tombstone`() = runTest {
        val task = TaskEntity(
            localId = "task-1",
            title = "Révision Automatique",
            subject = "Automatique",
            category = TaskCategory.EXAMEN.name,
            dueDateEpochMs = 1_800_000_000_000L,
            priority = TaskPriority.HAUTE.name,
            syncState = SyncState.LOCAL_ONLY.name
        )
        db.taskDao().upsertTask(task)

        assertEquals(1, db.taskDao().getAllActiveTasksOnce().size)
        assertEquals(1, db.taskDao().getDirtyTasks().size)

        val updatedTask = task.copy(isCompleted = true, syncState = SyncState.PENDING_UPLOAD.name)
        db.taskDao().updateTask(updatedTask)
        assertTrue(db.taskDao().getTaskById("task-1")!!.isCompleted)

        db.taskDao().softDeleteTask(
            localId = "task-1",
            syncState = SyncState.PENDING_DELETE.name,
            updatedAt = 1_800_000_100_000L
        )

        assertEquals(0, db.taskDao().getAllActiveTasksOnce().size)
        val dirty = db.taskDao().getDirtyTasks()
        assertEquals(1, dirty.size)
        assertTrue(dirty.first().isDeleted)
        assertEquals(SyncState.PENDING_DELETE.name, dirty.first().syncState)

        db.taskDao().deleteTaskEntity(dirty.first())
        assertEquals(0, db.taskDao().getDirtyTasks().size)
    }

    @Test
    fun `flashcard dao and SM2 algorithm update intervals properly`() = runTest {
        val deck = FlashcardDeckEntity(
            localId = "deck-1",
            title = "Électronique de puissance",
            subject = "Électrotechnique",
            syncState = SyncState.LOCAL_ONLY.name
        )
        db.flashcardDao().upsertDeck(deck)

        val now = 1_750_000_000_000L
        val card = FlashcardEntity(
            localId = "card-1",
            deckId = "deck-1",
            question = "Quel composant permet le redressement commandé ?",
            answer = "Le thyristor (SCR)",
            nextReviewAtEpochMs = now,
            syncState = SyncState.LOCAL_ONLY.name
        )
        db.flashcardDao().upsertFlashcard(card)
        assertEquals(1, db.flashcardDao().observeDueFlashcardsForDeck("deck-1", now).first().size)

        val firstPass = Sm2Algorithm.calculateNextReview(
            currentEaseFactor = card.easeFactor,
            currentIntervalDays = card.intervalDays,
            currentRepetitions = card.repetitions,
            quality = Sm2Quality.BIEN,
            nowEpochMs = now
        )
        assertEquals(1, firstPass.repetitions)
        assertEquals(1, firstPass.intervalDays)

        val secondPass = Sm2Algorithm.calculateNextReview(
            currentEaseFactor = firstPass.easeFactor,
            currentIntervalDays = firstPass.intervalDays,
            currentRepetitions = firstPass.repetitions,
            quality = Sm2Quality.FACILE,
            nowEpochMs = now
        )
        assertEquals(2, secondPass.repetitions)
        assertEquals(6, secondPass.intervalDays)

        val resetPass = Sm2Algorithm.calculateNextReview(
            currentEaseFactor = secondPass.easeFactor,
            currentIntervalDays = secondPass.intervalDays,
            currentRepetitions = secondPass.repetitions,
            quality = Sm2Quality.A_REVOIR,
            nowEpochMs = now
        )
        assertEquals(0, resetPass.repetitions)
        assertEquals(1, resetPass.intervalDays)

        val updatedCard = card.copy(
            easeFactor = secondPass.easeFactor,
            intervalDays = secondPass.intervalDays,
            repetitions = secondPass.repetitions,
            nextReviewAtEpochMs = secondPass.nextReviewAtEpochMs,
            syncState = SyncState.PENDING_UPLOAD.name
        )
        db.flashcardDao().updateFlashcard(updatedCard)

        val storedCards = db.flashcardDao().observeFlashcardsForDeck("deck-1").first()
        assertEquals(1, storedCards.size)
        assertEquals(6, storedCards.first().intervalDays)
        assertEquals(2, storedCards.first().repetitions)
        assertEquals(0, db.flashcardDao().observeDueFlashcardsForDeck("deck-1", now).first().size)

        db.flashcardDao().deleteFlashcardEntity(storedCards.first())
        assertEquals(0, db.flashcardDao().observeFlashcardsForDeck("deck-1").first().size)
    }

    @Test
    fun `pomodoro dao and sync log dao persist sessions and prune old logs`() = runTest {
        val session = PomodoroSessionEntity(
            localId = "pomo-1",
            subject = "Thermodynamique",
            mode = PomodoroMode.INTENSIF.name,
            focusMinutes = 50,
            breakMinutes = 10,
            xpEarned = XP_POMODORO_REWARD,
            syncState = SyncState.LOCAL_ONLY.name
        )
        db.pomodoroDao().upsertSession(session)

        val sessions = db.pomodoroDao().getAllActiveSessionsOnce()
        assertEquals(1, sessions.size)
        assertEquals(50, sessions.first().focusMinutes)
        assertEquals(25, XP_POMODORO_REWARD)
        assertEquals(15, XP_FLASHCARD_SESSION_REWARD)

        db.pomodoroDao().updateSession(session.copy(focusMinutes = 55))
        assertEquals(55, db.pomodoroDao().getSessionById("pomo-1")!!.focusMinutes)
        db.pomodoroDao().deleteSessionEntity(db.pomodoroDao().getSessionById("pomo-1")!!)
        assertEquals(0, db.pomodoroDao().getAllActiveSessionsOnce().size)

        db.syncLogDao().insertLog(
            SyncLogEntity(
                id = "log-old",
                entityType = "SYNC",
                entityId = "pomo-1",
                action = "UPLOAD",
                details = "Ancien log",
                timestampEpochMs = 1_000L
            )
        )
        db.syncLogDao().insertLog(
            SyncLogEntity(
                id = "log-new",
                entityType = "SYNC",
                entityId = "pomo-1",
                action = "UPLOAD",
                details = "Nouveau log",
                timestampEpochMs = 5_000L
            )
        )
        db.syncLogDao().pruneOldLogs(cutoffEpochMs = 2_000L)

        val recent = db.syncLogDao().observeRecentLogs().first()
        assertEquals(1, recent.size)
        assertEquals("log-new", recent.first().id)
    }

    @Test
    fun `v2 daos persist institutions official schedules announcements and support tickets`() = runTest {
        db.institutionDao().replaceAll(
            listOf(
                InstitutionEntity("iup", DEFAULT_INSTITUTION, "Douala", true),
                InstitutionEntity("inactive", "Autre École", "Yaoundé", false)
            )
        )
        assertEquals(1, db.institutionDao().observeActiveInstitutions().first().size)
        assertEquals(2, db.institutionDao().observeAllInstitutions().first().size)

        db.officialScheduleDao().replaceAll(
            listOf(
                OfficialScheduleEntity(
                    id = "sched-1",
                    title = "Examen Automatique",
                    subject = "Automatique",
                    category = TaskCategory.EXAMEN.name,
                    dueDateEpochMs = 1_800_000_000_000L,
                    roomLocation = "Amphi A",
                    instructor = "Dr. Nkoulou"
                )
            )
        )
        assertEquals(1, db.officialScheduleDao().observeAll().first().size)

        db.announcementDao().replaceAll(
            listOf(
                AnnouncementCacheEntity("a1", "Urgent", "Cours déplacé", AnnouncementPriority.URGENT.name, pinned = true, archived = false),
                AnnouncementCacheEntity("a2", "Archivé", "Ancien", AnnouncementPriority.INFORMATION.name, pinned = false, archived = true)
            )
        )
        val activeAnnouncements = db.announcementDao().observeActiveAnnouncements().first()
        assertEquals(1, activeAnnouncements.size)
        assertEquals("a1", activeAnnouncements.first().id)

        db.supportTicketDao().replaceForUser(
            "uid-1",
            listOf(
                SupportTicketEntity(
                    id = "t1",
                    userId = "uid-1",
                    studentName = "Jean",
                    studentEmail = "jean@iup.cm",
                    institution = DEFAULT_INSTITUTION,
                    subject = "Emploi du temps",
                    message = "Horaire TP ?",
                    status = TicketStatus.REPONDU.name,
                    adminReply = "Salle B12 à 14h"
                )
            )
        )
        val userTickets = db.supportTicketDao().observeUserTickets("uid-1").first()
        assertEquals(1, userTickets.size)
        assertEquals("Salle B12 à 14h", userTickets.first().adminReply)
    }

    @Test
    fun `database singleton and migrations 1 to 3 preserve offline support`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = YFUAAppDatabase.getInstance(context)
        val second = YFUAAppDatabase.getInstance(context)
        assertTrue(first === second)

        val sqliteDb = first.openHelper.writableDatabase
        YFUAAppDatabase.MIGRATION_1_2.migrate(sqliteDb)
        YFUAAppDatabase.MIGRATION_2_3.migrate(sqliteDb)
        assertNotNull(first.taskDao())
        assertNotNull(first.flashcardDao())
        assertNotNull(first.pomodoroDao())
    }

    @Test
    fun `academic specialties levels and backup serializers match specification`() {
        assertEquals(
            listOf(
                "Mécatronique",
                "Électronique",
                "Électrotechnique",
                "Maintenance Après-Vente Automobile",
                "Froid et Climatisation",
                "Fabrication Mécanique",
                "Génie Civil",
                "Informatique Industrielle"
            ),
            AcademicSpecialty.entries.map { it.displayName }
        )

        assertEquals(
            listOf(
                "Brevet de Technicien Supérieur - Niveau 1",
                "Brevet de Technicien Supérieur - Niveau 2",
                "Licence Professionnelle",
                "Master Professionnel",
                "Cycle Ingénieur"
            ),
            AcademicLevel.entries.map { it.displayName }
        )

        assertEquals(DEFAULT_INSTITUTION, StudentProfile().institution)

        val tasks = listOf(TaskEntity(localId = "t1", title = "TP Méca", subject = "Mécanique", dueDateEpochMs = 1000L))
        val pomos = listOf(PomodoroSessionEntity(localId = "p1", subject = "Mécanique", focusMinutes = 25, breakMinutes = 5, xpEarned = 25))
        val json = BackupJsonSerializer.serializeToJson(
            profile = StudentProfileEntity(
                fullName = "Jean Dupont",
                phoneNumber = "+237 699001122",
                photoUri = "/tmp/avatar.jpg"
            ),
            tasks = tasks,
            decks = listOf(FlashcardDeckEntity(localId = "d1", title = "Deck 1", subject = "Mécanique")),
            flashcards = listOf(FlashcardEntity(localId = "c1", deckId = "d1", question = "Q", answer = "R")),
            pomodoros = pomos
        )
        val restored = BackupJsonSerializer.deserializeFromJson(json)
        assertEquals("Jean Dupont", restored.profile?.fullName)
        assertEquals("+237 699001122", restored.profile?.phoneNumber)
        assertEquals("/tmp/avatar.jpg", restored.profile?.photoUri)
        assertEquals(1, restored.tasks.size)
        assertEquals(1, restored.decks.size)
        assertEquals(1, restored.flashcards.size)
        assertEquals(1, restored.pomodoros.size)

        val csv = BackupJsonSerializer.serializeTasksAndSessionsToCsv(tasks, pomos)
        assertTrue(csv.contains("TP Méca"))
        assertTrue(csv.contains("25 min"))
    }
}
