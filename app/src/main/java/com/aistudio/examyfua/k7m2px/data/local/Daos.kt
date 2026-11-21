package com.aistudio.examyfua.k7m2px.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM student_profile WHERE id = 'local_student' LIMIT 1")
    fun observeProfile(): Flow<StudentProfileEntity?>

    @Query("SELECT * FROM student_profile WHERE id = 'local_student' LIMIT 1")
    suspend fun getProfileOnce(): StudentProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: StudentProfileEntity)

    @Update
    suspend fun updateProfile(profile: StudentProfileEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isCompleted ASC, dueDateEpochMs ASC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    suspend fun getAllActiveTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE syncState != 'SYNCED'")
    suspend fun getDirtyTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE localId = :localId LIMIT 1")
    suspend fun getTaskById(localId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTaskEntity(task: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE localId = :localId")
    suspend fun softDeleteTask(localId: String, syncState: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE syncState != 'SYNCED'")
    fun observeDirtyTaskCount(): Flow<Int>

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND syncState = 'SYNCED'")
    suspend fun purgeSyncedDeletedTasks()
}

@Dao
interface OfficialScheduleDao {
    @Query("SELECT * FROM official_schedules ORDER BY dueDateEpochMs ASC")
    fun observeAll(): Flow<List<OfficialScheduleEntity>>

    @Query("SELECT * FROM official_schedules ORDER BY dueDateEpochMs ASC")
    suspend fun getAllOnce(): List<OfficialScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<OfficialScheduleEntity>)

    @Query("DELETE FROM official_schedules")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(items: List<OfficialScheduleEntity>) {
        clearAll()
        if (items.isNotEmpty()) upsertAll(items)
    }
}

@Dao
interface InstitutionDao {
    @Query("SELECT * FROM institutions WHERE isActive = 1 ORDER BY name ASC")
    fun observeActiveInstitutions(): Flow<List<InstitutionEntity>>

    @Query("SELECT * FROM institutions ORDER BY name ASC")
    fun observeAllInstitutions(): Flow<List<InstitutionEntity>>

    @Query("SELECT * FROM institutions")
    suspend fun getAllOnce(): List<InstitutionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(institution: InstitutionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(institutions: List<InstitutionEntity>)

    @Query("DELETE FROM institutions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(institutions: List<InstitutionEntity>) {
        clearAll()
        if (institutions.isNotEmpty()) upsertAll(institutions)
    }
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements_cache WHERE archived = 0 ORDER BY pinned DESC, updatedAtEpochMs DESC")
    fun observeActiveAnnouncements(): Flow<List<AnnouncementCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AnnouncementCacheEntity>)

    @Query("DELETE FROM announcements_cache")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(items: List<AnnouncementCacheEntity>) {
        clearAll()
        if (items.isNotEmpty()) upsertAll(items)
    }
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets WHERE userId = :uid ORDER BY updatedAtEpochMs DESC")
    fun observeUserTickets(uid: String): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTicket(ticket: SupportTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tickets: List<SupportTicketEntity>)

    @Query("DELETE FROM support_tickets WHERE userId = :uid")
    suspend fun clearForUser(uid: String)

    @Transaction
    suspend fun replaceForUser(uid: String, tickets: List<SupportTicketEntity>) {
        clearForUser(uid)
        if (tickets.isNotEmpty()) upsertAll(tickets)
    }
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_decks WHERE isDeleted = 0 ORDER BY updatedAtEpochMs DESC")
    fun observeActiveDecks(): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE isDeleted = 0")
    suspend fun getAllActiveDecksOnce(): List<FlashcardDeckEntity>

    @Query("SELECT * FROM flashcard_decks WHERE syncState != 'SYNCED'")
    suspend fun getDirtyDecks(): List<FlashcardDeckEntity>

    @Query("SELECT * FROM flashcard_decks WHERE localId = :localId LIMIT 1")
    suspend fun getDeckById(localId: String): FlashcardDeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeck(deck: FlashcardDeckEntity)

    @Update
    suspend fun updateDeck(deck: FlashcardDeckEntity)

    @Delete
    suspend fun deleteDeckEntity(deck: FlashcardDeckEntity)

    @Query("UPDATE flashcard_decks SET isDeleted = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE localId = :localId")
    suspend fun softDeleteDeck(localId: String, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM flashcards WHERE isDeleted = 0 ORDER BY nextReviewAtEpochMs ASC")
    fun observeAllActiveFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY nextReviewAtEpochMs ASC")
    fun observeFlashcardsForDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 AND nextReviewAtEpochMs <= :nowEpochMs ORDER BY nextReviewAtEpochMs ASC")
    fun observeDueFlashcardsForDeck(deckId: String, nowEpochMs: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE isDeleted = 0")
    suspend fun getAllActiveFlashcardsOnce(): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE syncState != 'SYNCED'")
    suspend fun getDirtyFlashcards(): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE localId = :localId LIMIT 1")
    suspend fun getFlashcardById(localId: String): FlashcardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashcard(card: FlashcardEntity)

    @Update
    suspend fun updateFlashcard(card: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcardEntity(card: FlashcardEntity)

    @Query("UPDATE flashcards SET isDeleted = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE localId = :localId")
    suspend fun softDeleteFlashcard(localId: String, syncState: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM flashcard_decks WHERE syncState != 'SYNCED'")
    fun observeDirtyDeckCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE syncState != 'SYNCED'")
    fun observeDirtyCardCount(): Flow<Int>
}

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions WHERE isDeleted = 0 ORDER BY completedAtEpochMs DESC")
    fun observeSessions(): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE isDeleted = 0")
    suspend fun getAllActiveSessionsOnce(): List<PomodoroSessionEntity>

    @Query("SELECT * FROM pomodoro_sessions WHERE syncState != 'SYNCED'")
    suspend fun getDirtySessions(): List<PomodoroSessionEntity>

    @Query("SELECT * FROM pomodoro_sessions WHERE localId = :localId LIMIT 1")
    suspend fun getSessionById(localId: String): PomodoroSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: PomodoroSessionEntity)

    @Update
    suspend fun updateSession(session: PomodoroSessionEntity)

    @Delete
    suspend fun deleteSessionEntity(session: PomodoroSessionEntity)

    @Query("UPDATE pomodoro_sessions SET isDeleted = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE localId = :localId")
    suspend fun softDeleteSession(localId: String, syncState: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE syncState != 'SYNCED'")
    fun observeDirtySessionCount(): Flow<Int>
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestampEpochMs DESC LIMIT 30")
    fun observeRecentLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SyncLogEntity)

    @Query("DELETE FROM sync_logs WHERE timestampEpochMs < :cutoffEpochMs")
    suspend fun pruneOldLogs(cutoffEpochMs: Long)

    @Query("DELETE FROM sync_logs")
    suspend fun clearAllLogs()
}
