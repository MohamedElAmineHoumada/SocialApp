// app/src/main/java/com/Groupe15/SocialApp/data/model/Post.kt

package com.Groupe15.SocialApp.models

data class Post(
        val postId: String         = "",
        val authorUid: String      = "",
        val authorUsername: String = "",
        val authorProfileUrl: String = "",
        val content: String        = "",
        val imageUrl: String       = "",
        val likesCount: Int        = 0,
        val commentsCount: Int     = 0,
        val createdAt: Long        = 0L   // ← ajout : timestamp millis, pour orderBy Firestore
) {
        // Constructeur vide requis par Firestore pour désérialiser les documents
        constructor() : this("")
}