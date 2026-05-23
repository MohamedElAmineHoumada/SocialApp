package com.Groupe15.SocialApp.ui.post

import android.net.Uri
import com.Groupe15.SocialApp.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    /** Upload les images dans Storage puis crée le document dans Firestore */
    suspend fun createPost(caption: String, imageUris: List<Uri>): Post {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Utilisateur non connecté")

        // 1. Upload chaque image
        val imageUrls = imageUris.map { uri ->
            val ref = storage.reference
                .child("posts/$uid/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

        // 2. Créer le document Post
        val postId = UUID.randomUUID().toString()
        val post = hashMapOf(
            "id"          to postId,
            "userId"      to uid,
            "caption"     to caption,
            "imageUrls"   to imageUrls,
            "likesCount"  to 0,
            "commentsCount" to 0,
            "createdAt"   to com.google.firebase.Timestamp.now()
        )
        firestore.collection("posts").document(postId).set(post).await()

        return Post(
            id           = postId,
            userId       = uid,
            caption      = caption,
            imageUrls    = imageUrls,
            likesCount   = 0,
            commentsCount = 0
        )
    }
}