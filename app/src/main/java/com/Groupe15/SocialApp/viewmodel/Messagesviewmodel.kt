package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class Conversation(
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val lastMessage: String = "",
    val timestamp: String = "",
    val isOnline: Boolean = false,
    val hasUnread: Boolean = false
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _conversations = MutableLiveData<List<Conversation>>(emptyList())
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _filteredConversations = MutableLiveData<List<Conversation>>(emptyList())
    val filteredConversations: LiveData<List<Conversation>> = _filteredConversations

    private var allConversations = listOf<Conversation>()

    init {
        if (currentUserId.isNotEmpty()) {
            loadConversations()
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            messageRepository.getConversations(currentUserId)
                .catch { /* ignorer les erreurs réseau temporaires */ }
                .collect { summaries ->
                    // Pour chaque conversation, récupérer le profil de l'autre utilisateur
                    val conversations = summaries.mapNotNull { summary ->
                        try {
                            val userDoc = firestore
                                .collection("users")
                                .document(summary.otherUserId)
                                .get()
                                .await()

                            val username = userDoc.getString("username") ?: "Utilisateur"
                            val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

                            Conversation(
                                userId = summary.otherUserId,
                                username = username,
                                profileImageUrl = profileImageUrl,
                                lastMessage = summary.lastMessage,
                                timestamp = formatTimestamp(summary.lastMessageTimestamp.toDate().time),
                                isOnline = false,
                                hasUnread = false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    allConversations = conversations
                    _conversations.value = allConversations
                    _filteredConversations.value = allConversations
                }
        }
    }

    fun filterConversations(query: String) {
        if (query.isBlank()) {
            _filteredConversations.value = allConversations
        } else {
            _filteredConversations.value = allConversations.filter {
                it.username.contains(query, ignoreCase = true) ||
                        it.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }

    private fun formatTimestamp(time: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - time
        return when {
            diff < 60_000 -> "À l'instant"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            else -> {
                val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                sdf.format(java.util.Date(time))
            }
        }
    }
}
