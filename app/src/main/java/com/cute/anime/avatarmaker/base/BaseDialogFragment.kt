package com.cute.anime.avatarmaker.base

import android.R
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import com.cute.anime.avatarmaker.utils.showSystemUI

abstract class BaseDialogFragment<B : ViewDataBinding>(var context: Activity) :
    DialogFragment() {
    lateinit var binding: B
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = makeBinding(inflater, container)
        initView(binding.root)
        return binding.root
    }

    abstract fun makeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): B

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.Theme_Translucent_NoTitleBar
        );
    }

    override fun onDestroy() {
        super.onDestroy()
        context.showSystemUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
    }

    abstract fun initView(view: View)
}