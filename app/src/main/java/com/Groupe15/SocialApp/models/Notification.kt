package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val type: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserAvatar: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val targetId: String = ""
) {
    constructor() : this("")
}
