package com.anime.couple.couplemaker.dialog

import android.app.Activity
import android.widget.Toast
import com.anime.couple.couplemaker.base.BaseDialog1
import com.anime.couple.couplemaker.utils.onClick
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.DialogCreateNameBinding


class CreateNameDialog(val context: Activity) :
    BaseDialog1<DialogCreateNameBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_create_name
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: ((String) -> Unit) = {}
    var onDismissClick: (() -> Unit) = {}

    override fun initView() {
    }

    override fun initAction() {
        binding.apply {
            tvNo.onClick {
                onNoClick.invoke()
            }
            tvYes.onClick {
                val input = edtName.text.toString().trim()

                when {
                    input == "" -> {
                        Toast.makeText(context, context.getString(R.string.please_enter_your_package_name), Toast.LENGTH_SHORT).show()
                    }


                    else -> {
                        onYesClick.invoke(input)
                    }
                }
            }
            flOutSide.onClick {
                onDismissClick.invoke()
            }
        }
    }

    override fun onDismissListener() {

    }
}