package com.Groupe15.SocialApp.repository

import android.net.Uri
import com.Groupe15.SocialApp.models.Post
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    private suspend fun getFollowingUids(currentUid: String): List<String> {
        return try {
            firestore.collection("users")
                .document(currentUid)
                .collection("following")
                .get()
                .await()
                .documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Posts des utilisateurs suivis + ses propres posts.
     * PAS d'orderBy dans la query pour éviter les index composites Firestore.
     * Le tri se fait côté client après réception.
     */
    fun getLivePosts(): Flow<List<Post>> = callbackFlow {
        val currentUid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val followingUids = getFollowingUids(currentUid)
        val authorUids = (followingUids + currentUid).distinct()

        val listeners = mutableListOf<ListenerRegistration>()
        val chunkResults = mutableMapOf<Int, List<Post>>()

        val chunks = authorUids.chunked(30)

        chunks.forEachIndexed { index, chunk ->
            val listener = firestore.collection("posts")
                .whereIn("userId", chunk)   // pas d'orderBy → pas d'index requis
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    chunkResults[index] = snapshot?.toObjects(Post::class.java) ?: emptyList()
                    // Tri côté client : du plus récent au plus ancien
                    val merged = chunkResults.values.flatten()
                        .sortedByDescending { it.getCreatedAtMillis() }
                        .take(50)
                    trySend(merged)
                }
            listeners.add(listener)
        }

        awaitClose { listeners.forEach { it.remove() } }
    }

    suspend fun createPost(caption: String, imageUris: List<Uri> = emptyList()): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Non connecté"))

            // Lire le vrai username depuis Firestore
            val userDoc = firestore.collection("users").document(uid).get().await()
            val username = userDoc.getString("username")
                ?: userDoc.getString("displayName")
                ?: auth.currentUser?.displayName
                ?: "Utilisateur"
            val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

            val imageUrls = imageUris.map { uri ->
                val ref = storage.reference.child("posts/$uid/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            }

            val postId = firestore.collection("posts").document().id
            val post = Post(
                postId           = postId,
                authorUid        = uid,
                authorUsername   = username,
                authorProfileUrl = profileImageUrl,
                content          = caption,
                imageUrls        = imageUrls,
                createdAt        = Timestamp.now()
            )
            firestore.collection("posts").document(postId).set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(uid)

        firestore.runTransaction { transaction ->
            val likeDoc = transaction.get(likeRef)
            if (likeDoc.exists()) {
                transaction.delete(likeRef)
                transaction.update(postRef, "likesCount", FieldValue.increment(-1))
            } else {
                transaction.set(likeRef, mapOf("userId" to uid))
                transaction.update(postRef, "likesCount", FieldValue.increment(1))
            }
        }.await()
    }
}