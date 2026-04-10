package com.anime.couple.couplemaker.ui.category

import androidx.recyclerview.widget.RecyclerView
import com.anime.couple.couplemaker.base.AbsBaseAdapter
import com.anime.couple.couplemaker.base.AbsBaseDiffCallBack
import com.anime.couple.couplemaker.data.model.CustomModel
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.utils.shimmer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.shimmer.ShimmerDrawable
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ItemCategoryBinding

class CategoryAdapter : AbsBaseAdapter<CustomModel, ItemCategoryBinding>(
    R.layout.item_category, DiffCallBack()
) {
    var onCLick: ((Int) -> Unit)? = null
    override fun bind(
        binding: ItemCategoryBinding,
        position: Int,
        data: CustomModel,
        holder: RecyclerView.ViewHolder
    ) {
        val shimmerDrawable = ShimmerDrawable().apply {
            setShimmer(shimmer)
        }
        Glide.with(binding.root).load(data.avt).encodeQuality(70).diskCacheStrategy(
            DiskCacheStrategy.AUTOMATIC).placeholder(shimmerDrawable).into(binding.imvImage)
        binding.imvImage.onSingleClick {
            onCLick?.invoke(position)
        }
    }

    class DiffCallBack : AbsBaseDiffCallBack<CustomModel>() {
        override fun itemsTheSame(
            oldItem: CustomModel, newItem: CustomModel
        ): Boolean {
            return oldItem.avt == newItem.avt
        }

        override fun contentsTheSame(
            oldItem: CustomModel, newItem: CustomModel
        ): Boolean {
            return oldItem.avt != newItem.avt
        }

    }
}