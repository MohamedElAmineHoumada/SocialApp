package com.Groupe15.SocialApp.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentChatBinding
import com.Groupe15.SocialApp.models.Message
import com.Groupe15.SocialApp.models.MessageType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var chatAdapter: ChatAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupHeader()
        setupRecyclerView()
        loadDummyMessages()
        
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Change l'icône du FAB en micro comme sur l'image
        binding.btnSend.setImageResource(R.drawable.ic_mic)
    }

    private fun setupHeader() {
        val userName = arguments?.getString("userName") ?: "Chat"
        binding.tvChatName.text = userName
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentUserId)
        binding.rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun loadDummyMessages() {
        val dummyMessages = listOf(
            Message(
                id = "1",
                senderId = "other_id",
                text = "That video is absolutely incredible. What setup did you use?",
                timestamp = Timestamp.now(),
                type = MessageType.TEXT
            ),
            Message(
                id = "2",
                senderId = currentUserId,
                text = "Thanks Alex! It was actually a really simple 3-point LED setup but I used some custom diffusion panels to get that soft glow.",
                timestamp = Timestamp.now(),
                type = MessageType.TEXT
            ),
            Message(
                id = "3",
                senderId = currentUserId,
                text = "Here's a quick snap of the layout I used for the shoot!",
                timestamp = Timestamp.now(),
                type = MessageType.IMAGE
            ),
            Message(
                id = "4",
                senderId = "other_id",
                text = "Wow, that's genius. I need to try those panels. Are you free to hop on a quick call later to discuss a collaboration? I have a creative concept that would fit your style perfectly.",
                timestamp = Timestamp.now(),
                type = MessageType.TEXT
            )
        )
        chatAdapter.submitList(dummyMessages)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
