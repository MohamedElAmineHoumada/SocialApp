package com.Groupe15.SocialApp.models


data class FollowRequest(
    val id: String,
    val name: String,
    val role: String,
    val mutualFriends: Int,
    val avatarUrl: String? = null
)