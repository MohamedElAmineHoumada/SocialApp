package com.Groupe15.SocialApp.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.Groupe15.SocialApp.databinding.FragmentDiscoverBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupWhoToFollow()
        setupTrending()
    }

    private fun setupWhoToFollow() {
        // Pour l'instant, on laisse vide ou on mettra un adapter plus tard
        // binding.rvWhoToFollow.adapter = ...
    }

    private fun setupTrending() {
        // binding.rvTrending.adapter = ...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
