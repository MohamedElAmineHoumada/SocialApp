package com.Groupe15.SocialApp.models

import com.google.firebase.Timestamp

enum class CallStatus {
    IDLE, RINGING, CONNECTED, ENDED, DECLINED, MISSED
}

data class Call(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatar: String = "",
    val isVideo: Boolean = false,
    val status: String = CallStatus.IDLE.name,
    val timestamp: Timestamp = Timestamp.now()
)
