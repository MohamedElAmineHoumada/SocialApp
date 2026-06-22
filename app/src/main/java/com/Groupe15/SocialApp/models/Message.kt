package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val type: MessageType = MessageType.TEXT,
    val audioDuration: Int = 0 // Duration in seconds for VOICE type
)

enum class MessageType {
    TEXT, IMAGE, GIF, VOICE
}
