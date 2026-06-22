package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Message
import com.Groupe15.SocialApp.models.MessageType
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.repository.AuthRepository
import com.Groupe15.SocialApp.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    val currentUser: LiveData<User?> = authRepository.getCurrentUser().asLiveData()

    private val _otherUser = MutableLiveData<User?>(null)
    val otherUser: LiveData<User?> = _otherUser

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
        fetchOtherUser(otherUserId)
        listenToMessages()
    }

    private fun fetchOtherUser(userId: String) {
        viewModelScope.launch {
            authRepository.getUserById(userId).collect { user ->
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
                    // En cas d'erreur réseau, on conserve les derniers messages affichés
                    _sendStatus.value = SendStatus.Error(e.message ?: "Erreur de chargement")
                }
                .collect { messages ->
                    _messages.value = messages
                    markAsRead()
                }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            try {
                messageRepository.markLastMessageAsRead(currentChatId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Envoie un message texte ou image vers Firestore.
     */
    fun sendMessage(text: String, type: MessageType = MessageType.TEXT, imageUrl: String = "") {
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
                    imageUrl = imageUrl
                )
                _sendStatus.value = SendStatus.Success
            } catch (e: Exception) {
                _sendStatus.value = SendStatus.Error(e.message ?: "Erreur d'envoi")
            }
        }
    }

    fun deleteChat(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                messageRepository.deleteChat(currentChatId)
                onDeleted()
            } catch (e: Exception) {
                _sendStatus.value = SendStatus.Error("Erreur lors de la suppression")
            }
        }
    }
}

sealed class SendStatus {
    object Sending : SendStatus()
    object Success : SendStatus()
    data class Error(val message: String) : SendStatus()
}
