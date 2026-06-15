package com.Groupe15.SocialApp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.Groupe15.SocialApp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AppCompatDelegate

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer le thème sauvegardé avant le super.onCreate pour éviter le flash blanc
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Gestion manuelle de la navigation personnalisée
        binding.btnNavHome.setOnClickListener { navController.navigate(R.id.feedFragment) }
        binding.btnNavMessages.setOnClickListener { navController.navigate(R.id.messagesFragment) }
        binding.btnNavStudio.setOnClickListener { navController.navigate(R.id.createPostFragment) }
        binding.btnNavNetwork.setOnClickListener { navController.navigate(R.id.networkFragment) }
        binding.btnNavProfile.setOnClickListener { navController.navigate(R.id.profileFragment) }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavIcons(destination.id)
            when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.forgotPasswordFragment,
                R.id.onboardingWelcomeFragment,
                R.id.onboardingDobFragment,
                R.id.onboardingGenderFragment,
                R.id.onboardingInterestsFragment,
                R.id.chatFragment,
                R.id.editProfileFragment,
                R.id.createPostFragment -> {
                    hideBottomNav()
                }
                else -> {
                    showBottomNav()
                }
            }
        }
    }

    private fun updateNavIcons(selectedId: Int) {
        val activeColor = getColor(R.color.purple_primary)
        val inactiveColor = getColor(R.color.app_text_secondary)

        // Home
        binding.navHome.setColorFilter(if (selectedId == R.id.feedFragment) activeColor else inactiveColor)
        binding.tvNavHome.setTextColor(if (selectedId == R.id.feedFragment) activeColor else inactiveColor)

        // Messages
        binding.navMessages.setColorFilter(if (selectedId == R.id.messagesFragment) activeColor else inactiveColor)
        binding.tvNavMessages.setTextColor(if (selectedId == R.id.messagesFragment) activeColor else inactiveColor)

        // Studio
        binding.tvNavStudio.setTextColor(if (selectedId == R.id.createPostFragment) activeColor else inactiveColor)

        // Network
        binding.navNetwork.setColorFilter(if (selectedId == R.id.networkFragment) activeColor else inactiveColor)
        binding.tvNavNetwork.setTextColor(if (selectedId == R.id.networkFragment) activeColor else inactiveColor)

        // Profile
        binding.navProfile.setColorFilter(if (selectedId == R.id.profileFragment) activeColor else inactiveColor)
        binding.tvNavProfile.setTextColor(if (selectedId == R.id.profileFragment) activeColor else inactiveColor)
    }

    private fun hideBottomNav() {
        if (binding.bottomNavContainer.visibility == View.VISIBLE) {
            binding.bottomNavContainer.animate()
                .translationY(binding.bottomNavContainer.height.toFloat())
                .alpha(0f)
                .setDuration(200)
                .withEndAction { binding.bottomNavContainer.visibility = View.GONE }
        }
    }

    private fun showBottomNav() {
        if (binding.bottomNavContainer.visibility == View.GONE) {
            binding.bottomNavContainer.visibility = View.VISIBLE
            binding.bottomNavContainer.translationY = binding.bottomNavContainer.height.toFloat()
            binding.bottomNavContainer.alpha = 0f
            binding.bottomNavContainer.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
        }
    }
}
