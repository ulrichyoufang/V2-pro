package com.aistudio.examyfua.k7m2px.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudentProfileEntity::class,
        TaskEntity::class,
        OfficialScheduleEntity::class,
        InstitutionEntity::class,
        AnnouncementCacheEntity::class,
        SupportTicketEntity::class,
        FlashcardDeckEntity::class,
        FlashcardEntity::class,
        PomodoroSessionEntity::class,
        SyncLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class YFUAAppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun taskDao(): TaskDao
    abstract fun officialScheduleDao(): OfficialScheduleDao
    abstract fun institutionDao(): InstitutionDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        private const val DATABASE_NAME = "yfua_offline_db"

        @Volatile
        private var INSTANCE: YFUAAppDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "student_profile", "isSuspended")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `isSuspended` INTEGER NOT NULL DEFAULT 0")
                }
                if (!hasColumn(db, "tasks", "isOfficial")) {
                    db.execSQL("ALTER TABLE `tasks` ADD COLUMN `isOfficial` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `official_schedules` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `dueDateEpochMs` INTEGER NOT NULL,
                        `priority` TEXT NOT NULL,
                        `targetInstitution` TEXT NOT NULL,
                        `targetLevel` TEXT NOT NULL,
                        `targetSpecialty` TEXT NOT NULL,
                        `roomLocation` TEXT NOT NULL,
                        `instructor` TEXT NOT NULL,
                        `isOfficial` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `institutions` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `announcements_cache` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `targetInstitution` TEXT NOT NULL,
                        `targetLevel` TEXT NOT NULL,
                        `targetSpecialty` TEXT NOT NULL,
                        `pinned` INTEGER NOT NULL,
                        `archived` INTEGER NOT NULL,
                        `isOfficial` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `support_tickets` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `studentName` TEXT NOT NULL,
                        `studentEmail` TEXT NOT NULL,
                        `institution` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `adminReply` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "student_profile", "phoneNumber")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `phoneNumber` TEXT NOT NULL DEFAULT ''")
                }
                if (!hasColumn(db, "student_profile", "photoUri")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `photoUri` TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
            }
            return false
        }

        fun getInstance(context: Context): YFUAAppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YFUAAppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
