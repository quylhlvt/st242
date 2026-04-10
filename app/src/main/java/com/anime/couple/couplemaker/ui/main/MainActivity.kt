package com.anime.couple.couplemaker.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.lifecycleScope
import com.anime.couple.couplemaker.base.AbsBaseActivity
import com.anime.couple.couplemaker.data.callapi.reponse.DataResponse
import com.anime.couple.couplemaker.data.callapi.reponse.LoadingStatus
import com.anime.couple.couplemaker.data.model.BodyPartModel
import com.anime.couple.couplemaker.data.model.ColorModel
import com.anime.couple.couplemaker.data.model.CustomModel
import com.anime.couple.couplemaker.data.repository.ApiRepository
import com.anime.couple.couplemaker.dialog.DialogExit
import com.anime.couple.couplemaker.ui.category.CategoryActivity
import com.anime.couple.couplemaker.ui.my_creation.MyCreationActivity
import com.anime.couple.couplemaker.ui.quick_mix.QuickMixActivity
import com.anime.couple.couplemaker.ui.setting.SettingActivity
import com.anime.couple.couplemaker.utils.CONST
import com.anime.couple.couplemaker.utils.DataHelper
import com.anime.couple.couplemaker.utils.DataHelper.getData
import com.anime.couple.couplemaker.utils.SharedPreferenceUtils
import com.anime.couple.couplemaker.utils.backPress
import com.anime.couple.couplemaker.utils.music.MusicLocal
import com.anime.couple.couplemaker.utils.newIntent
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ActivityMainBinding
import com.anime.couple.couplemaker.ui.cosplay.CosplayActivity
import com.anime.couple.couplemaker.ui.randomone.RandomCatActivity
import com.anime.couple.couplemaker.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.forEach

