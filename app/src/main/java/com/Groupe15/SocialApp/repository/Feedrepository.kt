package com.Groupe15.SocialApp.repository

import android.net.Uri
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
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
                    doc.toObject(Post::class.java)?.also { post ->
                        if (post.postId.isBlank()) post.postId = doc.id
                    }
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
                    doc.toObject(Story::class.java)?.let { story ->
                        if (story.storyId.isBlank()) story.copy(storyId = doc.id) else story
                    }
                } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createStory(mediaUri: Uri, text: String?, filter: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            val userDoc = firestore.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "User"
            val profileUrl = userDoc.getString("profileImageUrl") ?: ""

            val storageRef = FirebaseStorage.getInstance().reference
                .child("stories/$uid/${UUID.randomUUID()}.jpg")
            storageRef.putFile(mediaUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val storyId = firestore.collection("stories").document().id
            val story = Story(
                storyId = storyId,
                userId = uid,
                username = username,
                userProfileUrl = profileUrl,
                mediaUrl = downloadUrl,
                text = text,
                filter = filter,
                timestamp = Timestamp.now()
            )

            firestore.collection("stories").document(storyId).set(story).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareToStory(post: Post): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            val username = auth.currentUser?.displayName ?: "User"

            val story = hashMapOf(
                "userId" to uid,
                "username" to username,
                "userProfileUrl" to (auth.currentUser?.photoUrl?.toString() ?: ""),
                "mediaUrl" to post.imageUrl,
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

    // ✅ NOUVEAU : écoute en temps réel les IDs des posts sauvegardés de l'utilisateur courant
    fun getSavedPostIdsFlow(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid).collection("savedPosts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.id } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ✅ NOUVEAU : récupère les posts correspondant à une liste d'IDs (pour l'onglet "Saved")
    suspend fun getPostsByIds(postIds: List<String>): List<Post> {
        if (postIds.isEmpty()) return emptyList()
        val results = mutableListOf<Post>()
        postIds.chunked(30).forEach { chunk ->
            try {
                val snapshot = firestore.collection("posts")
                    .whereIn("id", chunk)
                    .get()
                    .await()
                results.addAll(
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.also { post ->
                            if (post.postId.isBlank()) post.postId = doc.id
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return results
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