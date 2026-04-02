package com.cute.anime.avatarmaker.ui.randomone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.net.ConnectivityManager
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.data.callapi.reponse.DataResponse
import com.cute.anime.avatarmaker.data.callapi.reponse.LoadingStatus
import com.cute.anime.avatarmaker.data.model.BodyPartModel
import com.cute.anime.avatarmaker.data.model.ColorModel
import com.cute.anime.avatarmaker.data.model.CustomModel
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.databinding.ActivityRandomCatBinding
import com.cute.anime.avatarmaker.dialog.DialogExit
import com.cute.anime.avatarmaker.ui.customview.CustomviewActivity
import com.cute.anime.avatarmaker.utils.CONST
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.isInternetAvailable
import com.cute.anime.avatarmaker.utils.isNetworkConnected
import com.cute.anime.avatarmaker.utils.newIntent
import com.cute.anime.avatarmaker.utils.onSingleClick
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class RandomCatActivity : AbsBaseActivity<ActivityRandomCatBinding>() {
    @Inject
    lateinit var apiRepository: ApiRepository
    private var randomModel: CustomModel? = null
    private var randomCoords: ArrayList<ArrayList<Int>>? = null
    private var listImageSortView: ArrayList<String>? = null
    private var characterBitmap: Bitmap? = null
    private var loadingJob: Job? = null
    override fun getLayoutId(): Int = R.layout.activity_random_cat
    private var checkCallingDataOnline = false
    private var hasShownNoInternetDialog = false

    private val networkReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connectivityManager =
                context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (!checkCallingDataOnline) {
                if (networkInfo != null && networkInfo.isConnected) {
                    var hasOnlineData = false
                    DataHelper.arrBlackCentered.forEach {
                        if (it.checkDataOnline) {
                            hasOnlineData = true
                            return@forEach
                        }
                    }

                    if (!hasOnlineData) {
                        DataHelper.callApi(apiRepository)
                    }
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun initView() {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
        observeDataOnline()

        if (DataHelper.arrBg.isEmpty() || DataHelper.arrBlackCentered.isEmpty()) {
            finish()
            return
        }

        binding.apply {
            tv1.isSelected = true
            imvNext.isEnabled = false
            btnRandomize.isEnabled = false
            imvNext.alpha = 0.5f
            btnRandomize.alpha = 0.5f
        }
        randomizeCharacter()
    }

    private fun observeDataOnline() {
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
                                .toList()
                                .sortedBy { (_, list) ->
                                    list.firstOrNull()?.level ?: Int.MAX_VALUE
                                }
                                .toMap()
                            sortedMap.forEach { key, list ->
                                var a = arrayListOf<BodyPartModel>()
                                list.forEach { x10 ->
                                    // ✅ Skip quantity = 0
                                    if (x10.quantity <= 0) return@forEach

                                    var b = arrayListOf<ColorModel>()
                                    val halfQuantity = maxOf(1, x10.quantity / 2)

                                    x10.colorArray.split(",").forEach { coler ->
                                        var c = arrayListOf<String>()
                                        if (coler == "") {
                                            for (i in 1..halfQuantity) {
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${i}.png")
                                            }
                                            b.add(ColorModel("#", c))
                                        } else {
                                            for (i in 1..halfQuantity) {
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${coler}/${i}.png")
                                            }
                                            b.add(ColorModel(coler, c))
                                        }
                                    }
                                    a.add(
                                        BodyPartModel(
                                            "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/${x10.parts}/nav.png",
                                            b
                                        )
                                    )
                                }

                                var dataModel = CustomModel(
                                    "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/avatar.png",
                                    a,
                                    true
                                )

                                dataModel.bodyPart.forEach { mbodyPath ->
                                    if (mbodyPath.icon.substringBeforeLast("/")
                                            .substringAfterLast("/").substringAfter("-") == "1"
                                    ) {
                                        mbodyPath.listPath.forEach {
                                            // ✅ Check isNotEmpty
                                            if (it.listPath.isNotEmpty() && it.listPath[0] != "dice") {
                                                it.listPath.add(0, "dice")
                                            }
                                        }
                                    } else {
                                        mbodyPath.listPath.forEach {
                                            // ✅ Check isNotEmpty
                                            if (it.listPath.isNotEmpty() && it.listPath[0] != "none") {
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

    private fun randomizeCharacter() {
        loadingJob?.cancel()
        binding.progressBar.visibility = View.GONE
        binding.imgCharacter.visibility = View.VISIBLE
        Glide.with(this@RandomCatActivity)
            .asGif()
            .load(R.drawable.gif)
            .into(binding.imgCharacter)

        loadingJob = lifecycleScope.launch(Dispatchers.Default) {

            val availableModels: List<CustomModel>

            if (!isInternetAvailable(this@RandomCatActivity)) {
                // Không có mạng → load local ngay, không cần dialog
                availableModels = DataHelper.arrBlackCentered.filter { !it.checkDataOnline }
            } else {
                // Có mạng → ping thực tế, timeout 10s
                val hasRealInternet = withContext(Dispatchers.IO) {
                    try {
                        withTimeout(10_000L) {
                            isNetworkConnected(this@RandomCatActivity)
                        }
                    } catch (e: TimeoutCancellationException) {
                        false
                    }
                }

                if (!hasRealInternet) {
                    if (!hasShownNoInternetDialog) {
                        hasShownNoInternetDialog = true
                        withContext(Dispatchers.Main) {
                            val dialog = DialogExit(this@RandomCatActivity, "networked")
                            dialog.show()
                        }
                    }
                    availableModels = DataHelper.arrBlackCentered.filter { !it.checkDataOnline }
                }else{
                    availableModels = DataHelper.arrBlackCentered

                }

            }

            if (availableModels.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
                return@launch
            }

            randomModel = availableModels.random()

            randomModel?.let { model ->
                val list = ArrayList<String>().apply {
                    repeat(model.bodyPart.size) { add("") }
                }

                model.bodyPart.forEach {
                    val (x, _) = it.icon.substringBeforeLast("/")
                        .substringAfterLast("/")
                        .split("-")
                        .map { it.toInt() }
                    if (x - 1 < list.size) list[x - 1] = it.icon  // ✅ bounds check
                }
                listImageSortView = list

                val coords = arrayListOf<ArrayList<Int>>()
                list.forEach { data ->
                    val bodyPart = model.bodyPart.find { it.icon == data }
                    val pair = if (bodyPart != null) {
                        val path = bodyPart.listPath[0].listPath
                        val color = bodyPart.listPath

                        val validIndices = path.indices.filter { idx ->
                            val value = path[idx]
                            value != "none" && value != "dice"
                        }

                        val randomValue = if (validIndices.isNotEmpty()) {
                            validIndices.random()
                        } else {
                            // ✅ guard isNotEmpty
                            if (path.isEmpty()) 1
                            else if (path[0] == "none") 2 else 1
                        }

                        val randomColor = (0 until color.size).random()
                        arrayListOf(randomValue, randomColor)
                    } else {
                        arrayListOf(-1, -1)
                    }
                    coords.add(pair)
                }
                randomCoords = coords

                loadCharacterBitmap(model, list, coords)
            }
        }
    }


    private suspend fun loadCharacterBitmap(
        model: CustomModel,
        imageList: ArrayList<String>,
        coords: ArrayList<ArrayList<Int>>
    ) = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.imgCharacter.visibility = View.VISIBLE
                Glide.with(this@RandomCatActivity)
                    .asGif()
                    .load(R.drawable.gif)
                    .into(binding.imgCharacter)
            }

            // Tính size
            val targetSize = calculateTargetSize(model, imageList, coords)
            val width = targetSize.first
            val height = targetSize.second

            // Tạo bitmap tổng
            val merged = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(merged)
            val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

            var layerCount = 0

            // Load và vẽ TẤT CẢ layer trước khi hiển thị
            imageList.forEachIndexed { index, icon ->
                if (icon.isEmpty()) {
                    return@forEachIndexed
                }

                val coord = coords.getOrNull(index) ?: return@forEachIndexed

                if (coord[0] >= 0) {
                    val bodyPart = model.bodyPart.find { it.icon == icon }
                    val targetPath = bodyPart
                        ?.listPath?.getOrNull(coord[1])
                        ?.listPath?.getOrNull(coord[0])

                    if (!targetPath.isNullOrEmpty() && targetPath != "none" && targetPath != "dice") {
                        try {
                            val bmp = Glide.with(this@RandomCatActivity)
                                .asBitmap()
                                .load(targetPath)
                                .override(width, height)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .submit()
                                .get()

                            val srcRect = Rect(0, 0, bmp.width, bmp.height)
                            canvas.drawBitmap(bmp, srcRect, dstRect, null)
                            layerCount++

                            Log.d("RandomCat", "Drew layer $index successfully")
                        } catch (e: Exception) {
                            Log.e("RandomCat", "Error loading layer $index: ${e.message}")
                        }
                    }
                }
            }

            Log.d("RandomCat", "Total layers drawn: $layerCount")

            // FIX: Chỉ hiển thị nếu có ít nhất 1 layer được vẽ thành công
            if (layerCount > 0) {
                characterBitmap?.recycle()
                characterBitmap = merged

                withContext(Dispatchers.Main) {
                    // QUAN TRỌNG: Clear Glide trước để dừng gif
                    Glide.with(this@RandomCatActivity).clear(binding.imgCharacter)
                    binding.imgCharacter.setImageBitmap(merged)
                    binding.apply {
                        imvNext.isEnabled = true
                        btnRandomize.isEnabled = true
                        imvNext.alpha = 1f
                        btnRandomize.alpha = 1f
                    }
                }
            } else {
                // Không có layer nào -> hiển thị placeholder hoặc thông báo lỗi
                merged.recycle()
                withContext(Dispatchers.Main) {
                    Glide.with(this@RandomCatActivity).clear(binding.imgCharacter)
                    binding.imgCharacter.setImageResource(R.drawable.img_random_btn_randomone)
                    binding.apply {
                        imvNext.isEnabled = false
                        btnRandomize.isEnabled = true
                        imvNext.alpha = 0.5f
                        btnRandomize.alpha = 1f
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RandomCat", "Error in loadCharacterBitmap: ${e.message}")
            withContext(Dispatchers.Main) {
                Glide.with(this@RandomCatActivity).clear(binding.imgCharacter)
                binding.progressBar.visibility = View.GONE
                binding.imgCharacter.visibility = View.VISIBLE
                binding.imgCharacter.setImageResource(R.drawable.img_random_btn_randomone)
            }
        }
    }

    private suspend fun calculateTargetSize(
        model: CustomModel,
        listImageSortView: List<String>,
        coordSet: ArrayList<ArrayList<Int>>
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            for (index in listImageSortView.indices) {
                val icon = listImageSortView[index]
                val coord = coordSet[index]

                // FIX: Thay đổi điều kiện để khớp với loadCharacterBitmap
                if (coord[0] > -1 && coord[1] > -1) {
                    val targetPath = model.bodyPart
                        .find { it.icon == icon }
                        ?.listPath?.getOrNull(coord[1])
                        ?.listPath?.getOrNull(coord[0])
                    if (!targetPath.isNullOrEmpty() && targetPath != "none" && targetPath != "dice") {
                        val bmp = Glide.with(this@RandomCatActivity)
                            .asBitmap()
                            .load(targetPath)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .submit()
                            .get()
                        val size = Pair(bmp.width / 2, bmp.height / 2)
                        bmp.recycle()
                        return@withContext size
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext Pair(512, 512)
    }

    override fun initAction() {
        binding.apply {
            imvBack.onSingleClick { finish() }

            btnRandomize.onSingleClick {
                imvNext.isEnabled = false
                btnRandomize.isEnabled = false
                imvNext.alpha = 0.5f
                btnRandomize.alpha = 0.5f

                imgCharacter.setImageBitmap(null)
                progressBar.visibility = View.VISIBLE
                imgCharacter.visibility = View.GONE
                Glide.with(this@RandomCatActivity)
                    .asGif()
                    .load(R.drawable.gif)
                    .into(imgCharacter)

                characterBitmap?.recycle()
                characterBitmap = null

                randomizeCharacter()
            }

            imvNext.onSingleClick {
                randomModel?.let { model ->
                    if (!isInternetAvailable(this@RandomCatActivity) && model.checkDataOnline) {
                        DialogExit(this@RandomCatActivity, "network").show()
                        return@onSingleClick
                    } else {
                        lifecycleScope.launch {
                            val hasInternet = withContext(Dispatchers.IO) {
                                isNetworkConnected(this@RandomCatActivity)
                            }
                            if (!hasInternet&& model.checkDataOnline) {
                                DialogExit(this@RandomCatActivity, "networked").show()
                            } else {
                                val index = DataHelper.arrBlackCentered.indexOf(model)
                                if (index != -1) {
                                    startActivity(
                                        newIntent(
                                            this@RandomCatActivity,
                                            CustomviewActivity::class.java
                                        )
                                            .putExtra("data", index)
                                            .putExtra("arr", randomCoords)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        unregisterReceiver(networkReceiver)
//        loadingJob?.cancel()
//        characterBitmap?.recycle()
//        characterBitmap = null
//    }
}