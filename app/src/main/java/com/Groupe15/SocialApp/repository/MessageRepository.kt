package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.Message
import com.Groupe15.SocialApp.models.MessageType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: com.google.firebase.storage.FirebaseStorage
) {

    suspend fun uploadAudio(uri: android.net.Uri, userId: String): String {
        val ref = storage.reference.child("audio_messages/$userId/${UUID.randomUUID()}.m4a")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    /**
     * Écoute les messages d'une conversation en temps réel.
     * orderBy sur une seule collection simple → pas d'index composite requis.
     */
    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Ne pas fermer le flow sur erreur : garder les messages affichés
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id       = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            text     = doc.getString("text") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                            isRead   = doc.getBoolean("isRead") ?: false,
                            isDelivered = doc.getBoolean("isDelivered") ?: false,
                            type     = MessageType.valueOf(
                                doc.getString("type") ?: MessageType.TEXT.name
                            ),
                            audioDuration = doc.getLong("audioDuration")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        imageUrl: String = "",
        audioDuration: Int = 0
    ) {
        val messageData = hashMapOf(
            "senderId"  to senderId,
            "text"      to text,
            "imageUrl"  to imageUrl,
            "timestamp" to Timestamp.now(),
            "isRead"    to false,
            "isDelivered" to true,
            "type"      to type.name,
            "audioDuration" to audioDuration
        )

        firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageData)
            .await()

        val lastMessage = if (type == MessageType.IMAGE) "Sent an image" else text
        updateConversationSummary(chatId, senderId, receiverId, lastMessage)

        // Send Push Notification
        sendPushNotification(receiverId, "Nouveau message", lastMessage, senderId)
    }

    private suspend fun sendPushNotification(receiverId: String, title: String, body: String, senderId: String) {
        try {
            val userDoc = firestore.collection("users").document(receiverId).get().await()
            val fcmToken = userDoc.getString("fcmToken")
            
            if (!fcmToken.isNullOrBlank()) {
                // Ici, normalement on appelle une Cloud Function ou un backend.
                // Pour l'exemple, on crée un document dans une collection "outgoing_notifications"
                // qu'une Cloud Function pourrait écouter.
                val pushData = hashMapOf(
                    "to" to fcmToken,
                    "title" to title,
                    "body" to body,
                    "data" to mapOf(
                        "otherUserId" to senderId,
                        "type" to "message"
                    ),
                    "timestamp" to Timestamp.now()
                )
                firestore.collection("outgoing_notifications").add(pushData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markLastMessageAsRead(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .update("isLastMessageRead", true)
            .await()
    }

    suspend fun markAllMessagesAsRead(chatId: String, currentUserId: String) {
        try {
            val unreadMessages = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereNotEqualTo("senderId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (unreadMessages.isEmpty) return

            val batch = firestore.batch()
            for (doc in unreadMessages.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            
            // Also update conversation summary
            firestore.collection("chats").document(chatId)
                .update("isLastMessageRead", true)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteChat(chatId: String) {
        // Note: This only deletes the conversation summary. 
        // In a production app, you'd use a Cloud Function to clean up the 'messages' sub-collection.
        firestore.collection("chats").document(chatId).delete().await()
    }

    private suspend fun updateConversationSummary(
        chatId: String,
        senderId: String,
        receiverId: String,
        lastMessageText: String
    ) {
        val summaryData = hashMapOf(
            "participants"           to listOf(senderId, receiverId),
            "lastMessage"            to lastMessageText,
            "lastMessageTimestamp"   to Timestamp.now(),
            "lastSenderId"           to senderId,
            "isLastMessageRead"      to false
        )
        firestore.collection("chats").document(chatId).set(summaryData).await()
    }

    /**
     * Liste les conversations de l'utilisateur.
     * PAS d'orderBy combiné avec whereArrayContains pour éviter l'index composite.
     * Le tri se fait côté client.
     */
    fun getConversations(userId: String): Flow<List<ConversationSummary>> = callbackFlow {
        val listener = firestore
            .collection("chats")
            .whereArrayContains("participants", userId)
            // Pas d'orderBy ici → pas d'index composite requis
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val summaries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val participants = doc.get("participants") as? List<*>
                        val otherUserId = participants
                            ?.firstOrNull { it != userId } as? String
                            ?: return@mapNotNull null
                        ConversationSummary(
                            chatId               = doc.id,
                            otherUserId          = otherUserId,
                            lastMessage          = doc.getString("lastMessage") ?: "",
                            lastMessageTimestamp = doc.getTimestamp("lastMessageTimestamp")
                                ?: Timestamp.now(),
                            lastSenderId         = doc.getString("lastSenderId") ?: "",
                            isLastMessageRead    = doc.getBoolean("isLastMessageRead") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                // Tri côté client : du plus récent au plus ancien
                val sorted = summaries.sortedByDescending {
                    it.lastMessageTimestamp.toDate().time
                }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }
}

data class ConversationSummary(
    val chatId: String,
    val otherUserId: String,
    val lastMessage: String,
    val lastMessageTimestamp: Timestamp,
    val lastSenderId: String,
    val isLastMessageRead: Boolean
)