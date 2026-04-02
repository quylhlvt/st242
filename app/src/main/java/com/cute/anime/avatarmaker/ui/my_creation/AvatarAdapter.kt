package com.cute.anime.avatarmaker.ui.my_creation

import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.base.AbsBaseAdapter
import com.cute.anime.avatarmaker.base.AbsBaseDiffCallBack
import com.cute.anime.avatarmaker.utils.SystemUtils.loadImageFromFile
import com.cute.anime.avatarmaker.utils.hide
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.shimmer
import com.cute.anime.avatarmaker.utils.show
import com.facebook.shimmer.ShimmerDrawable
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ItemMyAvatarBinding

class AvatarAdapter :
    AbsBaseAdapter<String, ItemMyAvatarBinding>(R.layout.item_my_avatar, DiffCallBack()) {
    var onClick: ((Int, String) -> Unit)? = null
    var arrCheckTick = arrayListOf<Int>()
    var checkLongClick = false
    override fun bind(
        binding: ItemMyAvatarBinding,
        position: Int,
        data: String,
        holder: RecyclerView.ViewHolder
    ) {
        val shimmerDrawable = ShimmerDrawable().apply {
            setShimmer(shimmer)
        }
        binding.imvImage.loadImageFromFile(data)

        binding.imvImage.onSingleClick {
            onClick?.invoke(position, "item")
        }
        binding.btnDelete.onSingleClick {
            onClick?.invoke(position, "delete")
        }
        binding.btnEdit.onSingleClick {
            onClick?.invoke(position, "edit")
        }
        binding.imvImage.setOnLongClickListener {
            onClick?.invoke(position, "longclick")
            true
        }
        binding.btnSelect.onSingleClick {
            onClick?.invoke(position, "tick")
        }
        if (checkLongClick) {
            binding.btnSelect.show()
            if (position in arrCheckTick) {
                binding.btnSelect.setImageResource(R.drawable.imv_check_true)
            } else {
                binding.btnSelect.setImageResource(R.drawable.imv_check_false)
            }
            binding.btnEdit.hide()
            binding.btnDelete.hide()
        } else {
            binding.btnSelect.hide()
            binding.btnEdit.show()
            binding.btnDelete.show()
        }

    }

    class DiffCallBack : AbsBaseDiffCallBack<String>() {
        override fun itemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
        override fun contentsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean {
            return oldItem != newItem
        }
    }
}