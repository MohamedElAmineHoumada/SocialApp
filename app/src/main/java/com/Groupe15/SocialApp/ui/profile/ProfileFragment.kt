package com.Groupe15.SocialApp.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupThemeToggle()
        // Vous pourrez ajouter ici l'observation du viewModel pour les données du profil
    }

    private fun setupThemeToggle() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Mettre à jour l'icône initiale
        updateThemeIcon(prefs.getBoolean("dark_mode", false))

        binding.ibThemeToggle.setOnClickListener {
            val isDarkMode = prefs.getBoolean("dark_mode", false)
            val newMode = !isDarkMode

            // Sauvegarder la préférence
            prefs.edit().putBoolean("dark_mode", newMode).apply()

            // Appliquer le thème
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}