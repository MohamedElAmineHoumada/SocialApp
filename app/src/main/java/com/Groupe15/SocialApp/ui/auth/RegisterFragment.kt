package com.Groupe15.SocialApp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val viewModel: RegisterViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvGoLogin = view.findViewById<TextView>(R.id.tvGoLogin)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Remplis tous les champs !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(),
                    "Mot de passe trop court !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.isVisible = true
            btnRegister.isEnabled = false
            viewModel.register(email, password, username)
        }

        tvGoLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            progressBar.isVisible = false
            btnRegister.isEnabled = true

            if (state.success) {
                findNavController().navigate(R.id.action_register_to_feed)
            } else if (state.error != null) {
                Toast.makeText(requireContext(),
                    state.error, Toast.LENGTH_LONG).show()
            }
        })
    }
}