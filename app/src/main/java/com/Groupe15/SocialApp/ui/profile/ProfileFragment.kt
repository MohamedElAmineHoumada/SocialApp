package com.Groupe15.SocialApp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import coil.load
import com.Groupe15.SocialApp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val followViewModel: FollowViewModel by viewModels()

    // L'uid du profil affiché — passé via navigation args
    private lateinit var targetUid: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
        val tvBio = view.findViewById<TextView>(R.id.tvBio)
        val tvFollowers = view.findViewById<TextView>(R.id.tvFollowersCount)
        val tvFollowing = view.findViewById<TextView>(R.id.tvFollowingCount)
        val ivProfileImage = view.findViewById<ImageView>(R.id.ivProfilePic)
        val btnFollow = view.findViewById<Button>(R.id.btnFollow)

        // Récupérer l'uid depuis les arguments de navigation
        targetUid = arguments?.getString("targetUid") ?: return

        // Vérifier si déjà suivi
        followViewModel.checkIsFollowing(targetUid)

        btnFollow.setOnClickListener {
            val currentState = followViewModel.followState.value
            if (currentState is FollowState.IsFollowing) {
                if (currentState.isFollowing) {
                    followViewModel.unfollowUser(targetUid)
                } else {
                    followViewModel.followUser(targetUid)
                }
            }
        }

        // Observer l'état
        lifecycleScope.launch {
            followViewModel.followState.collect { state ->
                when (state) {
                    is FollowState.Loading -> btnFollow.isEnabled = false

                    is FollowState.IsFollowing -> {
                        btnFollow.isEnabled = true
                        btnFollow.text = if (state.isFollowing) "Se désabonner" else "Suivre"
                    }

                    is FollowState.FollowSuccess -> {
                        btnFollow.text = "Se désabonner"
                        btnFollow.isEnabled = true
                        followViewModel.checkIsFollowing(targetUid)
                    }

                    is FollowState.UnfollowSuccess -> {
                        btnFollow.text = "Suivre"
                        btnFollow.isEnabled = true
                        followViewModel.checkIsFollowing(targetUid)
                    }

                    is FollowState.Error -> {
                        btnFollow.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }

                    else -> {}
                }
            }
        }
    }
}