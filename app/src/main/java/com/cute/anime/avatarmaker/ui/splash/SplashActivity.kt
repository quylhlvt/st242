package com.cute.anime.avatarmaker.ui.splash

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.data.callapi.reponse.DataResponse
import com.cute.anime.avatarmaker.data.callapi.reponse.LoadingStatus
import com.cute.anime.avatarmaker.data.model.BodyPartModel
import com.cute.anime.avatarmaker.data.model.ColorModel
import com.cute.anime.avatarmaker.data.model.CustomModel
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.ui.language.LanguageActivity
import com.cute.anime.avatarmaker.ui.tutorial.TutorialActivity
import com.cute.anime.avatarmaker.utils.CONST
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.DataHelper.getData
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import com.cute.anime.avatarmaker.utils.music.MusicLocal
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ActivitySplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.forEach
import kotlin.collections.toList
@AndroidEntryPoint
class SplashActivity : AbsBaseActivity<ActivitySplashBinding>() {
    @Inject
    lateinit var apiRepository: ApiRepository
    @Inject
    lateinit var sharedPreferenceUtils: SharedPreferenceUtils
    private var minDelayPassed = false
    private var dataReady = false
    override fun getLayoutId(): Int = R.layout.activity_splash
    override fun initView() {
        MusicLocal.isInSplashOrTutorial = true
        // Observe data loading TRƯỚC khi load
        observeDataLoading()
        lifecycleScope.launch {
            delay(3000)
            minDelayPassed = true
            // Nếu data đã sẵn sàng thì chuyển màn ngay
            if (dataReady) {
                navigateToNextScreen()
            }
        }



    }
    override fun onResume() {
        super.onResume()
    }
    override fun initAction() {
        // Bắt đầu load data
        lifecycleScope.launch(Dispatchers.IO) {
            getData(apiRepository)
        }
    }

    private fun observeDataLoading() {
        DataHelper.arrDataOnline.observe(this) { response ->
            response?.let {
                when (it.loadingStatus) {
                    LoadingStatus.Loading -> {
                        // Đang loading
                    }

                    LoadingStatus.Success -> {
                        if (DataHelper.arrBlackCentered.isNotEmpty() && !DataHelper.arrBlackCentered.first().checkDataOnline) {
                            val listA = (it as DataResponse.DataSuccess).body
                            if (listA != null) {
                                val sortedMap = listA
                                    .toList()
                                    .sortedBy { (_, list) ->
                                        list.firstOrNull()?.level ?: Int.MAX_VALUE
                                    }
                                    .toMap()

                                sortedMap.forEach { key, list ->
                                    val bodyPartList = arrayListOf<BodyPartModel>()
                                    list.forEach { x10 ->
                                        // ✅ Skip item quantity = 0
                                        if (x10.quantity <= 0) return@forEach

                                        val colorList = arrayListOf<ColorModel>()
                                        val thumbList = arrayListOf<String>()

                                        val halfQuantity = maxOf(1, x10.quantity / 2)



                                        if (x10.colorArray.isEmpty()) {
                                            for (i in 1..halfQuantity) {
                                            thumbList.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/thumb_${i}.png")
                                        }
                                            val pathList = arrayListOf<String>()
                                            for (i in 1..halfQuantity) {
                                                pathList.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${i}.png")
                                            }
                                            colorList.add(ColorModel("", pathList))
                                        } else {
                                            for (i in 1..halfQuantity*2+1) {
                                                thumbList.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/thumb_${i}.png")
                                            }
                                            x10.colorArray.split(",").forEach { color ->
                                                val pathList = arrayListOf<String>()
                                                for (i in 1..x10.quantity) {  // ✅ có màu → full quantity
                                                    pathList.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${color}/${i}.png")
                                                }
                                                colorList.add(ColorModel(color, pathList))
                                            }
                                        }

                                        bodyPartList.add(
                                            BodyPartModel(
                                                "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/${x10.parts}/nav.png",
                                                colorList,
                                                thumbList
                                            )
                                        )
                                    }

                                    val dataModel = CustomModel(
                                        "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/avatar.png",
                                        bodyPartList,
                                        true
                                    )

                                    // ✅ SAU - thêm isNotEmpty() giống SplashActivity
                                    dataModel.bodyPart.forEach { mbodyPath ->
                                        if (mbodyPath.icon.substringBeforeLast("/")
                                                .substringAfterLast("/").substringAfter("-") == "1"
                                        ) {
                                            mbodyPath.listPath.forEach {
                                                if (it.listPath.isNotEmpty() && it.listPath[0] != "dice") {
                                                    it.listPath.add(0, "dice")
                                                }
                                            }
                                        } else {
                                            mbodyPath.listPath.forEach {
                                                if (it.listPath.isNotEmpty() && it.listPath[0] != "none") {
                                                    it.listPath.add(0, "none")
                                                    it.listPath.add(1, "dice")
                                                }
                                            }
                                        }
                                    }

                                    DataHelper.arrBlackCentered.add(0, dataModel)
                                    preloadFirstCharacterImages()
                                }
                            }
                        }

                        dataReady = true
                        if (minDelayPassed) {
                            navigateToNextScreen()
                        }
                    }

                    LoadingStatus.Error -> {
                        if (DataHelper.arrBlackCentered.isNotEmpty()) {
                            dataReady = true
                            if (minDelayPassed) {
                                navigateToNextScreen()
                            }
                        } else {
                            lifecycleScope.launch(Dispatchers.IO) {
                                delay(2000)
                                getData(apiRepository)
                            }
                        }
                    }

                    else -> {
                        // Loading hoặc trạng thái khác - đợi
                    }
                }
            }
        }
    }


    private fun preloadFirstCharacterImages() {
        val model = DataHelper.arrBlackCentered.firstOrNull() ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            model.bodyPart.forEach { bodyPart ->
                // Chỉ preload ảnh mặc định (index 1 hoặc 0 tùy loại)
                val colorGroup = bodyPart.listPath.getOrNull(0) ?: return@forEach
                val firstPath = colorGroup.listPath.firstOrNull {
                    it != "none" && it != "dice" && it.isNotEmpty()
                } ?: return@forEach

                try {
                    Glide.with(applicationContext)
                        .download(firstPath)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .priority(Priority.HIGH)
                        .preload()
                } catch (e: Exception) {
                    // Bỏ qua lỗi preload
                }
            }
        }
    }
    private fun navigateToNextScreen() {
        // Double-check: CHỈ navigate khi data thực sự sẵn sàng
        if (!dataReady || DataHelper.arrBlackCentered.isEmpty()) {
            return
        }
        if (!sharedPreferenceUtils.getBooleanValue(CONST.LANGUAGE)) {
            startActivity(Intent(this@SplashActivity, LanguageActivity::class.java))
        } else {
            startActivity(Intent(this@SplashActivity, TutorialActivity::class.java))
        }
        finish()
    }

    override fun onBackPressed() {
        // Không cho phép back ở splash
    }
}