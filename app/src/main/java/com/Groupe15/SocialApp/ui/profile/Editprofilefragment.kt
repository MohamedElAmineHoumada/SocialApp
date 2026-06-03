package com.Groupe15.SocialApp.ui.profile
import androidx.navigation.fragment.findNavController
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.databinding.FragmentEditProfileBinding
import com.bumptech.glide.Glide
import com.Groupe15.SocialApp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setNewProfileImage(it)
            binding.ivProfilePic.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                user ?: return@collect
                binding.etDisplayName.setText(user.displayName)
                binding.etUsername.setText(user.username)
                binding.etBio.setText(user.bio)
                binding.etWebsite.setText(user.website)

                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this@EditProfileFragment)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(binding.ivProfilePic)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSaving.collect { isSaving ->
                binding.btnSave.isEnabled = !isSaving
                binding.btnSave.text = if (isSaving) "Saving..." else "Save"
                binding.progressBar.visibility =
                    if (isSaving) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveSuccess.collect { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { msg ->
                if (!msg.isNullOrBlank())
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        // Retour
        binding.ibClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Changer photo de profil
        binding.ivProfilePic.setOnClickListener {
            pickImage.launch("image/*")
        }
        binding.tvChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Sauvegarder
        binding.btnSave.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()
            val website = binding.etWebsite.text.toString().trim()
            viewModel.saveProfile(displayName, username, bio, website)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}