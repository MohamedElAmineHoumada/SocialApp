package com.Groupe15.SocialApp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentOnboardingInterestsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingInterestsFragment : Fragment() {

    private var _binding: FragmentOnboardingInterestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var interestsAdapter: InterestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingInterestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFinish.setOnClickListener {
            // Logique pour terminer l'onboarding (ex: sauvegarder dans Firestore)
            // findNavController().navigate(R.id.action_global_discoverFragment)
            // Pour l'instant on retourne au login ou home selon le flow
            activity?.finish()
        }

        updateFinishButton(0)
    }

    private fun setupRecyclerView() {
        interestsAdapter = InterestsAdapter { count ->
            updateFinishButton(count)
        }

        binding.rvInterests.apply {
            adapter = interestsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }

        interestsAdapter.setInterests(getMockInterests())
    }

    private fun updateFinishButton(count: Int) {
        binding.btnFinish.isEnabled = count >= 3
        binding.btnFinish.alpha = if (count >= 3) 1.0f else 0.5f
        binding.btnFinish.text = if (count >= 3) "Terminer" else "Choisir encore ${3 - count}"
    }

    private fun getMockInterests(): List<Interest> {
        return listOf(
            Interest("1", "Musique", R.drawable.ic_mic),
            Interest("2", "Photographie", R.drawable.ic_image_add),
            Interest("3", "Voyage", R.drawable.ic_globe),
            Interest("4", "Technologie", R.drawable.ic_settings),
            Interest("5", "Sport", R.drawable.ic_people),
            Interest("6", "Art", R.drawable.ic_grid),
            Interest("7", "Cuisine", R.drawable.ic_info),
            Interest("8", "Mode", R.drawable.ic_tag)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
