package com.aistudio.examyfua.k7m2px.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aistudio.examyfua.k7m2px.data.local.LocalPreferences
import com.aistudio.examyfua.k7m2px.data.local.YFUAAppDatabase
import com.aistudio.examyfua.k7m2px.data.remote.FirestoreService
import com.aistudio.examyfua.k7m2px.data.repository.NetworkMonitor
import com.aistudio.examyfua.k7m2px.data.repository.SyncManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (Firebase.auth.currentUser == null) {
            return Result.success()
        }
        val db = YFUAAppDatabase.getInstance(applicationContext)
        val firestoreService = FirestoreService(applicationContext)
        val prefs = LocalPreferences(applicationContext)
        val networkMonitor = NetworkMonitor(applicationContext)
        val syncManager = SyncManager(applicationContext, db, firestoreService, prefs, networkMonitor)

        val syncResult = syncManager.syncNow(reason = "Tâche d'arrière-plan WorkManager")
        networkMonitor.unregister()
        return if (syncResult.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_ONE_TIME_SYNC = "yfua_one_time_sync"
        private const val UNIQUE_PERIODIC_SYNC = "yfua_periodic_sync"

        fun enqueueOneTimeWhenConnected(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_SYNC,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun ensurePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_SYNC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
