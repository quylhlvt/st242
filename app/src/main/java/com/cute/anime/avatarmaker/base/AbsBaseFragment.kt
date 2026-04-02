package com.cute.anime.avatarmaker.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.cute.anime.avatarmaker.utils.SystemUtils
import com.cute.anime.avatarmaker.utils.showSystemUI


abstract class AbsBaseFragment <V: ViewDataBinding, G: Activity>: Fragment() {
    lateinit var binding : V
    private var mView: View? = null
    lateinit var mActivity : Activity


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (mView != null){
            mView
        } else{
            SystemUtils.setLocale(requireContext())
            binding = DataBindingUtil.inflate(inflater, getLayout(), container, false)
            binding.lifecycleOwner = this
            mView = binding.root
            mActivity = activity as G
            mActivity.showSystemUI()
            initView()
            setClick()
            binding.root
        }

    }

    abstract fun getLayout(): Int
    abstract fun initView()
    abstract fun setClick()
}