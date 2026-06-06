package com.Groupe15.SocialApp.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.databinding.ItemChatReceivedBinding
import com.Groupe15.SocialApp.databinding.ItemChatSentBinding
import com.Groupe15.SocialApp.databinding.ItemChatSentImageBinding
import com.Groupe15.SocialApp.models.Message
import com.Groupe15.SocialApp.models.MessageType
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(private val currentUserId: String) :
    ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
        private const val TYPE_SENT_IMAGE = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            if (message.type == MessageType.IMAGE) TYPE_SENT_IMAGE else TYPE_SENT
        } else {
            TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT -> SentViewHolder(ItemChatSentBinding.inflate(inflater, parent, false))
            TYPE_SENT_IMAGE -> SentImageViewHolder(ItemChatSentImageBinding.inflate(inflater, parent, false))
            else -> ReceivedViewHolder(ItemChatReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is SentImageViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
        }
    }

    inner class SentViewHolder(private val binding: ItemChatSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessageSent.text = message.text
            binding.tvTimeSent.text = formatTimestamp(message.timestamp.toDate().time)
        }
    }

    inner class SentImageViewHolder(private val binding: ItemChatSentImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvImageCaption.text = message.text
            binding.tvTimeSentImage.text = formatTimestamp(message.timestamp.toDate().time)
            Glide.with(binding.ivSentImage)
                .load(message.imageUrl)
                .into(binding.ivSentImage)
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemChatReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessageReceived.text = message.text
            binding.tvTimeReceived.text = formatTimestamp(message.timestamp.toDate().time)
        }
    }

    private fun formatTimestamp(time: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(time)
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}
