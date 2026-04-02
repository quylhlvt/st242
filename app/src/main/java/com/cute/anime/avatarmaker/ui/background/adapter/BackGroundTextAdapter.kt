package com.cute.anime.avatarmaker.ui.background.adapter

import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.base.AbsBaseAdapter
import com.cute.anime.avatarmaker.base.AbsBaseDiffCallBack
import com.cute.anime.avatarmaker.data.model.SelectedModel
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ItemTextBgBinding

class BackGroundTextAdapter :
    AbsBaseAdapter<SelectedModel, ItemTextBgBinding>(R.layout.item_text_bg, DiffCallBack()) {
    var onClick: ((Int) -> Unit)? = null
    override fun bind(
        binding: ItemTextBgBinding,
        position: Int,
        data: SelectedModel,
        holder: RecyclerView.ViewHolder
    ) {
        binding.imv.onSingleClick {
            onClick?.invoke(position)
        }
        Glide.with(binding.root).load(data.path).encodeQuality(70)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.imv)
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