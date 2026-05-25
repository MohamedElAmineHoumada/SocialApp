package com.Groupe15.SocialApp.ui.feed

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: PostAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val fabCreatePost = view.findViewById<FloatingActionButton>(R.id.fabCreatePost)

        adapter = PostAdapter(
            onLike = { post -> viewModel.toggleLike(post.postId) },
            onComment = { },
            onProfile = { }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            tvEmpty.isVisible = posts.isEmpty()
        }
    }
}