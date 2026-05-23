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
            with(binding) {
                tvStoryUsername.text = story.username

                if (story.isCurrentUser) {
                    tvStoryUsername.text = "Your Story"
                    ivAddStory.visibility = android.view.View.VISIBLE
                } else {
                    ivAddStory.visibility = android.view.View.GONE
                }

                // Gradient ring for unseen stories
                storyRing.setViewed(story.isViewed)

                Glide.with(ivStoryAvatar)
                    .load(story.userProfileUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(ivStoryAvatar)

                root.setOnClickListener { onStoryClick(story) }
            }
        }
    }

    class StoryDiffCallback : DiffUtil.ItemCallback<Story>() {
        override fun areItemsTheSame(oldItem: Story, newItem: Story) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: Story, newItem: Story) = oldItem == newItem
    }
}