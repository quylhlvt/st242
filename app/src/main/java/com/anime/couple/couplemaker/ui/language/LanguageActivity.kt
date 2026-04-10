package com.anime.couple.couplemaker.ui.language

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anime.couple.couplemaker.base.AbsBaseActivity
import com.anime.couple.couplemaker.data.model.LanguageModel
import com.anime.couple.couplemaker.ui.main.MainActivity
import com.anime.couple.couplemaker.ui.tutorial.TutorialActivity
import com.anime.couple.couplemaker.utils.CONST
import com.anime.couple.couplemaker.utils.DataHelper
import com.anime.couple.couplemaker.utils.SharedPreferenceUtils
import com.anime.couple.couplemaker.utils.SystemUtils
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ActivityLanguageBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.text.equals

@AndroidEntryPoint
class LanguageActivity : AbsBaseActivity<ActivityLanguageBinding>() {
    lateinit var adapter: LanguageAdapter
    var codeLang: String? = null

    @Inject
    lateinit var providerSharedPreference: SharedPreferenceUtils


    override fun getLayoutId(): Int = R.layout.activity_language
    override fun initView() {

        codeLang = providerSharedPreference.getStringValue("language")
        if (codeLang.equals("")) {
            binding.icBack.visibility = View.GONE
//            binding.rlParent.setBackgroundResource(R.drawable.img_bg_start)
            binding.tvTitle2.visibility = View.GONE
//            binding.actionBar.txtCustom.isSelected = true
//            binding.imvDone.setImageResource(R.drawable.ic_tick_2)
        }else{
//            binding.imvDone.setImageResource(R.drawable.ic_tick)
            binding.tvTitle1.visibility = View.GONE
//            binding.actionBar.txtCustom.isSelected = true
            binding.rlParent.setBackgroundResource(R.drawable.img_bg_splash)
//            binding.actionBar.backCustom.visibility = View.VISIBLE
             binding.icBack.visibility = View.VISIBLE
             binding.imvDone.visibility = View.VISIBLE
        }
        binding.rclLanguage.itemAnimator = null
        adapter = LanguageAdapter()
        setRecycleView()
    }

    override fun initAction() {
//
        binding.icBack.onSingleClick {
            finish()
        }

        binding.imvDone.onSingleClick {
            saveLang()
        }
        binding.imvDone.onSingleClick {
            saveLang()
        }
    }
    private fun saveLang(){
        if (codeLang.equals("")) {
            Toast.makeText(
                this,
                getString(R.string.you_have_not_selected_anything_yet),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            SystemUtils.setPreLanguage(applicationContext, codeLang)
            providerSharedPreference.putStringValue("language", codeLang)
            if (SharedPreferenceUtils.Companion.getInstance(applicationContext).getBooleanValue(
                    CONST.LANGUAGE
                )) {
                var intent = Intent(
                    applicationContext,
                    MainActivity::class.java
                )
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                finishAffinity()
                startActivity(intent)
            } else {
                SharedPreferenceUtils.Companion.getInstance(applicationContext)
                    .putBooleanValue(CONST.LANGUAGE, true)
                var intent = Intent(applicationContext, TutorialActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setRecycleView() {
        var i = 0
        lateinit var x: LanguageModel
        if (!codeLang.equals("")) {
            DataHelper.listLanguage.forEach {
                DataHelper.listLanguage[i].active = false
                if (codeLang.equals(it.code)) {
                    x = DataHelper.listLanguage[i]
                    x.active = true
                }
                i++
            }

            DataHelper.listLanguage.remove(x)
            DataHelper.listLanguage.add(0, x)
        }
        adapter.getData(DataHelper.listLanguage)
        binding.rclLanguage.adapter = adapter
        val manager = GridLayoutManager(applicationContext, 1, RecyclerView.VERTICAL, false)
        binding.rclLanguage.layoutManager = manager

        adapter.onClick = {
            if (codeLang.equals("")) {
                binding.imvDone.visibility = View.VISIBLE

            }
            codeLang = DataHelper.listLanguage[it].code
        }
    }

    override fun onBackPressed() {
        DataHelper.listLanguage[DataHelper.positionLanguageOld].active = false
        DataHelper.positionLanguageOld = 0
        super.onBackPressed()
    }
}