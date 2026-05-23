package com.Groupe15.SocialApp.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.ItemStoryBinding
import com.Groupe15.SocialApp.models.Story
import com.bumptech.glide.Glide

class StoryAdapter(
    private val onStoryClick: (Story) -> Unit
) : ListAdapter<Story, StoryAdapter.StoryViewHolder>(StoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StoryViewHolder(
        private val binding: ItemStoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(story: Story) {
            binding.tvStoryUsername.text =
                if (story.isCurrentUser) "Your Story" else story.username

            binding.ivAddStory.visibility =
                if (story.isCurrentUser) android.view.View.VISIBLE
                else android.view.View.GONE

            if (story.userProfileUrl.isNotEmpty()) {
                Glide.with(binding.ivStoryAvatar)
                    .load(story.userProfileUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivStoryAvatar)
            }

            binding.root.setOnClickListener { onStoryClick(story) }
        }
    }

    class StoryDiffCallback : DiffUtil.ItemCallback<Story>() {
        override fun areItemsTheSame(oldItem: Story, newItem: Story) =
            oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: Story, newItem: Story) =
            oldItem == newItem
    }
}