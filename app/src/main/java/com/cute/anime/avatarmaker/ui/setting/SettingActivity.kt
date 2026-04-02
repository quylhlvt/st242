package com.cute.anime.avatarmaker.ui.setting

import android.view.View
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.ui.language.LanguageActivity
import com.cute.anime.avatarmaker.utils.RATE
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import com.cute.anime.avatarmaker.utils.newIntent
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.policy
import com.cute.anime.avatarmaker.utils.rateUs
import com.cute.anime.avatarmaker.utils.shareApp
import com.cute.anime.avatarmaker.utils.unItem
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ActivitySettingBinding
import com.cute.anime.avatarmaker.utils.onClick
import com.cute.anime.avatarmaker.utils.show
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingActivity : AbsBaseActivity<ActivitySettingBinding>() {
    @Inject
    lateinit var sharedPreferences: SharedPreferenceUtils
    override fun getLayoutId(): Int = R.layout.activity_setting

    override fun initView() {

        if (sharedPreferences.getBooleanValue(RATE)) {
            binding.llRateUs.visibility = View.GONE
        }
        unItem = {
            binding.llRateUs.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
    }
    override fun initAction() {
        binding.apply {
            llLanguage.onSingleClick {
                startActivity(
                    newIntent(
                        applicationContext,
                        LanguageActivity::class.java
                    )
                )
            }
//            imvMusic.onSingleClick {
//                initMusic(imvMusic)
//            }
            llRateUs.onSingleClick {
                rateUs(0)
            }
            llShareApp.onClick {
                shareApp()
            }
            llPrivacy.onSingleClick {
                policy()
            }
            imvBack.onSingleClick {
                finish()
            }
        }
    }
}