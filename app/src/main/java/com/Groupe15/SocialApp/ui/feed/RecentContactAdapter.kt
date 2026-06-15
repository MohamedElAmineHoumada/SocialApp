package com.Groupe15.SocialApp.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.User

class RecentContactAdapter(
    private val onContactClick: (User) -> Unit
) : ListAdapter<User, RecentContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar = itemView.findViewById<ImageView>(R.id.ivContactAvatar)
        private val tvName = itemView.findViewById<TextView>(R.id.tvContactName)

        fun bind(user: User) {
            tvName.text = user.displayName.ifEmpty { user.username }
            ivAvatar.load(user.profileImageUrl.ifEmpty { null }) {
                crossfade(true)
                placeholder(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }
            itemView.setOnClickListener { onContactClick(user) }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}