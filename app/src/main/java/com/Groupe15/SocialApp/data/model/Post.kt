package com.Groupe15.SocialApp.data.model

data class Post(
    val postId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorProfileUrl: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0
) {
    constructor() : this("")
}