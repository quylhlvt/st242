package com.anime.couple.couplemaker.ui.background.adapter

import androidx.recyclerview.widget.RecyclerView
import com.anime.couple.couplemaker.base.AbsBaseAdapter
import com.anime.couple.couplemaker.base.AbsBaseDiffCallBack
import com.anime.couple.couplemaker.data.model.SelectedModel
import com.anime.couple.couplemaker.utils.hide
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.utils.show
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ItemImageBinding

class ImageAdapter :
    AbsBaseAdapter<SelectedModel, ItemImageBinding>(R.layout.item_image, DiffCallBack()) {
    var onClick: ((Int) -> Unit)? = null
    var posSelect = -1
    override fun bind(
        binding: ItemImageBinding,
        position: Int,
        data: SelectedModel,
        holder: RecyclerView.ViewHolder
    ) {
        binding.tvAddImage.isSelected = true
        binding.imvImage.onSingleClick {
            onClick?.invoke(position)
        }

        Glide.with(binding.root).load(data.path).encodeQuality(70)
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.imvImage)
        if (position == 0) {
            binding.lnlAddItem.show()
        } else {
            binding.lnlAddItem.hide()
        }
        if (data.isSelected) {
            binding.vFocus.show()

        } else {
            binding.vFocus.hide()

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