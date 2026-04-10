package com.anime.couple.couplemaker.ui.customview

import android.graphics.Color
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.base.AbsBaseAdapter
import com.anime.couple.couplemaker.data.model.ColorModel
import com.anime.couple.couplemaker.databinding.ItemColorBinding
import com.anime.couple.couplemaker.utils.onClickCustom
import com.anime.couple.couplemaker.base.AbsBaseDiffCallBack
import android.util.Log

class ColorAdapter : AbsBaseAdapter<ColorModel, ItemColorBinding>(R.layout.item_color, DiffColor()) {
    var onClick: ((Int) -> Unit)? = null
    var posColor = 0

    fun setPos(pos: Int) {
        posColor = pos
        notifyDataSetChanged() // Nếu cần update UI ngay
    }

    class DiffColor : AbsBaseDiffCallBack<ColorModel>() {
        override fun itemsTheSame(oldItem: ColorModel, newItem: ColorModel): Boolean {
            return oldItem.color == newItem.color
        }

        override fun contentsTheSame(oldItem: ColorModel, newItem: ColorModel): Boolean {
            return oldItem == newItem  // So sánh toàn bộ object tốt hơn
        }
    }

    override fun bind(
        binding: ItemColorBinding,
        position: Int,
        data: ColorModel,
        holder: RecyclerView.ViewHolder
    ) {
        if (posColor == position) {
            binding.imv.visibility = View.VISIBLE
        } else {
            binding.imv.visibility = View.GONE
        }

        val colorInt = try {
            val hex = "#${data.color.trim()}"
            if (hex.length == 7 || hex.length == 9) {
                hex.toColorInt()
            } else {
                Color.GRAY
            }
        } catch (e: Exception) {
            Log.w("ColorAdapter", "Invalid color: #${data.color}", e)
            Color.GRAY  // hoặc Color.TRANSPARENT nếu muốn trong suốt
        }

        binding.bg.setColorFilter(colorInt)

        binding.bg.onClickCustom {
            onClick?.invoke(position)
        }
    }
}