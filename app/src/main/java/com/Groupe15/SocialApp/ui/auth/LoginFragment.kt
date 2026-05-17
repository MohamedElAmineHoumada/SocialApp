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

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvGoRegister = view.findViewById<TextView>(R.id.tvGoRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
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
            btnLogin.isEnabled = false
            viewModel.login(email, password)
        }

        tvGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            progressBar.isVisible = false
            btnLogin.isEnabled = true

            if (state.success) {
                findNavController().navigate(R.id.action_login_to_feed)
            } else if (state.error != null) {
                Toast.makeText(requireContext(),
                    state.error, Toast.LENGTH_LONG).show()
            }
        })
    }
}