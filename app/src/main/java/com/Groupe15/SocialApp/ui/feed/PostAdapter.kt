package com.Groupe15.SocialApp.ui.feed

import android.text.format.DateUtils
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
    private val currentUserId: String?,
    private val onLike: (Post) -> Unit,
    private val onComment: (Post) -> Unit,
    private val onShare: (Post) -> Unit,
    private val onFollow: (String) -> Unit,
    private val onProfile: (String) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivAvatar = itemView.findViewById<ImageView>(R.id.ivAvatar)
        private val tvUsername = itemView.findViewById<TextView>(R.id.tvUsername)
        private val tvContent = itemView.findViewById<TextView>(R.id.tvContent)
        private val tvTimestamp = itemView.findViewById<TextView>(R.id.tvTimestamp)
        private val btnLike = itemView.findViewById<ImageButton>(R.id.btnLike)
        private val btnComment = itemView.findViewById<ImageButton>(R.id.btnComment)
        private val btnShare = itemView.findViewById<ImageButton>(R.id.btnShare)
        private val btnFollow = itemView.findViewById<TextView>(R.id.btnFollow)
        private val ivPostImage = itemView.findViewById<ImageView>(R.id.ivPostImage)

        fun bind(post: Post) {
            tvUsername.text = post.authorUsername
            tvContent.text = post.content

            // Formatage de la date
            tvTimestamp.text = if (post.createdAt != null) {
                DateUtils.getRelativeTimeSpanString(
                    post.getCreatedAtMillis(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            } else itemView.context.getString(R.string.no_posts) // Ou un placeholder

            // Avatar
            ivAvatar.load(post.authorProfileUrl.ifEmpty { null }) {
                crossfade(true)
                placeholder(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            // Image du post (Fond de carte)
            ivPostImage.load(post.imageUrl.ifEmpty { null }) {
                crossfade(true)
                placeholder(R.drawable.placeholder_cover)
                error(R.drawable.placeholder_cover)
            }

            // Actions
            btnLike.setOnClickListener { onLike(post) }
            btnComment.setOnClickListener { onComment(post) }
            btnShare.setOnClickListener { onShare(post) }
            
            // Masquer Follow si c'est notre propre post
            btnFollow.visibility = if (post.authorUid == currentUserId) View.GONE else View.VISIBLE
            btnFollow.setOnClickListener { onFollow(post.authorUid) }
            
            val toProfile = View.OnClickListener { onProfile(post.authorUid) }
            tvUsername.setOnClickListener(toProfile)
            ivAvatar.setOnClickListener(toProfile)
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
