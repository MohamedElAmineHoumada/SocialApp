package com.Groupe15.SocialApp.ui.messages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
class MessagesViewModel @Inject constructor() : ViewModel() {

    private val _conversations = MutableLiveData<List<Conversation>>(emptyList())
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _filteredConversations = MutableLiveData<List<Conversation>>(emptyList())
    val filteredConversations: LiveData<List<Conversation>> = _filteredConversations

    private var allConversations = listOf<Conversation>()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            // Mock data — à remplacer par Firebase
            allConversations = listOf(
                Conversation("1", "Alex Rivera", "", "Hey! The design proposal...", "2m ago", true, true),
                Conversation("2", "Jordan Smith", "", "That's amazing! Can't wait...", "1h ago", false, false),
                Conversation("3", "Elena Vance", "", "Check out this new collaboration...", "3h ago", true, true),
                Conversation("4", "Marcus Chen", "", "The meeting has been rescheduled...", "Yesterday", false, false),
                Conversation("5", "Sasha Gray", "", "I loved your latest post! Truly inspir...", "Oct 12", false, false)
            )
            _conversations.value = allConversations
            _filteredConversations.value = allConversations
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
}