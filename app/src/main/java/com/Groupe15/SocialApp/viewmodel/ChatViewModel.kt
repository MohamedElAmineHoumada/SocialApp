package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Message
import com.Groupe15.SocialApp.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _sendStatus = MutableLiveData<SendStatus>()
    val sendStatus: LiveData<SendStatus> = _sendStatus

    private var currentChatId: String = ""
    private var currentUserId: String = ""
    private var otherUserId: String = ""

    /**
     * Initialise le chat avec les IDs des deux participants.
     * À appeler dans ChatFragment.onViewCreated().
     */
    fun initChat(currentUserId: String, otherUserId: String) {
        this.currentUserId = currentUserId
        this.otherUserId = otherUserId
        this.currentChatId = messageRepository.getChatId(currentUserId, otherUserId)
        listenToMessages()
    }

    /**
     * Écoute en temps réel les messages depuis Firestore.
     */
    private fun listenToMessages() {
        viewModelScope.launch {
            messageRepository.getMessages(currentChatId)
                .catch { e ->
                    // En cas d'erreur réseau, on conserve les derniers messages affichés
                    _sendStatus.value = SendStatus.Error(e.message ?: "Erreur de chargement")
                }
                .collect { messages ->
                    _messages.value = messages
                }
        }
    }

    /**
     * Envoie un message texte vers Firestore.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                _sendStatus.value = SendStatus.Sending
                messageRepository.sendMessage(
                    chatId = currentChatId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    text = text.trim()
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
