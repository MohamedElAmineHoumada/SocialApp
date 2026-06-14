package com.Groupe15.SocialApp.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.databinding.ItemInterestBinding

data class Interest(
    val id: String,
    val name: String,
    val iconRes: Int,
    var isSelected: Boolean = false
)

class InterestsAdapter(private val onSelectionChanged: (Int) -> Unit) :
    RecyclerView.Adapter<InterestsAdapter.InterestViewHolder>() {

    private val interests = mutableListOf<Interest>()

    fun setInterests(newInterests: List<Interest>) {
        interests.clear()
        interests.addAll(newInterests)
        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int = interests.count { it.isSelected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val binding = ItemInterestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InterestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        holder.bind(interests[position])
    }

    override fun getItemCount(): Int = interests.size

    inner class InterestViewHolder(private val binding: ItemInterestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(interest: Interest) {
            binding.tvName.text = interest.name
            binding.ivIcon.setImageResource(interest.iconRes)

            updateSelectionUI(interest.isSelected)

            binding.root.setOnClickListener {
                interest.isSelected = !interest.isSelected
                updateSelectionUI(interest.isSelected)
                onSelectionChanged(getSelectedCount())
            }
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            val context = binding.root.context
            if (isSelected) {
                binding.root.setCardBackgroundColor(context.getColor(R.color.purple_primary))
                binding.tvName.setTextColor(context.getColor(R.color.white))
                binding.ivIcon.setColorFilter(context.getColor(R.color.white))
                binding.cbSelected.visibility = View.VISIBLE
            } else {
                binding.root.setCardBackgroundColor(context.getColor(R.color.app_card))
                binding.tvName.setTextColor(context.getColor(R.color.app_text_primary))
                binding.ivIcon.setColorFilter(context.getColor(R.color.purple_primary))
                binding.cbSelected.visibility = View.GONE
            }
        }
    }
}
