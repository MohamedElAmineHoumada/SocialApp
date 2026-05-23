package com.Groupe15.SocialApp.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.Post

class PostAdapter(
    private val onLike: (Post) -> Unit,
    private val onComment: (Post) -> Unit,
    private val onProfile: (String) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivAvatar = itemView.findViewById<ImageView>(R.id.ivAvatar)
        private val tvUsername = itemView.findViewById<TextView>(R.id.tvUsername)
        private val tvContent = itemView.findViewById<TextView>(R.id.tvContent)
        private val tvTimestamp = itemView.findViewById<TextView>(R.id.tvTimestamp)
        private val tvLikesCount = itemView.findViewById<TextView>(R.id.tvLikesCount)
        private val tvCommentsCount = itemView.findViewById<TextView>(R.id.tvCommentsCount)
        private val btnLike = itemView.findViewById<ImageButton>(R.id.btnLike)
        private val btnComment = itemView.findViewById<ImageButton>(R.id.btnComment)
        private val ivPostImage = itemView.findViewById<ImageView>(R.id.ivPostImage)

        fun bind(post: Post) {
            tvUsername.text = post.authorUsername
            tvContent.text = post.content
            tvLikesCount.text = post.likesCount.toString()
            tvCommentsCount.text = post.commentsCount.toString()
            tvTimestamp.text = "à l'instant"

            // Avatar
            if (post.authorProfileUrl.isNotEmpty()) {
                ivAvatar.load(post.authorProfileUrl)
            }

            // Image du post
            if (post.imageUrl.isNotEmpty()) {
                ivPostImage.visibility = View.VISIBLE
                ivPostImage.load(post.imageUrl)
            } else {
                ivPostImage.visibility = View.GONE
            }

            // Clics
            btnLike.setOnClickListener { onLike(post) }
            btnComment.setOnClickListener { onComment(post) }
            tvUsername.setOnClickListener { onProfile(post.authorUid) }
            ivAvatar.setOnClickListener { onProfile(post.authorUid) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) =
            oldItem.postId == newItem.postId
        override fun areContentsTheSame(oldItem: Post, newItem: Post) =
            oldItem == newItem
    }
}