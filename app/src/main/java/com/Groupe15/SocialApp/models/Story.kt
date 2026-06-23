package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String = "",
    val mediaUrl: String = "",
    @get:PropertyName("imageUrl") val imageUrl: String = "",
    val type: String = "image",
    val text: String? = null,
    val filter: String = "Original",
    val timestamp: Timestamp? = null,
    val isViewed: Boolean = false,
    val postId: String? = null
) {
    @get:Exclude
    val isCurrentUser: Boolean
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == userId

    @get:Exclude
    val displayMediaUrl: String
        get() = if (mediaUrl.isNotBlank()) mediaUrl else imageUrl
}