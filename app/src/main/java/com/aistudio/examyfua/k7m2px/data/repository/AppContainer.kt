package com.aistudio.examyfua.k7m2px.data.repository

import android.content.Context
import com.aistudio.examyfua.k7m2px.data.local.LocalPreferences
import com.aistudio.examyfua.k7m2px.data.local.YFUAAppDatabase
import com.aistudio.examyfua.k7m2px.data.remote.AiAssistantRepository
import com.aistudio.examyfua.k7m2px.data.remote.FirestoreService
import com.aistudio.examyfua.k7m2px.data.remote.GoogleDriveBackupClient
import com.aistudio.examyfua.k7m2px.worker.NotificationHelper
import com.aistudio.examyfua.k7m2px.worker.SyncWorker
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(appContext: Context) {
    private val context = appContext.applicationContext

    val database: YFUAAppDatabase by lazy { YFUAAppDatabase.getInstance(context) }
    val preferences: LocalPreferences by lazy { LocalPreferences(context) }
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(context) }

    val firestoreDb: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    }

    val firestoreService: FirestoreService by lazy {
        FirestoreService(firestoreDb)
    }

    val aiAssistantRepository: AiAssistantRepository by lazy {
        AiAssistantRepository()
    }

    val driveBackupClient: GoogleDriveBackupClient by lazy {
        GoogleDriveBackupClient()
    }

    val syncManager: SyncManager by lazy {
        SyncManager(
            context = context,
            database = database,
            firestoreService = firestoreService,
            preferences = preferences,
            networkMonitor = networkMonitor
        )
    }

    val repository: YfuaRepository by lazy {
        YfuaRepository(
            context = context,
            database = database,
            preferences = preferences,
            syncManager = syncManager
        )
    }

    init {
        NotificationHelper.ensureChannels(context)
        SyncWorker.ensurePeriodicSync(context)
    }
}
