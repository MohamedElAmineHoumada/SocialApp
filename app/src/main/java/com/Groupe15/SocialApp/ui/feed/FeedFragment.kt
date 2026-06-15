package com.Groupe15.SocialApp.ui.feed

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val viewModel: FeedViewModel by activityViewModels()
    private lateinit var adapter: PostAdapter
    private lateinit var storyAdapter: StoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val rvStories = view.findViewById<RecyclerView>(R.id.rvStories)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val ivSearch = view.findViewById<View>(R.id.ivSearch)
        val ivNotification = view.findViewById<View>(R.id.ivNotification)

        ivSearch.setOnClickListener {
            findNavController().navigate(R.id.discoverFragment)
        }

        ivNotification.setOnClickListener {
            // Action pour les notifications
        }

        // Configuration adapter Posts
        adapter = PostAdapter(
            currentUserId = viewModel.currentUserId,
            onLike    = { post -> viewModel.toggleLike(post.postId) },
            onComment = { post ->
                CommentsBottomSheet.newInstance(post.postId)
                    .show(parentFragmentManager, "comments")
            },
            onShare = { post ->
                ShareBottomSheet.newInstance(post.postId)
                    .show(parentFragmentManager, "share")
            },
            onFollow = { uid -> viewModel.followUser(uid) },
            onProfile = { authorUid ->
                val bundle = Bundle().apply {
                    putString("uid", authorUid)
                }
                findNavController().navigate(R.id.action_feed_to_profile, bundle)
            }
        )

        // Configuration adapter Stories
        storyAdapter = StoryAdapter { story ->
            // Action au clic sur une story
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Correction pour le défilement fluide dans le SwipeRefreshLayout ou NestedScrollView
        recyclerView.isNestedScrollingEnabled = true

        rvStories.adapter = storyAdapter
        val storyLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvStories.layoutManager = storyLayoutManager

        swipeRefresh.setColorSchemeResources(R.color.purple_primary)
        swipeRefresh.setOnRefreshListener {
            viewModel.loadFeed()
            swipeRefresh.isRefreshing = false
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            tvEmpty.isVisible = posts.isEmpty()
        }

        viewModel.stories.observe(viewLifecycleOwner) { stories ->
            storyAdapter.submitList(stories)
        }
    }
}