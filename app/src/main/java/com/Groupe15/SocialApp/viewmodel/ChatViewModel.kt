package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Message
import com.Groupe15.SocialApp.models.MessageType
import com.Groupe15.SocialApp.repository.MessageRepository
import com.Groupe15.SocialApp.util.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: com.Groupe15.SocialApp.repository.AuthRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _otherUser = MutableLiveData<com.Groupe15.SocialApp.models.User?>()
    val otherUser: LiveData<com.Groupe15.SocialApp.models.User?> = _otherUser

    private val _sendStatus = MutableLiveData<SendStatus>()
    val sendStatus: LiveData<SendStatus> = _sendStatus

    private var currentChatId: String = ""
    private var currentUserId: String = ""
    private var otherUserId: String = ""

    private var audioRecorder: AudioRecorder? = null
    private var audioFile: File? = null

    fun setAudioRecorder(recorder: AudioRecorder) {
        this.audioRecorder = recorder
    }

    fun startRecording(cacheDir: File): Boolean {
        audioFile = File(cacheDir, "temp_audio_${System.currentTimeMillis()}.m4a")
        return audioFile?.let { audioRecorder?.start(it) } ?: false
    }

    fun stopRecording(onComplete: (File?) -> Unit) {
        audioRecorder?.stop()
        onComplete(audioFile)
    }

    fun sendVoiceMessage(file: File) {
        viewModelScope.launch {
            try {
                _sendStatus.value = SendStatus.Sending
                val url = messageRepository.uploadAudio(android.net.Uri.fromFile(file), currentUserId)
                sendMessage("Voice message", type = MessageType.VOICE, imageUrl = url)
                _sendStatus.value = SendStatus.Success
            } catch (e: Exception) {
                _sendStatus.value = SendStatus.Error(e.message ?: "Failed to upload audio")
            }
        }
    }

    /**
     * Initialise le chat avec les IDs des deux participants.
     */
    fun initChat(currentUserId: String, otherUserId: String) {
        this.currentUserId = currentUserId
        this.otherUserId = otherUserId
        this.currentChatId = messageRepository.getChatId(currentUserId, otherUserId)
        listenToMessages()
        listenToOtherUser()
    }

    private fun listenToOtherUser() {
        viewModelScope.launch {
            authRepository.getUserById(otherUserId).collect { user ->
                _otherUser.value = user
            }
        }
    }

    /**
     * Écoute en temps réel les messages depuis Firestore.
     */
    private fun listenToMessages() {
        viewModelScope.launch {
            messageRepository.getMessages(currentChatId)
                .catch { e ->
                    _sendStatus.value = SendStatus.Error(e.message ?: "Erreur de chargement")
                }
                .collect { messages ->
                    _messages.value = messages
                    markAllAsRead()
                }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            messageRepository.markAllMessagesAsRead(currentChatId, currentUserId)
        }
    }

    /**
     * Envoie un message texte ou image vers Firestore.
     */
    fun sendMessage(
        text: String, 
        type: MessageType = MessageType.TEXT, 
        imageUrl: String = "",
        audioDuration: Int = 0
    ) {
        if (text.isBlank() && type == MessageType.TEXT) return
        viewModelScope.launch {
            try {
                _sendStatus.value = SendStatus.Sending
                messageRepository.sendMessage(
                    chatId = currentChatId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    text = text.trim(),
                    type = type,
                    imageUrl = imageUrl,
                    audioDuration = audioDuration
                )
                _sendStatus.value = SendStatus.Success
            } catch (e: Exception) {
                _sendStatus.value = SendStatus.Error(e.message ?: "Erreur d'envoi")
            }
        }
    }
}

sealed class SendStatus {
    object Sending : SendStatus()
    object Success : SendStatus()
    data class Error(val message: String) : SendStatus()
}
