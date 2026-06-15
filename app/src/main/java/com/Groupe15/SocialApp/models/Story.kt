package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Story(
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String = "",
    val imageUrl: String = "",
    val postId: String = "",
    val timestamp: Timestamp? = null,
    val isCurrentUser: Boolean = false,
    val isViewed: Boolean = false
)