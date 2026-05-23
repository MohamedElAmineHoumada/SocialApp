package com.Groupe15.SocialApp.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.Groupe15.SocialApp.databinding.FragmentMessagesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MessagesViewModel by viewModels()
    private lateinit var conversationsAdapter: ConversationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupSearch()
    }

    private fun setupRecyclerView() {
        conversationsAdapter = ConversationsAdapter { conversation ->
            // navigate to chat
        }
        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversationsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.filteredConversations.observe(viewLifecycleOwner) { list ->
            conversationsAdapter.submitList(list)
            binding.tvEmptyState.visibility =
                if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupSearch() {
        binding.etSearchContacts.addTextChangedListener { text ->
            viewModel.filterConversations(text.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}