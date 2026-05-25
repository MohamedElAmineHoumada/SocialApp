// app/src/main/java/com/Groupe15/SocialApp/data/repository/PostRepository.kt

package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
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
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // Écoute en temps réel les 50 derniers posts (Firestore snapshot listener)
    fun getLivePosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    // Crée un nouveau post dans Firestore
    suspend fun createPost(content: String, imageUrl: String = ""): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Non connecté"))
            val username = auth.currentUser?.displayName ?: "Anonyme"

            val post = Post(
                postId     = firestore.collection("posts").document().id,
                authorUid  = uid,
                authorUsername = username,
                content    = content,
                imageUrls  = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
                createdAt  = Timestamp.now()
            )
            firestore.collection("posts")
                .document(post.postId)
                .set(post)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sera complété au Jour 3
    suspend fun toggleLike(postId: String) {
        // TODO Jour 3 : transaction Firestore
    }
}