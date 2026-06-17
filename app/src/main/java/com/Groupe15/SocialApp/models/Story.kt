package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String = "",
    val mediaUrl: String = "",
    val type: String = "image", // "image" ou "video"
    val text: String? = null,
    val filter: String = "Original",
    val timestamp: Timestamp? = null,
    val isViewed: Boolean = false
) {
    val isCurrentUser: Boolean
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == userId
}