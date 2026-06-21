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
    private val auth: FirebaseAuth
) {

    /**
     * IMPORTANT : pas d'orderBy() dans la query Firestore.
     * Un orderBy() sur un champ peut nécessiter un index composite côté Firestore
     * (selon les règles/autres requêtes du projet) ; si l'index est absent, le
     * listener reçoit une erreur FAILED_PRECONDITION et ne renvoie JAMAIS de
     * données — sans crash visible, juste une liste vide en permanence.
     * On trie donc côté client après réception, ce qui est sans risque.
     */
    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        if (postId.isBlank()) {
            android.util.Log.w("CommentsDebug", "postId vide, abandon")
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
                    android.util.Log.e("CommentsDebug", "Erreur listener comments postId=$postId : ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.let { comment ->
                        if (comment.commentId.isBlank()) comment.copy(commentId = doc.id) else comment
                    }
                } ?: emptyList()

                android.util.Log.d(
                    "CommentsDebug",
                    "postId=$postId reçu ${comments.size} commentaires bruts: ${snapshot?.documents?.size}"
                )

                val sorted = comments.sortedBy { it.timestamp }
                trySend(sorted)
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

            android.util.Log.d("CommentsDebug", "Ajout commentaire postId=$postId commentId=${commentRef.id} text=$text")

            val postRef = firestore.collection("posts").document(postId)

            firestore.runBatch { batch ->
                batch.set(commentRef, comment)
                batch.update(postRef, "commentsCount", FieldValue.increment(1))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CommentsDebug", "Erreur addComment: ${e.message}", e)
            Result.failure(e)
        }
    }
}