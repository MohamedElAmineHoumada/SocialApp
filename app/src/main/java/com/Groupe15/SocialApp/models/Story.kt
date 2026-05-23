package com.Groupe15.SocialApp.models

data class Story(
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String = "",
    val isCurrentUser: Boolean = false,
    val isViewed: Boolean = false
)