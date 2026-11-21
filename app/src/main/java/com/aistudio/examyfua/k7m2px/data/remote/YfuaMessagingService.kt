package com.aistudio.examyfua.k7m2px.data.remote

import com.aistudio.examyfua.k7m2px.data.local.LocalPreferences
import com.aistudio.examyfua.k7m2px.worker.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YfuaMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = LocalPreferences(applicationContext)
        serviceScope.launch {
            prefs.setFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "YFUA Entraide"
        val body = message.notification?.body ?: message.data["body"] ?: "Vous avez reçu un nouveau rappel d'étude."
        NotificationHelper.showNotification(
            context = applicationContext,
            channelId = NotificationHelper.CHANNEL_PEER,
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            body = body
        )
    }
}
