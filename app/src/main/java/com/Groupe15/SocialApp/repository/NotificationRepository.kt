package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.Notification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
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