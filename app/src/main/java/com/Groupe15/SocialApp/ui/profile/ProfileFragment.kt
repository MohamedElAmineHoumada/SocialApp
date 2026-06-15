package com.Groupe15.SocialApp.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()

    private lateinit var targetUid: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUid = arguments?.getString("uid") ?: ""
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        viewModel.loadProfile(
            targetUid = targetUid.ifEmpty { currentUid },
            currentUid = currentUid
        )

        if (targetUid.isNotEmpty() && targetUid != currentUid) {
            followViewModel.checkIsFollowing(targetUid)
        }

        setupThemeToggle()
        setupClickListeners()
        setupTabs()
        observeProfileViewModel()
        observeFollowViewModel()
    }

    private fun setupThemeToggle() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        updateThemeIcon(prefs.getBoolean("dark_mode", false))
        binding.ibThemeToggle.setOnClickListener {
            val isDarkMode = prefs.getBoolean("dark_mode", false)
            val newMode = !isDarkMode
            prefs.edit().putBoolean("dark_mode", newMode).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (newMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            updateThemeIcon(newMode)
        }
    }

    private fun updateThemeIcon(isDark: Boolean) {
        binding.ibThemeToggle.setImageResource(
            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )
    }

    private fun observeProfileViewModel() {
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
                binding.btnMessage.visibility = if (isOwn) View.GONE else View.VISIBLE
                binding.ibSettings.visibility = if (isOwn) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeFollowViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            followViewModel.followState.collect { state ->
                when (state) {
                    is FollowState.Loading -> binding.btnFollow.isEnabled = false
                    is FollowState.IsFollowing -> {
                        binding.btnFollow.isEnabled = true
                        if (state.isFollowing) {
                            binding.btnFollow.text = "Unfollow"

                            binding.btnFollow.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                            binding.btnFollow.setTextColor(android.graphics.Color.parseColor("#000000"))
                        } else {
                            binding.btnFollow.text = "Follow"

                            binding.btnFollow.setBackgroundColor(android.graphics.Color.parseColor("#0095F6"))
                            binding.btnFollow.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                        }
                    }
                    is FollowState.FollowSuccess -> {
                        binding.btnFollow.text = "Unfollow"
                        binding.btnFollow.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                        binding.btnFollow.setTextColor(android.graphics.Color.parseColor("#000000"))
                        binding.btnFollow.isEnabled = true
                        followViewModel.checkIsFollowing(targetUid)
                    }
                    is FollowState.UnfollowSuccess -> {
                        binding.btnFollow.text = "Follow"
                        binding.btnFollow.setBackgroundColor(android.graphics.Color.parseColor("#0095F6"))
                        binding.btnFollow.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                        binding.btnFollow.isEnabled = true
                        followViewModel.checkIsFollowing(targetUid)
                    }
                    is FollowState.Error -> {
                        binding.btnFollow.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }
        binding.btnFollow.setOnClickListener {
            val currentState = followViewModel.followState.value
            if (currentState is FollowState.IsFollowing) {
                if (currentState.isFollowing) {
                    followViewModel.unfollowUser(targetUid)
                } else {
                    followViewModel.followUser(targetUid)
                }
            }
        }
        binding.btnMessage.setOnClickListener {
            val bundle = Bundle().apply {
                putString("chatId", targetUid)
                putString("userName", binding.tvDisplayName.text.toString())
            }
            findNavController().navigate(R.id.action_profile_to_chat, bundle)
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