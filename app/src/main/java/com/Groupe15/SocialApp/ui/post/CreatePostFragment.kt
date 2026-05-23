package com.Groupe15.SocialApp.ui.post
import com.Groupe15.SocialApp.ui.post.CreatePostViewModel
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.Groupe15.SocialApp.databinding.FragmentCreatePostBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
@AndroidEntryPoint
class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatePostViewModel by viewModels()

    private lateinit var selectedImagesAdapter: SelectedImagesAdapter

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSelectedImagesRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupSelectedImagesRecyclerView() {
        selectedImagesAdapter = SelectedImagesAdapter { uri ->
            viewModel.removeImage(uri)
        }
        binding.rvSelectedImages.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = selectedImagesAdapter
        }
    }

    private fun observeViewModel() {

        // Images sélectionnées
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedImages.collect { images ->
                selectedImagesAdapter.submitList(images)
                binding.rvSelectedImages.visibility =
                    if (images.isEmpty()) View.GONE else View.VISIBLE
                binding.layoutAddMedia.visibility =
                    if (images.size >= 4) View.GONE else View.VISIBLE
            }
        }

        // Profil utilisateur courant
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                user ?: return@collect
                binding.tvUsername.text = user.displayName
                Glide.with(this@CreatePostFragment)
                    .load(user.profileImageUrl)
                    .placeholder(com.Groupe15.SocialApp.R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivProfilePic)
            }
        }

        // isPosting → bouton + progressBar
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPosting.collect { isPosting ->
                binding.btnPost.isEnabled = !isPosting
                binding.btnPost.text = if (isPosting) "Posting…" else "Post"
                binding.progressBar.visibility =
                    if (isPosting) View.VISIBLE else View.GONE
            }
        }

        // postSuccess → retour au feed
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postSuccess.collect { success ->
                if (success) findNavController().navigateUp()
            }
        }

        // Erreur
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { msg ->
                if (!msg.isNullOrBlank())
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        // Zone "Add Media" + icône
        binding.layoutAddMedia.setOnClickListener { pickImages.launch("image/*") }
        binding.ivAddMediaIcon.setOnClickListener  { pickImages.launch("image/*") }

        // Publier
        binding.btnPost.setOnClickListener {
            val caption = binding.etCaption.text.toString().trim()
            viewModel.createPost(caption)
        }

        // Fermer / retour
        binding.ibClose.setOnClickListener { findNavController().navigateUp() }

        // Audience picker bottom sheet
        binding.chipAudience.setOnClickListener {
            AudiencePickerBottomSheet().show(childFragmentManager, AudiencePickerBottomSheet.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}