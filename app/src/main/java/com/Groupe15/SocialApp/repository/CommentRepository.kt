package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationRepository: NotificationRepository // ✅ NOUVEAU
) {

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        if (postId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore
            .collection("posts")
            .document(postId)
            .collection("comments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.let { comment ->
                        if (comment.commentId.isBlank()) comment.copy(commentId = doc.id) else comment
                    }
                } ?: emptyList()

                val sorted = comments.sortedBy { it.timestamp }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, text: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Non connecté"))

            val userDoc = firestore.collection("users").document(uid).get().await()
            val username = userDoc.getString("username")
                ?: userDoc.getString("displayName")
                ?: auth.currentUser?.displayName
                ?: "Anonyme"
            val avatar = userDoc.getString("profileImageUrl") ?: ""

            val commentRef = firestore
                .collection("posts")
                .document(postId)
                .collection("comments")
                .document()

            val comment = Comment(
                commentId = commentRef.id,
                postId    = postId,
                userId    = uid,
                username  = username,
                text      = text,
                timestamp = System.currentTimeMillis()
            )

            val postRef = firestore.collection("posts").document(postId)

            firestore.runBatch { batch ->
                batch.set(commentRef, comment)
                batch.update(postRef, "commentsCount", FieldValue.increment(1))
            }.await()

            //  notification à l'auteur du post
            try {
                val postDoc = postRef.get().await()
                val authorUid = postDoc.getString("userId") ?: ""
                if (authorUid.isNotBlank() && authorUid != uid) {
                    notificationRepository.createNotification(
                        targetUid = authorUid,
                        type = "comment",
                        fromUserId = uid,
                        fromUserName = username,
                        fromUserAvatar = avatar,
                        content = "$username a commenté votre publication",
                        targetId = postId
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}