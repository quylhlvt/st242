package com.cute.anime.avatarmaker.dialog

import android.app.Activity
import android.widget.Toast
import com.cute.anime.avatarmaker.base.BaseDialog
import com.cute.anime.avatarmaker.utils.RATE
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.DialogRateBinding


class DialogRate(context: Activity) : BaseDialog<DialogRateBinding>(context, false) {
    var i = 0
    private lateinit var onPress: OnPress
    override fun getContentView(): Int = R.layout.dialog_rate
    interface OnPress {
        fun rating()
        fun cancel()
        fun later()
    }

    override fun initView() {
    }

    fun init(onPress: OnPress?) {
        this.onPress = onPress!!
    }

    override fun bindView() {
        setView(R.string.zero_star_title, R.string.zero_star, R.drawable.ic_rate_rero)
        binding.btnCancal.onSingleClick {
            dismiss()
            onPress.cancel()
        }
        binding.btnVote.onSingleClick {
            when (i) {
                0 -> {
                    Toast.makeText(
                        context,
                        context.getText(R.string.rate_us_0),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                1, 2, 3 -> {
                    SharedPreferenceUtils.Companion.getInstance(context).putBooleanValue(
                        RATE, true)
                    dismiss()
                    onPress.later()
                }

                else -> {
                    SharedPreferenceUtils.Companion.getInstance(context).putBooleanValue(
                        RATE, true)
                    onPress.rating()
                    Toast.makeText(
                        context,
                        context.getText(R.string.rate_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        binding.ll1.setOnRatingChangeListener { ratingBar, rating, fromUser ->
            i = rating.toInt()
            when (i) {
                0 -> {
                    setView(R.string.zero_star_title, R.string.zero_star, R.drawable.ic_rate_rero)
                }

                1 -> {
                    setView(R.string.one_start_title, R.string.one_start, R.drawable.ic_rate_one)
                }

                2 -> {
                    setView(R.string.one_start_title, R.string.one_start, R.drawable.ic_rate_two)
                }

                3 -> {
                    setView(R.string.one_start_title, R.string.one_start, R.drawable.ic_rate_three)
                }

                4 -> {
                    setView(R.string.four_start_title, R.string.four_start, R.drawable.ic_rate_four)
                }

                5 -> {
                    setView(R.string.four_start_title, R.string.four_start, R.drawable.ic_rate_five)
                }
            }
        }
    }

    fun setView(tv1: Int, tv2: Int, img: Int) {
        binding.tv1.text = (context.resources.getString(tv1))
        binding.tv2.text = (context.resources.getString(tv2))
        binding.imvAvtRate.setImageResource(img)
    }
}