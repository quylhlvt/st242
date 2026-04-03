package com.cute.anime.avatarmaker.ui.cosplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.ConnectivityManager
import android.text.TextUtils
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
import com.cute.anime.avatarmaker.databinding.ActivityCosplayBinding
import com.cute.anime.avatarmaker.dialog.DialogExit
import com.cute.anime.avatarmaker.ui.show.ShowActivity
import com.cute.anime.avatarmaker.utils.CONST
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.changeText
import com.cute.anime.avatarmaker.utils.fromList
import com.cute.anime.avatarmaker.utils.hide
import com.cute.anime.avatarmaker.utils.isInternetAvailable
import com.cute.anime.avatarmaker.utils.isNetworkConnected
import com.cute.anime.avatarmaker.utils.newIntent
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class CosplayActivity : AbsBaseActivity<ActivityCosplayBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_cosplay

    @Inject
    lateinit var apiRepository: ApiRepository
    private var randomModel: CustomModel? = null
    private var randomCoords: ArrayList<ArrayList<Int>>? = null
    private var characterBitmap: Bitmap? = null
    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null
    private var loadingJob: Job? = null
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
                        if (it.checkDataOnline) { hasOnlineData = true; return@forEach }
                    }
                    if (!hasOnlineData) DataHelper.callApi(apiRepository)
                }
            }
        }
    }

    override fun onRestart() { super.onRestart() }

    override fun initView() {
        val space = " "
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
        observeDataOnline()

        if (DataHelper.arrBg.isEmpty() || DataHelper.arrBlackCentered.isEmpty()) {
            finish(); return
        }

        binding.apply {
            tv1.isSelected = true
            tv2.isSelected = true
            txtTitle.isSelected = true
            btnCosplay.isEnabled = false
            btnRandomize.isEnabled = false
            btnCosplay.alpha = 0.5f
            btnRandomize.alpha = 0.5f
            txtContentCosplay.text = TextUtils.concat(
                changeText(this@CosplayActivity, getString(R.string.tvCosplay1), R.color.app_color, R.font.hvd_comic_serif_pro), space,
                changeText(this@CosplayActivity, getString(R.string.tvCosplay2), R.color.white, R.font.hvd_comic_serif_pro), space,
                changeText(this@CosplayActivity, getString(R.string.tvCosplay3), R.color.app_color, R.font.hvd_comic_serif_pro), space,
                changeText(this@CosplayActivity, getString(R.string.tvCosplay4), R.color.white, R.font.hvd_comic_serif_pro), space,
                changeText(this@CosplayActivity, getString(R.string.tvCosplay5), R.color.app_color, R.font.hvd_comic_serif_pro), space,
                changeText(this@CosplayActivity, getString(R.string.tvCosplay6), R.color.white, R.font.hvd_comic_serif_pro), space,
                changeText(this@CosplayActivity, getString(R.string.tvCosplay7), R.color.app_color, R.font.hvd_comic_serif_pro), space,
                changeText(this@CosplayActivity, getString(R.string.tvCosplay8), R.color.white, R.font.hvd_comic_serif_pro)
            )
        }
        randomizeCharacter()
    }

    private fun observeDataOnline() {
        DataHelper.arrDataOnline.observe(this) {
            it?.let {
                when (it.loadingStatus) {
                    LoadingStatus.Loading -> { checkCallingDataOnline = true }
                    LoadingStatus.Success -> {
                        if (DataHelper.arrBlackCentered.isNotEmpty() && !DataHelper.arrBlackCentered[0].checkDataOnline) {
                            checkCallingDataOnline = false
                            val listA = (it as DataResponse.DataSuccess).body ?: return@observe
                            checkCallingDataOnline = true
                            val sortedMap = listA.toList()
                                .sortedBy { (_, list) -> list.firstOrNull()?.level ?: Int.MAX_VALUE }
                                .toMap()
                            sortedMap.forEach { key, list ->
                                val a = arrayListOf<BodyPartModel>()
                                list.forEach { x10 ->
                                    if (x10.quantity <= 0) return@forEach
                                    val b = arrayListOf<ColorModel>()
                                    val halfQuantity = maxOf(1, x10.quantity / 2)
                                    x10.colorArray.split(",").forEach { coler ->
                                        val c = arrayListOf<String>()
                                        if (coler == "") {
                                            for (i in 1..halfQuantity)
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${i}.png")
                                            b.add(ColorModel("#", c))
                                        } else {
                                            for (i in 1..halfQuantity)
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${coler}/${i}.png")
                                            b.add(ColorModel(coler, c))
                                        }
                                    }
                                    a.add(BodyPartModel("${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/${x10.parts}/nav.png", b))
                                }
                                val dataModel = CustomModel("${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/avatar.png", a, true)
                                dataModel.bodyPart.forEach { mbodyPath ->
                                    if (mbodyPath.icon.substringBeforeLast("/").substringAfterLast("/").substringAfter("-") == "1") {
                                        mbodyPath.listPath.forEach {
                                            if (it.listPath.isNotEmpty() && it.listPath[0] != "dice") it.listPath.add(0, "dice")
                                        }
                                    } else {
                                        mbodyPath.listPath.forEach {
                                            if (it.listPath.isNotEmpty() && it.listPath[0] != "none") {
                                                it.listPath.add(0, "none"); it.listPath.add(1, "dice")
                                            }
                                        }
                                    }
                                }
                                DataHelper.arrBlackCentered.add(0, dataModel)
                            }
                        }
                        checkCallingDataOnline = false
                    }
                    LoadingStatus.Error -> { checkCallingDataOnline = false }
                    else -> { checkCallingDataOnline = true }
                }
            }
        }
    }

    // ── Data class ───────────────────────────────────────────────────────────
    private data class PartEntry(val bp: BodyPartModel, val x: Int, val y: Int)

    // ── Build coords + iconToCoord + layerOrder (y hệt RandomCatActivity) ───
    private fun buildCharData(
        parts: List<BodyPartModel>
    ): Triple<ArrayList<ArrayList<Int>>, Map<String, ArrayList<Int>>, List<BodyPartModel>> {

        val entries = parts.mapNotNull { bp ->
            val segs = bp.icon.substringBeforeLast("/").substringAfterLast("/").split("-")
            val x = segs.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val y = segs.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            PartEntry(bp, x, y)
        }

        val xToLocal = entries.map { it.x }.distinct().sorted()
            .mapIndexed { i, v -> v to i }.toMap()

        val layerArr = Array<BodyPartModel?>(entries.size) { null }
        entries.forEach { e -> xToLocal[e.x]?.let { if (it < layerArr.size) layerArr[it] = e.bp } }
        val layerOrder = layerArr.filterNotNull()

        val coords = arrayListOf<ArrayList<Int>>()
        val iconToCoord = mutableMapOf<String, ArrayList<Int>>()

        entries.sortedBy { it.y }.forEach { entry ->
            val bp = entry.bp
            val colorList = bp.listPath
            if (colorList.isEmpty()) {
                val c = arrayListOf(1, 0)
                coords.add(c); iconToCoord[bp.icon] = c; return@forEach
            }
            val randomColor = colorList.indices.random()
            val pathList = colorList[randomColor].listPath
            val validIndices = pathList.indices.filter {
                pathList[it] != "none" && pathList[it] != "dice" && pathList[it].isNotEmpty()
            }
            val randomValue = when {
                validIndices.isNotEmpty() -> validIndices.random()
                pathList.isEmpty() -> 1
                pathList[0] == "none" -> 2
                else -> 1
            }
            val c = arrayListOf(randomValue, randomColor)
            coords.add(c); iconToCoord[bp.icon] = c
        }

        return Triple(coords, iconToCoord, layerOrder)
    }
    private var retryCount = 0
    private val MAX_RETRY = 3

    private fun handleError() {
        if (retryCount < MAX_RETRY) {
            retryCount++
            randomizeCharacter()
        } else {
            retryCount = 0
            Glide.with(this@CosplayActivity).clear(binding.imgCharacter)
            binding.imgCharacter.setImageResource(R.drawable.img_random_btn_randomone)
            binding.btnRandomize.isEnabled = true; binding.btnRandomize.alpha = 1f
            binding.btnCosplay.isEnabled = false; binding.btnCosplay.alpha = 0.5f
        }
    }
    // ── Randomize ────────────────────────────────────────────────────────────
    private fun randomizeCharacter() {
        loadingJob?.cancel()
        bitmap1?.recycle(); bitmap1 = null
        bitmap2?.recycle(); bitmap2 = null
        characterBitmap = null
        binding.imgCharacter.visibility = View.VISIBLE
        Glide.with(this@CosplayActivity).asGif().load(R.drawable.gif).into(binding.imgCharacter)

        loadingJob = lifecycleScope.launch(Dispatchers.Default) {
            kotlinx.coroutines.yield()
            if (!isActive) return@launch

            val availableModels: List<CustomModel> = if (!isInternetAvailable(this@CosplayActivity)) {
                DataHelper.arrBlackCentered.filter { !it.checkDataOnline }
            } else {
                val hasRealInternet = withContext(Dispatchers.IO) {
                    try { withTimeout(10_000L) { isNetworkConnected(this@CosplayActivity) } }
                    catch (e: TimeoutCancellationException) { false }
                }
                if (!isActive) return@launch
                if (!hasRealInternet) {
                    if (!hasShownNoInternetDialog) {
                        hasShownNoInternetDialog = true
                        withContext(Dispatchers.Main) { DialogExit(this@CosplayActivity, "networked").show() }
                    }
                    DataHelper.arrBlackCentered.filter { !it.checkDataOnline }
                } else {
                    DataHelper.arrBlackCentered
                }
            }

            if (!isActive || availableModels.isEmpty()) return@launch

            randomModel = availableModels.random()
            randomModel?.let { model ->
                val bodyParts1 = model.bodyPart.filter { it.charType == 1 }
                val bodyParts2 = model.bodyPart.filter { it.charType == 2 }

                val (coords1, iconToCoord1, layerOrder1) = buildCharData(bodyParts1)
                val (coords2, iconToCoord2, layerOrder2) = buildCharData(bodyParts2)

                if (!isActive) return@launch

                randomCoords = ArrayList<ArrayList<Int>>().apply {
                    addAll(coords1); add(arrayListOf(-1, -1)); addAll(coords2)
                }

                loadCharacterBitmap(layerOrder1, iconToCoord1, layerOrder2, iconToCoord2)
            }
        }
    }

    // ── Load & draw ──────────────────────────────────────────────────────────
    private suspend fun loadCharacterBitmap(
        layerOrder1: List<BodyPartModel>,
        iconToCoord1: Map<String, ArrayList<Int>>,
        layerOrder2: List<BodyPartModel>,
        iconToCoord2: Map<String, ArrayList<Int>>
    ) = withContext(Dispatchers.IO) {

        fun collectPaths(layers: List<BodyPartModel>, coordMap: Map<String, ArrayList<Int>>): List<String> =
            layers.mapNotNull { bp ->
                val coord = coordMap[bp.icon] ?: return@mapNotNull null
                val path = bp.listPath.getOrNull(coord[1])?.listPath?.getOrNull(coord[0]) ?: return@mapNotNull null
                if (path == "none" || path == "dice" || path.isEmpty()) null else path
            }

        val paths1 = collectPaths(layerOrder1, iconToCoord1)
        val paths2 = collectPaths(layerOrder2, iconToCoord2)

        if ((paths1.isEmpty() && paths2.isEmpty()) || !isActive) {
            withContext(Dispatchers.Main) { handleError() }
            return@withContext
        }

        try {
            var width = 512; var height = 512

            // ── Load char1 ──
            val loaded1 = mutableListOf<Bitmap>()
            for (path in paths1) {
                if (!isActive) { loaded1.forEach { it.recycle() }; return@withContext }
                try {
                    val bmp = Glide.with(this@CosplayActivity).asBitmap().load(path)
                        .diskCacheStrategy(DiskCacheStrategy.ALL).submit().get()
                    if (loaded1.isEmpty()) { width = bmp.width / 2; height = bmp.height / 2 }
                    loaded1.add(bmp)
                } catch (e: Exception) { Log.e("Cosplay", "load1 $path: ${e.message}") }
            }

            // ── Load char2 ──
            val loaded2 = mutableListOf<Bitmap>()
            for (path in paths2) {
                if (!isActive) {
                    loaded1.forEach { it.recycle() }; loaded2.forEach { it.recycle() }
                    return@withContext
                }
                try {
                    val bmp = Glide.with(this@CosplayActivity).asBitmap().load(path)
                        .diskCacheStrategy(DiskCacheStrategy.ALL).submit().get()
                    if (loaded1.isEmpty() && loaded2.isEmpty()) { width = bmp.width / 2; height = bmp.height / 2 }
                    loaded2.add(bmp)
                } catch (e: Exception) { Log.e("Cosplay", "load2 $path: ${e.message}") }
            }

            if (!isActive || (loaded1.isEmpty() && loaded2.isEmpty())) {
                loaded1.forEach { it.recycle() }; loaded2.forEach { it.recycle() }
                withContext(Dispatchers.Main) { handleError() }
                return@withContext
            }

            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

            // ── Vẽ merged1 (char1) ──
            val merged1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(merged1).let { c -> loaded1.forEach { bmp -> c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dstRect, paint) } }
            loaded1.forEach { it.recycle() }; loaded1.clear()

            // ── Vẽ merged2 (char2) ──
            val merged2 = if (loaded2.isNotEmpty()) {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { m ->
                    Canvas(m).let { c -> loaded2.forEach { bmp -> c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dstRect, paint) } }
                    loaded2.forEach { it.recycle() }; loaded2.clear()
                }
            } else null

            // ── Gộp char1 + char2 thành 1 bitmap preview ──
            val mergedPreview = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(mergedPreview).let { c ->
                c.drawBitmap(merged1, Rect(0, 0, merged1.width, merged1.height), dstRect, paint)
                merged2?.let { c.drawBitmap(it, Rect(0, 0, it.width, it.height), dstRect, paint) }
            }

            if (!isActive) { merged1.recycle(); merged2?.recycle(); mergedPreview.recycle(); return@withContext }

            bitmap1 = merged1
            bitmap2 = merged2
            characterBitmap = mergedPreview
            retryCount = 0
            withContext(Dispatchers.Main) {
                Glide.with(this@CosplayActivity).clear(binding.imgCharacter)
                binding.imgCharacter.setImageBitmap(mergedPreview)
                binding.btnCosplay.isEnabled = true; binding.btnCosplay.alpha = 1f
                binding.btnRandomize.isEnabled = true; binding.btnRandomize.alpha = 1f
            }

        } catch (e: Exception) {
            if (!isActive) return@withContext
            e.printStackTrace()
            withContext(Dispatchers.Main) { handleError() }
        }
    }


    // ── Actions ──────────────────────────────────────────────────────────────
    override fun initAction() {
        binding.apply {
            imvBack.onSingleClick { finish() }

            btnRandomize.onSingleClick {
                retryCount = 0
                btnCosplay.isEnabled = false; btnRandomize.isEnabled = false
                btnCosplay.alpha = 0.5f; btnRandomize.alpha = 0.5f
                bitmap1?.recycle(); bitmap1 = null
                bitmap2?.recycle(); bitmap2 = null
                characterBitmap = null
                imgCharacter.setImageBitmap(null)
                imgCharacter.visibility = View.VISIBLE
                Glide.with(this@CosplayActivity).asGif().load(R.drawable.gif).into(imgCharacter)
                randomizeCharacter()
            }

            btnCancel.onSingleClick {
                dialogCoplay.hide()
                dialogCoplay.setOnTouchListener(null)
            }

            imvGuide.onSingleClick {
                dialogCoplay.setOnTouchListener { _, _ -> true }
                dialogCoplay.show()
            }

            btnCosplay.onSingleClick {
                randomModel?.let { model ->
                    if (!isInternetAvailable(this@CosplayActivity) && model.checkDataOnline) {
                        DialogExit(this@CosplayActivity, "network").show()
                        return@onSingleClick
                    }
                    val index = DataHelper.arrBlackCentered.indexOf(model)
                    if (index == -1) return@onSingleClick

                    lifecycleScope.launch {
                        val hasInternet = withContext(Dispatchers.IO) { isNetworkConnected(this@CosplayActivity) }
                        if (!hasInternet && model.checkDataOnline) {
                            DialogExit(this@CosplayActivity, "networked").show()
                            return@launch
                        }
                        try {
                            // Xóa cache cũ
                            cacheDir.listFiles { f -> f.name.startsWith("cosplay_preview_") }?.forEach { it.delete() }

// Lưu characterBitmap (char1 + char2 đã gộp) thay vì bitmap1 riêng lẻ
                            val file1 = java.io.File(cacheDir, "cosplay_preview_1_${System.currentTimeMillis()}.png")
                            characterBitmap?.let { bmp ->
                                java.io.FileOutputStream(file1).use { fos ->
                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.flush()
                                }
                            }


                            startActivity(
                                newIntent(this@CosplayActivity, ShowActivity::class.java)
                                    .putExtra("data", index)
                                    .putExtra("imgCoslay", file1.absolutePath)   // char1
                                    .putExtra("arr", fromList(randomCoords!!))
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            startActivity(
                                newIntent(this@CosplayActivity, ShowActivity::class.java)
                                    .putExtra("data", index)
                                    .putExtra("arr", fromList(randomCoords!!))
                            )
                        }
                    }
                }
            }
        }
    }
}