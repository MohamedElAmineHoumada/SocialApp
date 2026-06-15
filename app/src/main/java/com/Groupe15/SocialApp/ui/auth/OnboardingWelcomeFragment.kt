package com.Groupe15.SocialApp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentOnboardingWelcomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingWelcomeFragment : Fragment() {

    private var _binding: FragmentOnboardingWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingWelcome_to_dob)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
