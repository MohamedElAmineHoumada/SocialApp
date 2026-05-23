package com.Groupe15.SocialApp.models

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val coverImageUrl: String = "",
    val website: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0
) {
    // Constructeur vide requis par Firestore
    constructor() : this("")
}