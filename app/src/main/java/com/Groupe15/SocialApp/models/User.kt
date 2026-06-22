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
    val postsCount: Int = 0,
    val isPrivate: Boolean = false,
    val role: String = "", // ex: "UI/UX Designer", "Product Manager" — affiché dans les suggestions
    val fcmToken: String = "",
    val birthDate: String = "",
    val gender: String = "",
    val interests: List<String> = emptyList(),
    val isOnboardingCompleted: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
) {
    // Constructeur vide requis par Firestore
    constructor() : this("")
}