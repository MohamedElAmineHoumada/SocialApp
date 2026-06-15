package com.Groupe15.SocialApp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentSettingsBinding
import com.Groupe15.SocialApp.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        updateCurrentLanguageText()
        setupClickListeners()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authEvents.collect { event ->
                when (event) {
                    is AuthViewModel.AuthEvent.PasswordResetSent -> {
                        Toast.makeText(requireContext(), R.string.reset_link_sent, Toast.LENGTH_SHORT).show()
                    }
                    is AuthViewModel.AuthEvent.AccountDeleted -> {
                        Toast.makeText(requireContext(), R.string.account_deleted_success, Toast.LENGTH_SHORT).show()
                        requireActivity().finish()
                    }
                    is AuthViewModel.AuthEvent.Error -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.currentUser.collect { user ->
                user?.let {
                    if (binding.switchPrivate.isChecked != it.isPrivate) {
                        binding.switchPrivate.isChecked = it.isPrivate
                    }
                }
            }
        }
    }

    private fun updateCurrentLanguageText() {
        val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0]
        val language = currentLocale?.language ?: java.util.Locale.getDefault().language
        
        binding.tvCurrentLang.text = when (language) {
            "en" -> getString(R.string.language_english)
            "fr" -> getString(R.string.language_french)
            "es" -> getString(R.string.language_spanish)
            "ar" -> getString(R.string.language_arabic)
            "zh" -> getString(R.string.language_chinese)
            else -> getString(R.string.language_french) // Default fallback
        }
    }

    private fun setupClickListeners() {
        binding.switchPrivate.setOnCheckedChangeListener { _, isChecked ->
            authViewModel.updatePrivacyStatus(isChecked)
        }

        binding.btnResetPassword.setOnClickListener {
            val email = FirebaseAuth.getInstance().currentUser?.email
            if (email != null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reset_password)
                    .setMessage(getString(R.string.reset_password_confirm, email))
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        authViewModel.resetPassword(email)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }

        binding.btnChangeLanguage.setOnClickListener {
            showLanguageSelectionDialog()
        }

        binding.btnNotifications.setOnClickListener {
            // TODO: Implement notifications settings
        }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_warning)
                .setPositiveButton(R.string.delete) { _, _ ->
                    authViewModel.deleteAccount()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_french),
            getString(R.string.language_spanish),
            getString(R.string.language_arabic),
            getString(R.string.language_chinese)
        )
        val codes = arrayOf("en", "fr", "es", "ar", "zh")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.change_language)
            .setItems(languages) { _, which ->
                val appLocale: androidx.core.os.LocaleListCompat = androidx.core.os.LocaleListCompat.forLanguageTags(codes[which])
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                updateCurrentLanguageText()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}