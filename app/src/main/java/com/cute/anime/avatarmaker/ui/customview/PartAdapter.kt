package com.cute.anime.avatarmaker.ui.customview

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.base.AbsBaseAdapter
import com.cute.anime.avatarmaker.base.AbsBaseDiffCallBack
import com.cute.anime.avatarmaker.utils.onClickCustom
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ItemPartBinding

class PartAdapter : AbsBaseAdapter<String, ItemPartBinding>(R.layout.item_part, PathDiff()) {
    var onClick: ((Int,String) -> Unit)? = null
    var posPath = 0
    var listThumb: List<String> = emptyList()
    //    var checkOnline = false
    fun setPos(pos: Int) {
        val old = posPath
        posPath = pos
        if (old != pos) {
            if (old >= 0) notifyItemChanged(old)
            if (pos >= 0) notifyItemChanged(pos)
        }
    }

    class PathDiff : AbsBaseDiffCallBack<String>() {
        override fun itemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun contentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem != newItem
        }

    }

    override fun bind(
        binding: ItemPartBinding,
        position: Int,
        data: String,
        holder: RecyclerView.ViewHolder
    ) {
        binding.apply {
            val context = root.context

            if (posPath == position) {
                materialCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.app_color2)
                )
                materialCard.strokeColor =
                    ContextCompat.getColor(context, R.color.app_color)
            } else {
                materialCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.white)
                )
                materialCard.strokeColor =
                    ContextCompat.getColor(context, R.color.white)
            }
        }
        val displayPath = if (listThumb.size > position) listThumb[position] else data

        Glide.with(binding.imv).clear(binding.imv)
        // 🔴 BẮT BUỘC: scaleType cố định
        binding.imv.scaleType = ImageView.ScaleType.CENTER_INSIDE
        // reset padding (KHÔNG dùng margin)
        when (data) {
            "none" -> {
                loadImage(binding, R.drawable.ic_none)
            }
            "dice" -> {
                loadImage(binding, R.drawable.ic_random_layer)
            }
            else -> {
                loadImage(binding, displayPath)
            }
        }
        binding.root.onClickCustom {
            onClick?.invoke(position,data)
        }
    }
    private fun loadImage(binding: ItemPartBinding, data: Any) {
        Glide.with(binding.imv)
            .load(data)
            .encodeQuality(60)
            .override(128)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.imv)
    }
}