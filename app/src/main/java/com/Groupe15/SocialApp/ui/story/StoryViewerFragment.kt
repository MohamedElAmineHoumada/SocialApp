package com.Groupe15.SocialApp.ui.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.ui.feed.StoryViewerScreen
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoryViewerFragment : Fragment() {

    private val viewModel: FeedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val initialIndex = arguments?.getInt("initialIndex") ?: 0

        return ComposeView(requireContext()).apply {
            setContent {

                val stories by viewModel.stories.observeAsState(initial = emptyList())

                StoryViewerScreen(
                    stories = stories,
                    initialIndex = initialIndex,
                    onClose = { findNavController().navigateUp() }
                )
            }
        }
    }
}