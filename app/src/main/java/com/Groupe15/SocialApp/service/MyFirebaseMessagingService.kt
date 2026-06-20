package com.Groupe15.SocialApp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.Groupe15.SocialApp.MainActivity
import com.Groupe15.SocialApp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val title = remoteMessage.notification?.title ?: "Nouveau message"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val otherUserId = remoteMessage.data["otherUserId"] ?: ""
        val userName = remoteMessage.data["userName"] ?: "Utilisateur"

        showNotification(title, body, otherUserId, userName)
    }

    private fun showNotification(title: String, body: String, otherUserId: String, userName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "messages_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("otherUserId", otherUserId)
            putExtra("userName", userName)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // RemoteInput for Reply
        val KEY_TEXT_REPLY = "key_text_reply"
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Répondre...")
            .build()

        val replyIntent = Intent(this, ReplyReceiver::class.java).apply {
            putExtra("otherUserId", otherUserId)
        }
        
        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_comment,
            "Répondre",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_comment)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(replyAction)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Note: Idéalement, on injecte AuthRepository ici via Hilt ou on utilise FirebaseFirestore directement
        // Pour rester simple et efficace :
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("fcmToken", token)
        }
    }
}
