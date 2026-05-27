package com.Groupe15.SocialApp.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommentsBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: FeedViewModel by activityViewModels()
    private lateinit var adapter: CommentAdapter
    private var postId: String = ""

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: String): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString(ARG_POST_ID) ?: ""
        viewModel.selectPost(postId)  // déclenche le Flow des comments
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_comments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvComments)
        val etComment    = view.findViewById<EditText>(R.id.etComment)
        val btnSend      = view.findViewById<ImageButton>(R.id.btnSend)

        adapter = CommentAdapter()
        recyclerView.adapter       = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true  // scroll automatique vers le bas
        }

        // Observer les commentaires en temps réel
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            adapter.submitList(comments)
            if (comments.isNotEmpty()) {
                recyclerView.scrollToPosition(comments.size - 1)
            }
        }

        // Envoyer un commentaire
        btnSend.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Écris un commentaire", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addComment(postId, text)
            etComment.setText("")
        }
    }
}