package com.Groupe15.SocialApp.ui.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateStoryFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateStoryScreen(
                    onBack = { findNavController().navigateUp() },
                    onPostStory = { uri, text, filter ->
                        lifecycleScope.launch {
                            val result = viewModel.createStory(uri, text, filter)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Story publiée !", Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            } else {
                                Toast.makeText(context, "Erreur : ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
