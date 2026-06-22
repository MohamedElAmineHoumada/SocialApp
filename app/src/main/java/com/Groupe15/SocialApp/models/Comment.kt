package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Comment(
    val commentId      : String = "",
    val postId         : String = "",
    val userId         : String = "",
    val username       : String = "",
    val userProfileUrl : String = "",   // ← ajout : URL avatar pour CommentItem
    val text           : String = "",
    val timestamp      : Long   = 0L
) {
    constructor() : this("")  // requis par Firestore
}