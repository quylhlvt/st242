package com.anime.couple.couplemaker.dialog

import android.app.Activity
import android.graphics.Color
import com.anime.couple.couplemaker.base.BaseDialog
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.DialogColorPickerBinding


class ChooseColorDialog(context: Activity) : BaseDialog<DialogColorPickerBinding>(context, false) {
    var onDoneEvent: ((Int) -> Unit) = {}
    private var color = Color.WHITE
    override fun getContentView(): Int = R.layout.dialog_color_picker

    override fun initView() {
        binding.colorPickerView.hueSliderView = binding.hueSlider
        updateHexText(color)
        binding.txtDone.isSelected=true
        binding.tvTitle.isSelected=true
        binding.txtCancel.isSelected=true
    }

    override fun bindView() {
        binding.apply {

            colorPickerView.setOnColorChangedListener { newColor ->
                color = newColor
                updateHexText(color)
            }

            btnClose.onSingleClick { dismiss() }

            btnDone.onSingleClick {
                dismiss()
                onDoneEvent.invoke(color)
            }
        }
    }

    private fun updateHexText(color: Int) {
        val hex = color and 0xFFFFFF
//        binding.txtColor.text =
//            String.format(Locale.US, "#%06x", hex)
    }
}