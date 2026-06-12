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

        binding.bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.forgotPasswordFragment,
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

    private fun hideBottomNav() {
        if (binding.bottomNavigationView.visibility == View.VISIBLE) {
            binding.bottomNavigationView.animate()
                .translationY(binding.bottomNavigationView.height.toFloat())
                .alpha(0f)
                .setDuration(200)
                .withEndAction { binding.bottomNavigationView.visibility = View.GONE }
        }
    }

    private fun showBottomNav() {
        if (binding.bottomNavigationView.visibility == View.GONE) {
            binding.bottomNavigationView.visibility = View.VISIBLE
            binding.bottomNavigationView.translationY = binding.bottomNavigationView.height.toFloat()
            binding.bottomNavigationView.alpha = 0f
            binding.bottomNavigationView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
        }
    }
}
