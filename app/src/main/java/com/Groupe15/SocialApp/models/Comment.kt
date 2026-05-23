package com.Groupe15.SocialApp.models

data class Comment(
    val commentId : String = "",
    val postId    : String = "",   // ← ajout
    val userId    : String = "",
    val username  : String = "",
    val text      : String = "",
    val timestamp : Long   = 0L
) {
    constructor() : this("")  // ← ajout requis par Firestore
}