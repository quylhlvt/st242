package com.anime.couple.couplemaker.ui.setting

import android.view.View
import com.anime.couple.couplemaker.base.AbsBaseActivity
import com.anime.couple.couplemaker.ui.language.LanguageActivity
import com.anime.couple.couplemaker.utils.RATE
import com.anime.couple.couplemaker.utils.SharedPreferenceUtils
import com.anime.couple.couplemaker.utils.newIntent
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.utils.policy
import com.anime.couple.couplemaker.utils.rateUs
import com.anime.couple.couplemaker.utils.shareApp
import com.anime.couple.couplemaker.utils.unItem
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ActivitySettingBinding
import com.anime.couple.couplemaker.utils.onClick
import com.anime.couple.couplemaker.utils.show
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