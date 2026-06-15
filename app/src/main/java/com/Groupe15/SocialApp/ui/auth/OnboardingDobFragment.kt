package com.Groupe15.SocialApp.ui.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.FragmentOnboardingDobBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class OnboardingDobFragment : Fragment() {

    private var _binding: FragmentOnboardingDobBinding? = null
    private val binding get() = _binding!!

    private var selectedDay = 1
    private var selectedMonth = 6
    private var selectedYear = 2026

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingDobBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDateDisplay()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            updateDateDisplay()
        }

        val clickListener = View.OnClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(selectedYear, selectedMonth, selectedDay)
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.llDay.setOnClickListener(clickListener)
        binding.llMonth.setOnClickListener(clickListener)
        binding.llYear.setOnClickListener(clickListener)
        binding.cvPicker.setOnClickListener(clickListener)

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingDob_to_gender)
        }
    }

    private fun updateDateDisplay() {
        binding.tvDay.text = String.format("%02d", selectedDay)
        binding.tvMonth.text = String.format("%02d", selectedMonth + 1)
        binding.tvYear.text = selectedYear.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