@AndroidEntryPoint
class MainActivity : AbsBaseActivity<ActivityMainBinding>() {
    @Inject
    lateinit var apiRepository: ApiRepository
    var checkCallingDataOnline = false
    override fun getLayoutId(): Int = R.layout.activity_main
    private var networkReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connectivityManager =
                context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            if (!checkCallingDataOnline) {
                if (networkInfo != null && networkInfo.isConnected) {
                    var checkDataOnline = false
                    DataHelper.arrBlackCentered.forEach {
                        if (it.checkDataOnline) {
                            checkDataOnline = true
                            return@forEach
                        }
                    }
                    if (!checkDataOnline) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            getData(apiRepository)
                        }
                    }
                } else {
                    if (DataHelper.arrBlackCentered.isEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            getData(apiRepository)
                        }
                    }
                }
            }
        }
    }
    override fun onRestart() {
        super.onRestart()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
    }


    override fun initView() {

        MusicLocal.isInSplashOrTutorial = false

        binding.apply {
            tv1.post {
                tv1.isSelected = true
            }
            tv2.post {
               tv2.isSelected = true
            }
            tv3.post {
                tv3.isSelected = true
            }
            tv4.post {
                tv4.isSelected = true
            }

        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
        DataHelper.arrDataOnline.observe(this) {
            it?.let {
                when (it.loadingStatus) {
                    LoadingStatus.Loading -> {
                        checkCallingDataOnline = true
                    }

                    LoadingStatus.Success -> {
                        if (DataHelper.arrBlackCentered.isNotEmpty() && !DataHelper.arrBlackCentered[0].checkDataOnline) {
                            checkCallingDataOnline = false
                            val listA = (it as DataResponse.DataSuccess).body ?: return@observe
                            checkCallingDataOnline = true
                            val sortedMap = listA
                                .toList() // Chuyển map -> list<Pair<String, List<X10>>>
                                .sortedBy { (_, list) ->
                                    list.firstOrNull()?.level ?: Int.MAX_VALUE
                                }
                                .toMap()
                            sortedMap.forEach { key, list ->
                                var a = arrayListOf<BodyPartModel>()
                                list.forEachIndexed { index, x10 ->
                                    // ✅ Lấy charType từ parts
                                    val partSegs = x10.parts.split("-")
                                    val partCharType = partSegs.getOrNull(2)?.toIntOrNull() ?: 1

                                    var b = arrayListOf<ColorModel>()
                                    x10.colorArray.split(",").forEach { coler ->
                                        var c = arrayListOf<String>()
                                        if (coler == "") {
                                            val halfQuantity = maxOf(1, x10.quantity / 2)
                                            for (i in 1..halfQuantity) {
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${i}.png")
                                            }
                                            b.add(ColorModel("#", c))
                                        } else {
                                            for (i in 1..x10.quantity) {
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${coler}/${i}.png")
                                            }
                                            b.add(ColorModel(coler, c))
                                        }
                                    }
                                    a.add(
                                        BodyPartModel(
                                            "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/${x10.parts}/nav.png",
                                            b,
                                            charType = partCharType  // ✅
                                        )
                                    )
                                }
                                var dataModel =
                                    CustomModel(
                                        "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/avatar.png",
                                        a,
                                        true
                                    )
                                dataModel.bodyPart.forEach { mbodyPath ->
                                    if (mbodyPath.icon.substringBeforeLast("/")
                                            .substringAfterLast("/").substringAfter("-") == "1"
                                    ) {
                                        mbodyPath.listPath.forEach {
                                            if (it.listPath[0] != "dice") {
                                                it.listPath.add(0, "dice")
                                            }
                                        }
                                    } else {
                                        mbodyPath.listPath.forEach {
                                            if (it.listPath[0] != "none") {
                                                it.listPath.add(0, "none")
                                                it.listPath.add(1, "dice")
                                            }
                                        }
                                    }
                                }
                                DataHelper.arrBlackCentered.add(0, dataModel)
                            }
                        }
                        checkCallingDataOnline = false
                    }

                    LoadingStatus.Error -> {
                        checkCallingDataOnline = false
                    }

                    else -> {
                        checkCallingDataOnline = true
                    }
                }
            }
        }
    }


    override fun initAction() {
        binding.apply {
            btnCreate.onSingleClick {
                if (isDataReady()) {
                    startActivity(
                        newIntent(
                            applicationContext,
                            CategoryActivity::class.java
                        )
                    )
                } else {
                    showToast(this@MainActivity,R.string.please_wait_a_few_seconds_for_data_to_load)
                }
            }

            btnCosplay.onSingleClick {
                if (isDataReady()) {
                    startActivity(
                        newIntent(
                            applicationContext,
                            CosplayActivity::class.java
                        )
                    )
                } else {
                    showToast(this@MainActivity,R.string.please_wait_a_few_seconds_for_data_to_load)

                }
            }
            btnRandom.onSingleClick {
                if (isDataReady()) {
                    startActivity(
                        newIntent(
                            applicationContext,
                            RandomCatActivity::class.java
                        )
                    )
                } else {
                    lifecycleScope.launch {
                        val dialog= DialogExit(
                            this@MainActivity,
                            "awaitdata"
                        )
                        dialog.show()
                        delay(1500)
                        dialog.dismiss()
                    }
                }
            }
            btnMyWork.onSingleClick {
                if (isDataReady()) {
                    startActivity(
                        newIntent(
                            applicationContext,
                            MyCreationActivity::class.java
                        )
                    )
                } else {
                    showToast(this@MainActivity,R.string.please_wait_a_few_seconds_for_data_to_load)
                }
            }

            imvSetting.onSingleClick {
                startActivity(
                    newIntent(
                        applicationContext,
                        SettingActivity::class.java
                    )
                )
            }
        }
    }
    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(networkReceiver)
        } catch (e: Exception) {

        }
    }
    private fun isDataReady(): Boolean {
        return DataHelper.arrBlackCentered.isNotEmpty()
    }

    override fun onBackPressed() {
        lifecycleScope.launch {
            backPress(
                SharedPreferenceUtils(applicationContext)
            )
        }
    }
}