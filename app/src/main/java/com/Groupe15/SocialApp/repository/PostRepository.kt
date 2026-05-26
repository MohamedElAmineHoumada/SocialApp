package com.Groupe15.SocialApp.repository

import android.net.Uri
import com.Groupe15.SocialApp.models.Post
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
<<<<<<< HEAD
import com.google.firebase.firestore.SetOptions
=======
import com.google.firebase.storage.FirebaseStorage
>>>>>>> 881725f13a08dec842a589b7c6c1fcd5f702283f
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

    // Écoute en temps réel les 50 derniers posts
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

    // Crée un post avec images uploadées dans Storage
    suspend fun createPost(caption: String, imageUris: List<Uri> = emptyList()): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Non connecté"))
            val username = auth.currentUser?.displayName ?: "Anonyme"

            // Upload images si présentes
            val imageUrls = imageUris.map { uri ->
                val ref = storage.reference.child("posts/$uid/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            }

            val postId = firestore.collection("posts").document().id
            val post = Post(
                postId         = postId,
                authorUid      = uid,
                authorUsername = username,
                content        = caption,
                imageUrls      = imageUrls,
                createdAt      = Timestamp.now()
            )
            firestore.collection("posts").document(postId).set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

<<<<<<< HEAD
    // Sera complété au Jour 3
    suspend fun toggleLike(postId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Non connecté"))

            val postRef = firestore.collection("posts").document(postId)
            val likeRef = postRef.collection("likes").document(uid)

            firestore.runTransaction { transaction ->
                val likeSnap = transaction.get(likeRef)
                if (likeSnap.exists()) {
                    transaction.delete(likeRef)
                    transaction.set(
                        postRef,
                        mapOf("likesCount" to FieldValue.increment(-1)),
                        SetOptions.merge()
                    )
                } else {
                    transaction.set(
                        likeRef,
                        mapOf("uid" to uid, "likedAt" to System.currentTimeMillis())
                    )
                    transaction.set(
                        postRef,
                        mapOf("likesCount" to FieldValue.increment(1)),
                        SetOptions.merge()
                    )
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
=======
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
>>>>>>> 881725f13a08dec842a589b7c6c1fcd5f702283f
    }
}