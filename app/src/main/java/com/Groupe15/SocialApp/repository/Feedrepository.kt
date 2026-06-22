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
                        // Si storyId est vide dans le doc, on utilise l'ID du document Firestore
                        if (story.storyId.isBlank()) story.copy(storyId = doc.id) else story
                    }
                } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getStoriesByUserId(userId: String): List<Story> {
        return try {
            val snapshot = firestore.collection("stories")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Story::class.java)?.let { story ->
                    if (story.storyId.isBlank()) story.copy(storyId = doc.id) else story
                }
            }.sortedByDescending { it.timestamp?.seconds ?: 0L }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createStory(mediaUri: Uri, text: String?, filter: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            // ✅ Lecture Firestore — source de vérité pour username et photo
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

            // ✅ FIX BUG PHOTO : lecture Firestore au lieu de auth.currentUser?.photoUrl
            val userDoc = firestore.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: auth.currentUser?.displayName ?: "User"
            val profileUrl = userDoc.getString("profileImageUrl") ?: ""

            val storyId = firestore.collection("stories").document().id
            val story = hashMapOf(
                "storyId" to storyId,
                "userId" to uid,
                "username" to username,
                "userProfileUrl" to profileUrl,
                "mediaUrl" to post.imageUrl,
                "postId" to post.postId,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection("stories").document(storyId).set(story).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(postId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            val saveRef = firestore.collection("users").document(uid)
                .collection("savedPosts").document(postId)

            val doc = saveRef.get().await()
            if (doc.exists()) {
                saveRef.delete().await()
            } else {
                saveRef.set(
                    mapOf("postId" to postId, "timestamp" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    fun getPostByIdFlow(postId: String): Flow<Post?> = callbackFlow {
        val listener = firestore.collection("posts").document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val post = snapshot?.toObject(Post::class.java)?.also {
                    if (it.postId.isBlank()) it.postId = snapshot.id
                }
                trySend(post)
            }
        awaitClose { listener.remove() }
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

    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            val storyRef = firestore.collection("stories").document(storyId)
            val storyDoc = storyRef.get().await()

            if (!storyDoc.exists()) throw Exception("La story n'existe pas")

            val authorUid = storyDoc.getString("userId") ?: ""
            if (authorUid != uid) {
                throw Exception("Vous n'êtes pas l'auteur de cette story")
            }

            // Ne supprimer l'image du Storage QUE si ce n'est pas un partage de post
            val isSharedPost = storyDoc.contains("postId") && !storyDoc.getString("postId").isNullOrBlank()
            val mediaUrl = storyDoc.getString("mediaUrl")

            if (!isSharedPost && mediaUrl != null) {
                try {
                    FirebaseStorage.getInstance().getReferenceFromUrl(mediaUrl).delete().await()
                } catch (e: Exception) {
                    // Log mais continue la suppression du doc Firestore
                    e.printStackTrace()
                }
            }

            storyRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}