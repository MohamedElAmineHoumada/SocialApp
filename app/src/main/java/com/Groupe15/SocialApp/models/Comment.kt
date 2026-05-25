package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Comment(
    val commentId:String = "",
    val userId:String = "",
    val username:String = "",
    val text:String = "",
    val timestamp: Timestamp? = null
)