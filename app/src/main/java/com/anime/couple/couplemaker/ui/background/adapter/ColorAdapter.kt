package com.anime.couple.couplemaker.ui.background.adapter

import androidx.recyclerview.widget.RecyclerView
import com.anime.couple.couplemaker.base.AbsBaseAdapter
import com.anime.couple.couplemaker.base.AbsBaseDiffCallBack
import com.anime.couple.couplemaker.data.model.SelectedModel
import com.anime.couple.couplemaker.utils.hide
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.utils.show
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ItemColorBgBinding

class ColorAdapter :
    AbsBaseAdapter<SelectedModel, ItemColorBgBinding>(R.layout.item_color_bg, DiffCallBack()) {
    var onClick: ((Int) -> Unit)? = null
    var posSelect = -1
    override fun bind(
        binding: ItemColorBgBinding,
        position: Int,
        data: SelectedModel,
        holder: RecyclerView.ViewHolder
    ) {
        binding.imvColor.onSingleClick {
            onClick?.invoke(position)
        }
        if(position==0){
            binding.imvColor.setBackgroundResource(R.drawable.imv_add_color)
        }else{
            binding.imvColor.setBackgroundColor(data.color)
        }
        if (data.isSelected) {
            binding.vFocus1.show()
        } else {
            binding.vFocus1.hide()
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