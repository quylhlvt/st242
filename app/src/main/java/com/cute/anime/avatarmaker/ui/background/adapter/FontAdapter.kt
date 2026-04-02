package com.cute.anime.avatarmaker.ui.background.adapter

import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.base.AbsBaseAdapter
import com.cute.anime.avatarmaker.base.AbsBaseDiffCallBack
import com.cute.anime.avatarmaker.data.model.SelectedModel
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ItemFontBinding

class FontAdapter :
    AbsBaseAdapter<SelectedModel, ItemFontBinding>(R.layout.item_font, DiffCallBack()) {
    var onClick: ((Int) -> Unit)? = null
    var posSelect = 0
    override fun bind(
        binding: ItemFontBinding,
        position: Int,
        data: SelectedModel,
        holder: RecyclerView.ViewHolder
    ) {
        binding.apply {
            material.onSingleClick {
                onClick?.invoke(position)
            }
            tv.typeface = ResourcesCompat.getFont(binding.root.context, data.color)
            if (data.isSelected) {
                material.cardElevation=  3f

//            binding.tv.setTextColor(ContextCompat.getColor(binding.root.context, R.color.app_color))
                material.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.app_color))
            } else {
                material.cardElevation=  0f
//            binding.tv.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                material.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.app_color3))
            }
        }
    }

    class DiffCallBack : AbsBaseDiffCallBack<SelectedModel>() {
        override fun itemsTheSame(
            oldItem: SelectedModel,
            newItem: SelectedModel
        ): Boolean {
            return oldItem == newItem
        }

        override fun contentsTheSame(
            oldItem: SelectedModel,
            newItem: SelectedModel
        ): Boolean {
            return oldItem.path != newItem.path || oldItem.isSelected != newItem.isSelected
        }

    }
}