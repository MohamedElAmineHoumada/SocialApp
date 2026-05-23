package com.Groupe15.SocialApp.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.databinding.ItemConversationBinding
import com.bumptech.glide.Glide
import com.Groupe15.SocialApp.R

class ConversationsAdapter(
    private val onClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationsAdapter.ConversationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            binding.tvConversationUsername.text = conversation.username
            binding.tvLastMessage.text = conversation.lastMessage
            binding.tvTimestamp.text = conversation.timestamp

            if (conversation.profileImageUrl.isNotEmpty()) {
                Glide.with(binding.ivAvatar)
                    .load(conversation.profileImageUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivAvatar)
            }

            binding.viewOnlineIndicator.visibility =
                if (conversation.isOnline) android.view.View.VISIBLE
                else android.view.View.GONE

            binding.viewUnreadDot.visibility =
                if (conversation.hasUnread) android.view.View.VISIBLE
                else android.view.View.GONE

            binding.root.setOnClickListener { onClick(conversation) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation) =
            oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation) =
            oldItem == newItem
    }
}