package com.Groupe15.SocialApp.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUsername  = itemView.findViewById<TextView>(R.id.tvCommentUsername)
        private val tvText      = itemView.findViewById<TextView>(R.id.tvCommentText)
        private val tvTimestamp = itemView.findViewById<TextView>(R.id.tvCommentTimestamp)

        fun bind(comment: Comment) {
            tvUsername.text  = comment.username
            tvText.text      = comment.text
            tvTimestamp.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(comment.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) =
            oldItem.commentId == newItem.commentId
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) =
            oldItem == newItem
    }
}