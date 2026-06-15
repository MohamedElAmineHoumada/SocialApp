package com.Groupe15.SocialApp.repository

import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    val currentUserId get() = auth.currentUser?.uid

    fun getFeedPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    fun getStories(): Flow<List<Story>> = callbackFlow {
        val listener = firestore.collection("stories")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Story::class.java)
                } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    suspend fun shareToStory(post: Post): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            val username = auth.currentUser?.displayName ?: "User"
            
            val story = hashMapOf(
                "userId" to uid,
                "username" to username,
                "userProfileUrl" to (auth.currentUser?.photoUrl?.toString() ?: ""),
                "imageUrl" to post.imageUrl,
                "postId" to post.postId,
                "timestamp" to FieldValue.serverTimestamp()
            )
            
            firestore.collection("stories").add(story).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(postId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            val saveRef = firestore.collection("users").document(uid).collection("savedPosts").document(postId)
            
            val doc = saveRef.get().await()
            if (doc.exists()) {
                saveRef.delete().await()
            } else {
                saveRef.set(mapOf("postId" to postId, "timestamp" to FieldValue.serverTimestamp())).await()
            }
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
                transaction.update(postRef, "likesCount",
                    FieldValue.increment(-1))
            } else {
                transaction.set(likeRef, mapOf("userId" to uid))
                transaction.update(postRef, "likesCount",
                    FieldValue.increment(1))
            }
        }.await()
    }
}