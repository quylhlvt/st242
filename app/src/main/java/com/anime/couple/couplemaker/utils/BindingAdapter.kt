package com.anime.couple.couplemaker.utils


import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import androidx.core.graphics.toColorInt
import com.anime.couple.couplemaker.data.model.LanguageModel
import com.anime.couple.couplemaker.R
import com.google.android.material.card.MaterialCardView
import ir.kotlin.quyhcolorpicker.dp

@BindingAdapter("setBGCV")
fun ConstraintLayout.setBGCV(check: LanguageModel) {
    if (check.active) {
        this.setBackgroundResource(R.drawable.bg_card_border_100_select)
        this.setPadding(0,0,0,dp(6).toInt())
    } else {
        this.setBackgroundResource(R.drawable.bg_card_border_100_false)
        this.setPadding(0,0,0,0)
    }
}
@BindingAdapter("setCard")
fun MaterialCardView.setCard(model: LanguageModel) {
    val color = if (model.active) {
        ContextCompat.getColor(context, R.color.white)
    } else {
        ContextCompat.getColor(context, R.color.app_color)
    }

    strokeColor = color
}

@BindingAdapter("setSrcCheckLanguage")
fun AppCompatImageView.setSrcCheckLanguage(check: Boolean) {
    if (check) {
        this.setImageResource(R.drawable.img_radio_language_select)
    } else {
        this.setImageResource(R.drawable.img_radio_language_unselect)
    }
}
@BindingAdapter("setTextColor")
fun TextView.setTextColor(check: Boolean) {
    if (check) {
        this.setTextColor(ContextCompat.getColor(context, R.color.app_color2))
    } else {
        this.setTextColor(ContextCompat.getColor(context, R.color.app_color2))
    }
}
@BindingAdapter("setBG")
fun AppCompatImageView.setBG(id: Int) {
    Glide.with(this).load(id).into(this)
}
@BindingAdapter("setImg")
fun AppCompatImageView.setImg(data : Int){
    Glide.with(this).load(data).into(this)
}