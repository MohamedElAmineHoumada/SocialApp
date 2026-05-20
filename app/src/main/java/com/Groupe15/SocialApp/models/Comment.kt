package com.Groupe15.SocialApp.models

data class Comment(
    val commentId:String = "",
    val userId:String = "",
    val username:String = "",
    val text:String = "",
    val timestamp:Long = 0
)