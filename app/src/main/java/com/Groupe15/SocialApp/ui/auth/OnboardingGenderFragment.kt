package com.Groupe15.SocialApp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentOnboardingGenderBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingGenderFragment : Fragment() {

    private var _binding: FragmentOnboardingGenderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingGenderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cvFemale.setOnClickListener {
            selectGender(true)
        }

        binding.cvMale.setOnClickListener {
            selectGender(false)
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingGender_to_interests)
        }
    }

    private fun selectGender(isFemale: Boolean) {
        binding.cvFemale.setCardBackgroundColor(if (isFemale) resources.getColor(R.color.purple_primary, null) else resources.getColor(R.color.app_card, null))
        binding.cvMale.setCardBackgroundColor(if (!isFemale) resources.getColor(R.color.purple_primary, null) else resources.getColor(R.color.app_card, null))
        binding.btnContinue.setBackgroundColor(resources.getColor(R.color.purple_primary, null))
        binding.btnContinue.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
