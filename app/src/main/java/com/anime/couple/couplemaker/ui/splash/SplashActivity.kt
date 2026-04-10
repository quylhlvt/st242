package com.anime.couple.couplemaker.ui.splash

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.couple.couplemaker.base.AbsBaseActivity
import com.anime.couple.couplemaker.data.callapi.reponse.DataResponse
import com.anime.couple.couplemaker.data.callapi.reponse.LoadingStatus
import com.anime.couple.couplemaker.data.model.BodyPartModel
import com.anime.couple.couplemaker.data.model.ColorModel
import com.anime.couple.couplemaker.data.model.CustomModel
import com.anime.couple.couplemaker.data.repository.ApiRepository
import com.anime.couple.couplemaker.ui.language.LanguageActivity
import com.anime.couple.couplemaker.ui.tutorial.TutorialActivity
import com.anime.couple.couplemaker.utils.CONST
import com.anime.couple.couplemaker.utils.DataHelper
import com.anime.couple.couplemaker.utils.DataHelper.getData
import com.anime.couple.couplemaker.utils.SharedPreferenceUtils
import com.anime.couple.couplemaker.utils.music.MusicLocal
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ActivitySplashBinding
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
                                        if (x10.quantity <= 0) return@forEach

                                        // ✅ Parse x, y, charType từ parts (format: "x-y-charType")
                                        val partSegs = x10.parts.split("-")
                                        val partX = partSegs.getOrNull(0)?.toIntOrNull() ?: 0
                                        val partY = partSegs.getOrNull(1)?.toIntOrNull() ?: 0
                                        val partCharType = partSegs.getOrNull(2)?.toIntOrNull() ?: 1

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
                                            for (i in 1..halfQuantity * 2 + 1) {
                                                thumbList.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/thumb_${i}.png")
                                            }
                                            x10.colorArray.split(",").forEach { color ->
                                                val pathList = arrayListOf<String>()
                                                for (i in 1..x10.quantity) {
                                                    pathList.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${color}/${i}.png")
                                                }
                                                colorList.add(ColorModel(color, pathList))
                                            }
                                        }

                                        bodyPartList.add(
                                            BodyPartModel(
                                                // ✅ icon path dùng parts gốc (có đủ x-y-charType)
                                                "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/${x10.parts}/nav.png",
                                                colorList,
                                                thumbList,
                                                charType = partCharType  // ✅ truyền charType đúng
                                            )
                                        )
                                    }

                                    val dataModel = CustomModel(
                                        "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/avatar.png",
                                        bodyPartList,
                                        true
                                    )

                                    // ✅ Thay thế block forEach mbodyPath cũ
                                    val minYPerCharType = dataModel.bodyPart
                                        .groupBy { it.charType }
                                        .mapValues { (_, parts) ->
                                            parts.mapNotNull { bp ->
                                                bp.icon.substringBeforeLast("/").substringAfterLast("/")
                                                    .split("-").getOrNull(1)?.toIntOrNull()
                                            }.minOrNull() ?: Int.MAX_VALUE
                                        }

                                    dataModel.bodyPart.forEach { mbodyPath ->
                                        val folderY = mbodyPath.icon.substringBeforeLast("/").substringAfterLast("/")
                                            .split("-").getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                                        val minY = minYPerCharType[mbodyPath.charType] ?: Int.MAX_VALUE
                                        val isFirstNav = (folderY == minY)
                                        mbodyPath.listPath.forEach { colorModel ->
                                            if (colorModel.listPath.isEmpty()) return@forEach
                                            if (isFirstNav) {
                                                if (colorModel.listPath[0] != "dice") colorModel.listPath.add(0, "dice")
                                            } else {
                                                if (colorModel.listPath[0] != "none") {
                                                    colorModel.listPath.add(0, "none")
                                                    colorModel.listPath.add(1, "dice")
                                                }
                                            }
                                        }
                                    }

                                    DataHelper.arrBlackCentered.add(0, dataModel)
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