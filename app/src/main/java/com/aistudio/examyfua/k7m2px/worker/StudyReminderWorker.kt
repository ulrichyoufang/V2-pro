package com.aistudio.examyfua.k7m2px.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class StudyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.showNotification(
            context = applicationContext,
            channelId = NotificationHelper.CHANNEL_STUDY,
            notificationId = 2026,
            title = "Session de révision quotidienne YFUA",
            body = "Gardez votre série d'étude active ! Lancez un cycle Pomodoro ou révisez vos flashcards SM-2."
        )
        return Result.success()
    }

    companion object {
        private const val UNIQUE_DAILY_REMINDER = "yfua_daily_study_reminder"

        fun scheduleDaily(context: Context, hour: Int, minute: Int) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelayMs = (next.timeInMillis - now.timeInMillis).coerceAtLeast(10_000L)
            val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_DAILY_REMINDER,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancelDaily(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_DAILY_REMINDER)
        }
    }
}
