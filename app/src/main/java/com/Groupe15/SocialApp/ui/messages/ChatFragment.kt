package com.Groupe15.SocialApp.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentChatBinding
import com.Groupe15.SocialApp.viewmodel.ChatViewModel
import com.Groupe15.SocialApp.viewmodel.SendStatus
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    // ViewModel Hilt — survivra aux rotations d'écran
    private val viewModel: ChatViewModel by viewModels()

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
        setupInputBar()
        initViewModel()
        observeViewModel()

        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Initialise le ViewModel avec les IDs des deux participants.
     * L'argument "chatId" contient l'UID de l'autre utilisateur.
     */
    private fun initViewModel() {
        val otherUserId = arguments?.getString("chatId") ?: return
        // initChat est idempotent : appeler plusieurs fois n'ouvre pas plusieurs listeners
        viewModel.initChat(currentUserId, otherUserId)
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

    private fun setupInputBar() {
        // Basculer l'icône micro / envoyer selon le contenu du champ
        binding.etMessage.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) {
                binding.btnSend.setImageResource(R.drawable.ic_send)
            } else {
                binding.btnSend.setImageResource(R.drawable.ic_mic)
            }
        }

        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                // Déléguer l'envoi au ViewModel (qui écrit dans Firestore)
                viewModel.sendMessage(messageText)
                binding.etMessage.text.clear()
            } else {
                Toast.makeText(
                    context,
                    "Maintenez pour enregistrer un message audio",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.ivAdd.setOnClickListener {
            Toast.makeText(context, "Ouverture de la galerie photo...", Toast.LENGTH_SHORT).show()
        }

        binding.ivEmoji.setOnClickListener {
            Toast.makeText(context, "Affichage du clavier Emoji", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Mettre à jour la liste des messages en temps réel
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                // Scroller vers le dernier message après la mise à jour de la liste
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }

        // Gérer les erreurs d'envoi
        viewModel.sendStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is SendStatus.Error -> Toast.makeText(
                    context,
                    "Erreur : ${status.message}",
                    Toast.LENGTH_SHORT
                ).show()
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
