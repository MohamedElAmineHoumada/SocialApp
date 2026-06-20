package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val type: String = "", // "follow_request", "follow_accept", "like", "comment", etc.
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserAvatar: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val targetId: String = "" // postId or userId depending on type
) {
    constructor() : this("")
}
