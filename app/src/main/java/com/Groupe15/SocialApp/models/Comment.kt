package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Comment(
<<<<<<< HEAD
    val commentId : String = "",
    val postId    : String = "",   // ← ajout
    val userId    : String = "",
    val username  : String = "",
    val text      : String = "",
    val timestamp : Long   = 0L
) {
    constructor() : this("")  // ← ajout requis par Firestore
}
=======
    val commentId:String = "",
    val userId:String = "",
    val username:String = "",
    val text:String = "",
    val timestamp: Timestamp? = null
)
>>>>>>> 881725f13a08dec842a589b7c6c1fcd5f702283f
