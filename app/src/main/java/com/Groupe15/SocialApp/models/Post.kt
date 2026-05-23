package com.Groupe15.SocialApp.models

data class Post(
    val postId: String           = "",
    val authorUid: String        = "",
    val authorUsername: String   = "",
    val authorProfileUrl: String = "",
    val content: String          = "",
    val imageUrl: String         = "",
    val likesCount: Int          = 0,
    val commentsCount: Int       = 0,
    val createdAt: Long          = 0L,
    val location: String?        = null,
    val isLikedByCurrentUser: Boolean = false
) {
    // Constructeur vide requis par Firestore
    constructor() : this("")

    // Aliases pour l'UI
    val id: String get() = postId
    val userId: String get() = authorUid
    val username: String get() = authorUsername
    val userProfileUrl: String get() = authorProfileUrl
    val caption: String get() = content
    val imageUrls: List<String> get() = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList()
}