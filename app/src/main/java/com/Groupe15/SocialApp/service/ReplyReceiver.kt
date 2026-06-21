package com.Groupe15.SocialApp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.Groupe15.SocialApp.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence("key_text_reply")?.toString()
        val otherUserId = intent.getStringExtra("otherUserId") ?: return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (!replyText.isNullOrBlank()) {
            val chatId = messageRepository.getChatId(currentUserId, otherUserId)
            scope.launch {
                messageRepository.sendMessage(
                    chatId = chatId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    text = replyText
                )
            }
        }
    }
}
