package com.cute.anime.avatarmaker.ui.background.adapter

import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.base.AbsBaseAdapter
import com.cute.anime.avatarmaker.base.AbsBaseDiffCallBack
import com.cute.anime.avatarmaker.data.model.SelectedModel
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ItemStikerBgBinding

class StikerAdapter :
    AbsBaseAdapter<SelectedModel, ItemStikerBgBinding>(
        R.layout.item_stiker_bg,
        DiffCallBack()
    ) {
    var onClick: ((String) -> Unit)? = null
    override fun bind(
        binding: ItemStikerBgBinding,
        position: Int,
        data: SelectedModel,
        holder: RecyclerView.ViewHolder
    ) {
        binding.imv.onSingleClick {
            onClick?.invoke(data.path)
        }
        Glide.with(binding.root).load(data.path)
            .override(256, 256)
            .encodeQuality(50)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.imv)
    }

    class DiffCallBack :
        AbsBaseDiffCallBack<SelectedModel>() {
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