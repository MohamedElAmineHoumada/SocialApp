package com.Groupe15.SocialApp.models


data class SuggestionUser(
    val id: String,
    val name: String,
    val role: String,
    val mutualFriends: Int,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false
)