package com.Groupe15.SocialApp.ui.post

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.databinding.ItemSelectedImageBinding
import com.bumptech.glide.Glide

class SelectedImagesAdapter(
    private val onRemove: (Uri) -> Unit
) : ListAdapter<Uri, SelectedImagesAdapter.ImageViewHolder>(UriDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemSelectedImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(
        private val binding: ItemSelectedImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            Glide.with(binding.ivSelectedImage)
                .load(uri)
                .centerCrop()
                .into(binding.ivSelectedImage)

            binding.ibRemoveImage.setOnClickListener { onRemove(uri) }
        }
    }

    class UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(old: Uri, new: Uri) = old == new
        override fun areContentsTheSame(old: Uri, new: Uri) = old == new
    }
}