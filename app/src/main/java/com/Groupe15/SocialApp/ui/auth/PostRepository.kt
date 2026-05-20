package com.Groupe15.SocialApp.ui.auth

import com.Groupe15.SocialApp.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getLivePostsFlux(): Flow<List<Post>> = callbackFlow {
        val listenerRegistration = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val postList = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(postList)
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun toggleLike(postId: String) {
        // Logique de like
    }
}
