package com.Groupe15.SocialApp.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.BottomSheetAudienceBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AudiencePickerBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AudiencePickerBottomSheet"
    }

    private var _binding: BottomSheetAudienceBinding? = null
    private val binding get() = _binding!!

    var onAudienceSelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAudienceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionPublic.setOnClickListener {
            onAudienceSelected?.invoke("Public")
            dismiss()
        }
        binding.optionFriends.setOnClickListener {
            onAudienceSelected?.invoke("Friends")
            dismiss()
        }
        binding.optionOnlyMe.setOnClickListener {
            onAudienceSelected?.invoke("Only Me")
            dismiss()
        }
    }

    override fun getTheme() = R.style.BottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}