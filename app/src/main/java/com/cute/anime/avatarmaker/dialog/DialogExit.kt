package com.cute.anime.avatarmaker.dialog

import android.app.Activity
import androidx.core.content.ContextCompat
import com.cute.anime.avatarmaker.base.BaseDialog
import com.cute.anime.avatarmaker.utils.hide
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.show
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.DialogExitBinding

class DialogExit(context: Activity, var type: String) :
    BaseDialog<DialogExitBinding>(context, false) {
    var onClick: (() -> Unit)? = null
    override fun getContentView(): Int = R.layout.dialog_exit

    override fun initView() {
//        binding.txtContent.gradientHorizontal(
//            "#01579B".toColorInt(),
//            "#2686C6".toColorInt())
//        binding.txtTitle.setTextColor(ContextCompat.getColor(context, R.color.white))

        when (type) {
            "exit" -> {
                binding.txtTitle.text = context.getString(R.string.exit)
                binding.txtTitle.isSelected = true
                binding.txtContent.text =
                    context.getString(R.string.haven_saved_it_yet_are_you_sure_to_exit)
//                binding.nativeAds.show()
//                Admob.getInstance().loadNativeAd(
//                    context,
//                    context.getString(R.string.native_dialog),
//                    binding.nativeAds,
//                    com.lvt.ads.R.layout.ads_native_avg2
//                )
            }

            "network" -> {
                binding.apply {
//                    frameTitle.setBackgroundResource(R.drawable.bg_title_dialog_nointernet)
//                    constraintLayout.setBackgroundResource(R.drawable.bg_dialog_nointernet)
//                    imgInBgDialog.setImageResource(R.drawable.img_in_nointernet)
                    txtTitle.text = context.getString(R.string.no_internet)
                    txtTitle.isSelected = true
                    btnYes.hide()
                    btnNo.hide()
                    btnOk.show()
                    txtContent.hide()
                    txtContent1.show()
                    txtContent1.text =
                        context.getString(R.string.please_check_your_network_connection)
                }
            }
            "networked" -> {
                binding.apply {
//                    frameTitle.setBackgroundResource(R.drawable.bg_title_dialog_nointernet)
//                    constraintLayout.setBackgroundResource(R.drawable.bg_dialog_nointernet)
//                    imgInBgDialog.setImageResource(R.drawable.img_in_nointernet)
                    txtTitle.text = context.getString(R.string.internet)
                    txtTitle.isSelected = true
                    btnYes.hide()
                    btnNo.hide()
                    btnOk.show()
                    txtContent.hide()
                    txtContent1.show()
                    txtContent1.text =
                        context.getString(R.string.unstable_connection_please_check_your_network_connection)
                }
            }

            "loadingnetwork" -> {
                binding.apply {
//                    frameTitle.setBackgroundResource(R.drawable.bg_title_dialog_nointernet)
//                    constraintLayout.setBackgroundResource(R.drawable.bg_dialog_nointernet)
//                    imgInBgDialog.setImageResource(R.drawable.img_in_nointernet)
                    txtTitle.text = context.getString(R.string.no_internet)
                    txtTitle.isSelected = true
                    btnYes.hide()
                    btnNo.hide()
                    btnOk.show()
                    txtContent.hide()
                    txtContent1.show()
                    txtContent1.text =
                        context.getString(R.string.please_check_your_network_connection)
                }
            }

            "reset" -> {
                binding.apply {
//                    frameTitle.setBackgroundResource(R.drawable.bg_title_dialog_reset)
//                    constraintLayout.setBackgroundResource(R.drawable.bg_dialog_reset)
//                    imgInBgDialog.setImageResource(R.drawable.img_in_reset)
//                    val color = ContextCompat.getColor(context, R.color.app_color11)
//                    btnYes.setCardBackgroundColor(color)
//                    btnNo.strokeColor = color
//                    txtNo.setTextColor(color)
//                    txtContent.setTextColor(color)

                    binding.txtTitle.text = context.getString(R.string.reset)
                    binding.txtTitle.isSelected = true
                    binding.txtContent.text = context.getString(R.string.do_you_want_to_reset_all)
                }

            }

            "delete" -> {
                binding.txtTitle.text = context.getString(R.string.delete)
                binding.txtTitle.isSelected = true
                binding.txtContent.text =
                    context.getString(R.string.are_you_want_to_delete_this_item)
            }

            "awaitdata" -> {
                binding.apply {
//                    frameTitle.setBackgroundResource(R.drawable.bg_title_dialog_nointernet)
//                    constraintLayout.setBackgroundResource(R.drawable.bg_dialog_nointernet)
//                    imgInBgDialog.setImageResource(R.drawable.img_in_nointernet)
                    txtTitle.text = context.getString(R.string.no_internet)
                    txtTitle.isSelected = true
                    btnYes.hide()
                    btnNo.hide()
                    btnOk.show()
                    txtContent.hide()
                    txtContent1.show()
                    txtContent1.text =
                        context.getString(R.string.please_wait_a_few_seconds_for_data_to_load)
                }

            }

            "awaitdataHome" -> {
                binding.apply {
//                    frameTitle.setBackgroundResource(R.drawable.bg_title_dialog_nointernet)
//                    constraintLayout.setBackgroundResource(R.drawable.bg_dialog_nointernet)
//                    imgInBgDialog.setImageResource(R.drawable.img_in_nointernet)
                    txtTitle.text = context.getString(R.string.no_internet)
                    txtTitle.isSelected = true
                    btnYes.hide()
                    btnNo.hide()
                    btnOk.show()
                    txtContent.hide()
                    txtContent1.show()
                    txtContent1.text =
                        context.getString(R.string.please_connect_to_the_internet_to_download_more_data)
                }

            }
        }
    }

    override fun bindView() {
        binding.apply {
            btnYes.onSingleClick {
                onClick?.invoke()
                dismiss()
            }
            btnNo.onSingleClick {
                dismiss()
            }
            btnOk.onSingleClick {
                onClick?.invoke()
                dismiss()
            }
        }
    }
}