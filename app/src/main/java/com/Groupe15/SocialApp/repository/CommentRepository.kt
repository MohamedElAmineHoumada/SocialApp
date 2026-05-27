package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.Comment  // ← models au lieu de data.model
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, text: String): Result<Unit> {
        return try {
            val uid      = auth.currentUser?.uid ?: return Result.failure(Exception("Non connecté"))
            val username = auth.currentUser?.displayName ?: "Anonyme"

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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}