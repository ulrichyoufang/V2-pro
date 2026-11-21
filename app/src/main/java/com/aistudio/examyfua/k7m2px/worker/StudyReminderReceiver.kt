package com.aistudio.examyfua.k7m2px.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Rappel d'étude YFUA"
        val body = intent.getStringExtra("body") ?: "C'est l'heure de votre session de révision !"
        val notifId = intent.getIntExtra("notif_id", 1001)
        NotificationHelper.showNotification(
            context = context,
            channelId = NotificationHelper.CHANNEL_STUDY,
            notificationId = notifId,
            title = title,
            body = body
        )
    }
}
