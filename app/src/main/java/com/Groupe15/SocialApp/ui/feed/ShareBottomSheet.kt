package com.Groupe15.SocialApp.ui.feed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: FeedViewModel by activityViewModels()
    private lateinit var adapter: RecentContactAdapter
    private var postId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString("post_id") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_share, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        val rvContacts = view.findViewById<RecyclerView>(R.id.rvRecentContacts)
        adapter = RecentContactAdapter { user ->
            Toast.makeText(requireContext(), "Partagé avec ${user.displayName}", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        
        viewModel.recentContacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submitList(contacts)
        }
        
        setupActions(view)
    }

    private fun setupActions(view: View) {
        view.findViewById<View>(R.id.btnShareStory).setOnClickListener {
            val post = viewModel.posts.value?.find { it.postId == postId }
            if (post != null) {
                viewModel.shareToStory(post)
                Toast.makeText(requireContext(), "Ajouté à votre story", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Erreur : Post introuvable", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        view.findViewById<View>(R.id.btnSendMessage).setOnClickListener {
            Toast.makeText(requireContext(), "Ouverture des messages", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        
        // Actions du bas
        view.findViewById<View>(R.id.btnCopyLink)?.setOnClickListener {
            copyLinkToClipboard()
            dismiss()
        }

        view.findViewById<View>(R.id.btnSave)?.setOnClickListener {
            viewModel.toggleSavePost(postId)
            Toast.makeText(requireContext(), "Post enregistré", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun copyLinkToClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Post Link", "https://socialapp.com/post/$postId")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Lien copié dans le presse-papier", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(postId: String): ShareBottomSheet {
            return ShareBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("post_id", postId)
                }
            }
        }
    }
}