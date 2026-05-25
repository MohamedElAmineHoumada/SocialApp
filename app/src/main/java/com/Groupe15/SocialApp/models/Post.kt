package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Post(
    @get:PropertyName("id") @set:PropertyName("id") var postId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var authorUid: String = "",
    var authorUsername: String = "",
    var authorProfileUrl: String = "",
    @get:PropertyName("caption") @set:PropertyName("caption") var content: String = "",
    @get:PropertyName("imageUrls") @set:PropertyName("imageUrls") var imageUrls: List<String> = emptyList(),
    var likesCount: Int = 0,
    var commentsCount: Int = 0,
    var createdAt: Any? = null
) {
    fun getCreatedAtMillis(): Long {
        return when (val date = createdAt) {
            is Timestamp -> date.toDate().time
            is Long -> date
            else -> System.currentTimeMillis()
        }
    }

    // Keep compatibility with existing code expecting 'imageUrl'
    val imageUrl: String
        get() = imageUrls.firstOrNull() ?: ""
}