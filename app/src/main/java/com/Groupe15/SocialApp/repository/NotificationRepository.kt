package com.Groupe15.SocialApp.repository

import android.content.Context
import com.Groupe15.SocialApp.models.Notification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {

    suspend fun createNotification(
        targetUid: String,
        type: String,
        fromUserId: String,
        fromUserName: String,
        fromUserAvatar: String,
        content: String,
        targetId: String
    ) {
        // Ne pas se notifier soi-même
        if (targetUid.isBlank() || targetUid == fromUserId) return

        // ✅ Vérifier les paramètres de l'utilisateur cible
        val prefs = context.getSharedPreferences("notification_settings_$targetUid", Context.MODE_PRIVATE)
        val pauseAll = prefs.getBoolean("pause_all", false)
        if (pauseAll) return

        val shouldNotify = when (type) {
            "like" -> prefs.getBoolean("notify_likes", true)
            "comment" -> prefs.getBoolean("notify_comments", true)
            "follow_request" -> prefs.getBoolean("notify_follow_requests", true)
            "follow_accept" -> prefs.getBoolean("notify_follow_accepted", true)
            "message" -> prefs.getBoolean("notify_messages", true)
            else -> true
        }

        if (!shouldNotify) return

        try {
            val notifRef = firestore.collection("users")
                .document(targetUid)
                .collection("notifications")
                .document()

            val notification = Notification(
                id = notifRef.id,
                type = type,
                fromUserId = fromUserId,
                fromUserName = fromUserName,
                fromUserAvatar = fromUserAvatar,
                content = content,
                timestamp = Timestamp.now(),
                isRead = false,
                targetId = targetId
            )

            notifRef.set(notification).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}