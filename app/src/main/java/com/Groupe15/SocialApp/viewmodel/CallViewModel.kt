package com.Groupe15.SocialApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Groupe15.SocialApp.models.Call
import com.Groupe15.SocialApp.models.CallStatus
import com.Groupe15.SocialApp.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {

    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    private val _incomingCall = MutableStateFlow<Call?>(null)
    val incomingCall: StateFlow<Call?> = _incomingCall.asStateFlow()

    fun observeIncomingCalls(userId: String) {
        viewModelScope.launch {
            callRepository.observeIncomingCalls(userId).collect { call ->
                _incomingCall.value = call
            }
        }
    }

    fun startCall(
        callerId: String,
        callerName: String,
        callerAvatar: String,
        receiverId: String,
        receiverName: String,
        receiverAvatar: String,
        isVideo: Boolean
    ) {
        viewModelScope.launch {
            val call = Call(
                callerId = callerId,
                callerName = callerName,
                callerAvatar = callerAvatar,
                receiverId = receiverId,
                receiverName = receiverName,
                receiverAvatar = receiverAvatar,
                isVideo = isVideo,
                status = CallStatus.RINGING.name
            )
            val callId = callRepository.startCall(call)
            observeCall(callId)
        }
    }

    fun observeCall(callId: String) {
        viewModelScope.launch {
            callRepository.observeCall(callId).collect { call ->
                _currentCall.value = call
            }
        }
    }

    fun acceptCall() {
        val call = _incomingCall.value ?: return
        viewModelScope.launch {
            callRepository.acceptCall(call.callId)
            _currentCall.value = call.copy(status = CallStatus.CONNECTED.name)
            _incomingCall.value = null
        }
    }

    fun declineCall() {
        val call = _incomingCall.value ?: return
        viewModelScope.launch {
            callRepository.declineCall(call.callId)
            _incomingCall.value = null
        }
    }

    fun endCall() {
        val call = _currentCall.value ?: return
        viewModelScope.launch {
            callRepository.endCall(call.callId)
            _currentCall.value = null
        }
    }
    
    fun clearCurrentCall() {
        _currentCall.value = null
    }

    fun clearIncomingCall() {
        _incomingCall.value = null
    }

    private val _callHistory = MutableStateFlow<List<Call>>(emptyList())
    val callHistory: StateFlow<List<Call>> = _callHistory.asStateFlow()

    fun loadCallHistory(userId: String) {
        viewModelScope.launch {
            callRepository.getCallHistory(userId).collect { history ->
                _callHistory.value = history
            }
        }
    }
}
