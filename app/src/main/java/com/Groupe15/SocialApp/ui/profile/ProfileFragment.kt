package com.Groupe15.SocialApp.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateThemeIcon()
        observeViewModel()
        setupClickListeners()
        setupTabs()
    }

    private fun updateThemeIcon() {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDark = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        // If it's dark, show Sun icon to switch to light. If it's light, show Moon icon to switch to dark.
        binding.ibThemeToggle.setImageResource(
            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                user ?: return@collect
                binding.tvDisplayName.text = user.displayName.ifEmpty { user.username }
                binding.tvUsername.text = "@${user.username}"
                binding.tvBio.text = user.bio
                binding.tvWebsite.text = user.website
                binding.tvPostsCount.text = user.postsCount.toString()
                binding.tvFollowersCount.text = formatCount(user.followersCount)
                binding.tvFollowingCount.text = user.followingCount.toString()

                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(binding.ivProfilePic)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isOwnProfile.collect { isOwn ->
                binding.btnEditProfile.visibility = if (isOwn) View.VISIBLE else View.GONE
                binding.btnFollow.visibility = if (isOwn) View.GONE else View.VISIBLE
                binding.ibSettings.visibility = if (isOwn) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isFollowing.collect { isFollowing ->
                binding.btnFollow.text = if (isFollowing) "Following" else "Follow"
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnFollow.setOnClickListener {
            viewModel.toggleFollow()
        }

        binding.ibThemeToggle.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
            val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val isCurrentlyDark = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

            val newMode = if (isCurrentlyDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            
            // Save preference FIRST
            prefs.edit().putBoolean("dark_mode", newMode == AppCompatDelegate.MODE_NIGHT_YES).apply()
            
            // Apply theme switch - this will recreate the activity
            AppCompatDelegate.setDefaultNightMode(newMode)
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }
    }

    private fun setupTabs() {
        binding.tvTabPosts.setOnClickListener { switchTab(0) }
        binding.tvTabReels.setOnClickListener { switchTab(1) }
        binding.tvTabTagged.setOnClickListener { switchTab(2) }
    }

    private fun switchTab(tab: Int) {
        val indicators = listOf(
            binding.viewIndicatorPosts,
            binding.viewIndicatorReels,
            binding.viewIndicatorTagged
        )
        indicators.forEachIndexed { index, view ->
            view.visibility = if (index == tab) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}k"
        else -> count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}