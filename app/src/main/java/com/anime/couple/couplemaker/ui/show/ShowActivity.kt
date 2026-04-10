package com.anime.couple.couplemaker.ui.show

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.base.AbsBaseActivity
import com.anime.couple.couplemaker.data.model.BodyPartModel
import com.anime.couple.couplemaker.databinding.ActivityShowBinding
import com.anime.couple.couplemaker.dialog.DialogExit
import com.anime.couple.couplemaker.ui.customview.ColorAdapter
import com.anime.couple.couplemaker.ui.customview.CustomviewViewModel
import com.anime.couple.couplemaker.ui.customview.NavAdapter
import com.anime.couple.couplemaker.ui.customview.PartAdapter
import com.anime.couple.couplemaker.ui.successcoslay.SuccessCosplayActivity
import com.anime.couple.couplemaker.utils.DataHelper
import com.anime.couple.couplemaker.utils.inhide
import com.anime.couple.couplemaker.utils.isInternetAvailable
import com.anime.couple.couplemaker.utils.isNetworkConnected
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.utils.show
import com.anime.couple.couplemaker.utils.showToast
import com.anime.couple.couplemaker.utils.toList
import com.anime.couple.couplemaker.utils.viewToBitmap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@AndroidEntryPoint
class ShowActivity : AbsBaseActivity<ActivityShowBinding>() {

    val viewModel: CustomviewViewModel by viewModels()

    // ── Adapters ──────────────────────────────────────────────────────────────
    val adapterNav   by lazy { NavAdapter(this) }
    val adapterColor by lazy { ColorAdapter() }
    val adapterPart  by lazy { PartAdapter() }
    val adapterNav2   by lazy { NavAdapter(this) }
    val adapterColor2 by lazy { ColorAdapter() }
    val adapterPart2  by lazy { PartAdapter() }

    // ── Char data ─────────────────────────────────────────────────────────────
    var listData  = arrayListOf<BodyPartModel>()
    var listData2 = arrayListOf<BodyPartModel>()
    var arrInt    = arrayListOf<ArrayList<Int>>()   // [partIdx, colorIdx]
    var arrInt2   = arrayListOf<ArrayList<Int>>()
    var arrShowColor  = arrayListOf<Boolean>()
    var arrShowColor2 = arrayListOf<Boolean>()
    var listImg   = arrayListOf<AppCompatImageView>()
    var listImg2  = arrayListOf<AppCompatImageView>()
    private val iconToIndexMap  = mutableMapOf<String, Int>()
    private val iconToIndexMap2 = mutableMapOf<String, Int>()

    // ── Original paths (snapshot từ Cosplay để tính %) ────────────────────────
    private var originalPaths : ArrayList<String>? = null
    private var originalPaths2: ArrayList<String>? = null

    // ── State ─────────────────────────────────────────────────────────────────
    var blackCentered = 0
    var editingChar   = 1
    var checkRevert   = true
    var countRandom   = 0
    private var imgCoslay = ""
    private var canSave   = true
    private var arrIntHottrend : ArrayList<ArrayList<Int>>? = null
    private var arrIntHottrend2: ArrayList<ArrayList<Int>>? = null

    private val loadingLock = Any()
    private var loadingCount = 0

    override fun getLayoutId() = R.layout.activity_show
    // ── Timer ──────────────────────────────────────────────────────────────────
    private var countDownTimer: android.os.CountDownTimer? = null
    companion object {
        const val REQUEST_COSPLAY = 1001
    }
    private var remainingTimeMs = 300_000L

    private fun startCountdown(timeMs: Long = remainingTimeMs) {
        countDownTimer?.cancel()
        countDownTimer = object : android.os.CountDownTimer(timeMs, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMs = millisUntilFinished
                val seconds = millisUntilFinished / 1000
                binding.countdownTimer.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            }
            override fun onFinish() {
                remainingTimeMs = 0
                binding.countdownTimer.text = "00:00"
                if (!canSave) return
                binding.llLoading.visibility = View.VISIBLE
                val bmp  = viewToBitmap(binding.rl)
                val file = java.io.File(cacheDir, "show_preview_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(file).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.flush()
                }
                binding.llLoading.visibility = View.GONE
                startActivityForResult(
                    Intent(this@ShowActivity, SuccessCosplayActivity::class.java)
                        .putExtra("cosplayBitmapPath", imgCoslay)
                        .putExtra("currentBitmapPath", file.absolutePath)
                        .putExtra("matchPercent", calculateMatchPercent()),
                    REQUEST_COSPLAY
                )
            }
        }.start()
    }
    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (remainingTimeMs in 1..<300_000L && canSave) {
            startCountdown(remainingTimeMs)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_COSPLAY && resultCode == RESULT_OK) {
            countDownTimer?.cancel()
            remainingTimeMs = 300_000L

            // ── Reset arrShowColor ──
            arrShowColor.fill(true)
            arrShowColor2.fill(true)

            // ── Reset char1 ──
            listData.forEach { putImage(it.icon, 0, true) }
            arrInt.forEachIndexed { i, arr ->
                arr[0] = if (i == 0) 1 else 0; arr[1] = 0
            }

            // ── Reset char2 ──
            listData2.forEach { putImage2(it.icon, 0, true) }
            arrInt2.forEachIndexed { i, arr ->
                arr[0] = if (i == 0) 1 else 0; arr[1] = 0
            }

            // ── Reset nav về 0 rồi switch về char1 ──
            adapterNav.posNav  = 0
            adapterNav2.posNav = 0
            switchToChar(1)

            // ── Render lại char1 (tất cả part) ──
            listData.forEachIndexed { i, bp ->
                putImage(bp.icon, arrInt[i][0], false, i, arrInt[i][1])
            }

            // ── Render lại char2 (tất cả part) ──
            listData2.forEachIndexed { i, bp ->
                putImage2(bp.icon, arrInt2[i][0], false, i, arrInt2[i][1])
            }

            updateMatchUI()

            canSave = true
            binding.btnSave.alpha = 1f
            startCountdown()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer = null
    }
    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────
    override fun initView() {
        imgCoslay = intent.getStringExtra("imgCoslay").orEmpty()
        val isFlipped = intent.getBooleanExtra("isFlipped", false)

        // Ảnh cosplay thumbnail
        if (imgCoslay.isNotEmpty()) {
            val f = java.io.File(imgCoslay)
            Glide.with(this).load(f).diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true).into(binding.imgCharacter1)
            Glide.with(this).load(f).diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true).into(binding.imgCharacter2Show)
        }

        binding.btnSave.isSelected = true
        binding.imvBack.isSelected = true
        binding.txtTitle.post { binding.txtTitle.isSelected = true }

        if (DataHelper.arrBlackCentered.isEmpty()) { finish(); return }

        // Gắn adapter
        binding.rcvNav.adapter   = adapterNav;   binding.rcvNav.itemAnimator   = null
        binding.rcvColor.adapter = adapterColor; binding.rcvColor.itemAnimator = null
        binding.rcvPart.adapter  = adapterPart;  binding.rcvPart.itemAnimator  = null
        binding.rcvNav2.adapter   = adapterNav2;   binding.rcvNav2.itemAnimator   = null
        binding.rcvColor2.adapter = adapterColor2; binding.rcvColor2.itemAnimator = null
        binding.rcvPart2.adapter  = adapterPart2;  binding.rcvPart2.itemAnimator  = null

        // Load data
        loadData()

        // Flip
        if (isFlipped) {
            checkRevert = false
            listImg.forEach  { it.scaleX = -1f }
            listImg2.forEach { it.scaleX = -1f }
        }

        // Init UI adapter
        adapterNav.posNav  = 0
        adapterNav2.posNav = 0
        switchToChar(1)

        // Render ảnh ban đầu

        // Show loading
        binding.llLoading.visibility = View.VISIBLE
        canSave = false; binding.btnSave.alpha = 0.5f

        preloadImages {
            binding.llLoading.visibility = View.GONE
            canSave = true; binding.btnSave.alpha = 1f
            binding.root.post { updateMatchUI() }
            startCountdown()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD DATA
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadData() {
        // Reset
        binding.rl.removeAllViews()
        listData.clear();  arrInt.clear();  arrShowColor.clear();  iconToIndexMap.clear()
        listData2.clear(); arrInt2.clear(); arrShowColor2.clear(); iconToIndexMap2.clear()
        listImg.clear();   listImg2.clear()
        checkRevert = true
        originalPaths  = null
        originalPaths2 = null

        blackCentered = intent.getIntExtra("data", 0)

        // Parse arr từ intent
        val rawArr: ArrayList<ArrayList<Int>>? = try {
            intent.getStringExtra("arr")?.let { toList(it) }
                ?: (intent.getSerializableExtra("arr") as? ArrayList<ArrayList<Int>>)
        } catch (e: Exception) {
            intent.getSerializableExtra("arr") as? ArrayList<ArrayList<Int>>
        }

        // Tách coords char1 và char2 (ngăn cách bởi [-1,-1])
        arrIntHottrend  = null
        arrIntHottrend2 = null
        if (rawArr != null) {
            val sep = rawArr.indexOfFirst { it.size == 2 && it[0] == -1 && it[1] == -1 }
            if (sep >= 0) {
                arrIntHottrend  = ArrayList(rawArr.subList(0, sep))
                arrIntHottrend2 = ArrayList(rawArr.subList(sep + 1, rawArr.size))
            } else {
                arrIntHottrend = rawArr
            }
        }

        val cat = DataHelper.arrBlackCentered.getOrNull(blackCentered) ?: return

        // Build char data
        buildCharData(cat.bodyPart.filter { it.charType == 1 },
            listData, arrInt, arrShowColor, iconToIndexMap, listImg)
        buildCharData(cat.bodyPart.filter { it.charType == 2 },
            listData2, arrInt2, arrShowColor2, iconToIndexMap2, listImg2)

        // Apply hottrend coords + build originalPaths
        applyHottrendAndBuildOriginal(
            cat.bodyPart.filter { it.charType == 1 },
            arrIntHottrend, listData, arrInt, iconToIndexMap,
            onOriginal = { originalPaths = it }
        )
        applyHottrendAndBuildOriginal(
            cat.bodyPart.filter { it.charType == 2 },
            arrIntHottrend2, listData2, arrInt2, iconToIndexMap2,
            onOriginal = { originalPaths2 = it }
        )
    }

    /**
     * Từ danh sách bodyParts + hottrend coords (sort by y, giống CosplayActivity):
     * 1. Map icon → coord gốc
     * 2. Build originalPaths (snapshot để so sánh %)
     * 3. Apply coord vào arrInt để hiển thị nhân vật đúng trạng thái cosplay
     */
    private fun applyHottrendAndBuildOriginal(
        bodyParts: List<BodyPartModel>,
        hottrend: ArrayList<ArrayList<Int>>?,
        targetData: ArrayList<BodyPartModel>,
        targetArr: ArrayList<ArrayList<Int>>,
        iconMap: Map<String, Int>,
        onOriginal: (ArrayList<String>) -> Unit
    ) {
        if (hottrend == null) return

        // Sort by y → đúng thứ tự CosplayActivity.buildCharData
        val sortedByY = bodyParts.mapNotNull { bp ->
            val segs = bp.icon.substringBeforeLast("/").substringAfterLast("/").split("-")
            val y = segs.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            bp to y
        }.sortedBy { it.second }

        // icon → coord gốc
        val iconToCoord = mutableMapOf<String, ArrayList<Int>>()
        sortedByY.forEachIndexed { idx, (bp, _) ->
            hottrend.getOrNull(idx)?.let { iconToCoord[bp.icon] = it }
        }

        // Build originalPaths theo viewIndex (sort by x) — để tính match %
        val maxViewIndex = iconMap.values.maxOrNull() ?: -1
        val paths = ArrayList<String>(maxViewIndex + 1).apply {
            repeat(maxViewIndex + 1) { add("") }
        }
        targetData.forEach { bp ->
            val viewIdx = iconMap[bp.icon] ?: return@forEach
            val coord   = iconToCoord[bp.icon] ?: return@forEach
            val path    = bp.listPath.getOrNull(coord.getOrNull(1) ?: return@forEach)
                ?.listPath?.getOrNull(coord.getOrNull(0) ?: return@forEach)
                ?.takeIf { it != "none" && it != "dice" && it.isNotEmpty() } ?: return@forEach
            if (viewIdx < paths.size) paths[viewIdx] = path
        }
        onOriginal(paths)

        // ✅ KHÔNG apply coord vào arrInt — nhân vật hiển thị mặc định như tạo mới
    }

    /**
     * Build listData, arrInt, iconMap, listImg từ bodyParts
     * Sort by x → thứ tự layer vẽ
     * Sort by y → thứ tự nav
     */
    private fun buildCharData(
        bodyParts: List<BodyPartModel>,
        targetData: ArrayList<BodyPartModel>,
        targetArr: ArrayList<ArrayList<Int>>,
        targetShowColor: ArrayList<Boolean>,
        targetIconMap: MutableMap<String, Int>,
        targetImgs: ArrayList<AppCompatImageView>
    ) {
        val entries = bodyParts.mapNotNull { bp ->
            val parts = bp.icon.substringBeforeLast("/").substringAfterLast("/").split("-")
            val x = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val y = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            Triple(bp, x, y)
        }.sortedBy { it.second } // sort by x

        val xToLocal = entries.map { it.second }.distinct().sorted()
            .mapIndexed { i, v -> v to i }.toMap()

        // icon → viewIndex (x-based)
        entries.forEach { (bp, x, _) ->
            xToLocal[x]?.let { targetIconMap[bp.icon] = it }
        }

        // localListImage theo thứ tự y (nav order)
        val localListImage = Array(entries.size) { "" }
        entries.forEach { (bp, x, y) ->
            val lx = xToLocal[x] ?: return@forEach
            val yDistinct = entries.map { it.third }.distinct().sorted()
            val ly = yDistinct.indexOf(y)
            if (ly in localListImage.indices) localListImage[ly] = bp.icon
        }

        var checkFirst = true
        localListImage.forEach { icon ->
            if (icon.isEmpty()) return@forEach
            val bp = bodyParts.firstOrNull { it.icon == icon } ?: return@forEach
            targetShowColor.add(true)
            targetData.add(bp)
            targetArr.add(
                if (checkFirst) arrayListOf(1, 0).also { checkFirst = false }
                else arrayListOf(0, 0)
            )
        }

        // Tạo ImageView cho mỗi layer (theo viewIndex)
        repeat(targetIconMap.size) {
            targetImgs.add(AppCompatImageView(applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                binding.rl.addView(this)
            })
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────────────────────
    private fun renderAllImages() {
        listData.forEachIndexed { navIdx, bp ->
            putImage(bp.icon, arrInt[navIdx][0], false, navIdx, arrInt[navIdx][1])
        }
        listData2.forEachIndexed { navIdx, bp ->
            putImage2(bp.icon, arrInt2[navIdx][0], false, navIdx, arrInt2[navIdx][1])
        }
    }

    private fun preloadImages(onComplete: () -> Unit) {
        val toLoad = mutableListOf<Pair<AppCompatImageView, String>>()

        // ✅ Ẩn tất cả view trước
        listImg.forEach { it.visibility = View.GONE; it.tag = null }
        listImg2.forEach { it.visibility = View.GONE; it.tag = null }

        fun collect(data: ArrayList<BodyPartModel>, arr: ArrayList<ArrayList<Int>>,
                    map: Map<String, Int>, imgs: ArrayList<AppCompatImageView>) {
            data.forEachIndexed { i, bp ->
                val path = bp.listPath.getOrNull(arr[i][1])?.listPath?.getOrNull(arr[i][0]) ?: return@forEachIndexed
                if (path == "none" || path == "dice" || path.isEmpty()) return@forEachIndexed
                val view = imgs.getOrNull(map[bp.icon] ?: return@forEachIndexed) ?: return@forEachIndexed
                toLoad.add(view to path)
            }
        }
        collect(listData,  arrInt,  iconToIndexMap,  listImg)
        collect(listData2, arrInt2, iconToIndexMap2, listImg2)

        if (toLoad.isEmpty()) { onComplete(); return }

        val remaining = java.util.concurrent.atomic.AtomicInteger(toLoad.size)
        toLoad.forEach { (view, path) ->
            view.tag = path; view.visibility = View.VISIBLE
            Glide.with(applicationContext).load(path)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .priority(Priority.IMMEDIATE).skipMemoryCache(false)
                .dontAnimate().dontTransform()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, m: Any?, t: Target<Drawable>?, f: Boolean): Boolean {
                        if (remaining.decrementAndGet() == 0) binding.root.post { onComplete() }
                        return false
                    }
                    override fun onResourceReady(r: Drawable?, m: Any?, t: Target<Drawable>?, d: DataSource?, f: Boolean): Boolean {
                        if (remaining.decrementAndGet() == 0) binding.root.post { onComplete() }
                        return false
                    }
                }).into(view)
        }
    }



    // ─────────────────────────────────────────────────────────────────────────
    // PUT IMAGE
    // ─────────────────────────────────────────────────────────────────────────
    fun putImage(icon: String, pos: Int, checkRestart: Boolean = false,
                 posNav: Int? = null, posColor: Int? = null) {
        val viewIdx = iconToIndexMap[icon] ?: return
        val view = listImg.getOrNull(viewIdx) ?: return
        loadToView(view, pos, checkRestart, posNav, posColor, listData,
            adapterNav.posNav, adapterColor.posColor)
    }

    fun putImage2(icon: String, pos: Int, checkRestart: Boolean = false,
                  posNav: Int? = null, posColor: Int? = null) {
        val viewIdx = iconToIndexMap2[icon] ?: return
        val view = listImg2.getOrNull(viewIdx) ?: return
        loadToView(view, pos, checkRestart, posNav, posColor, listData2,
            adapterNav2.posNav, adapterColor2.posColor)
    }

    private fun loadToView(
        view: ImageView, pos: Int, checkRestart: Boolean,
        posNav: Int?, posColor: Int?,
        data: ArrayList<BodyPartModel>, defaultNav: Int, defaultColor: Int
    ) {
        if (checkRestart) { view.visibility = View.GONE; view.tag = null; return }
        val path = data[posNav ?: defaultNav].listPath[posColor ?: defaultColor].listPath[pos]
        if (view.tag == path && view.visibility == View.VISIBLE) return
        view.tag = path; view.visibility = View.VISIBLE
        synchronized(loadingLock) { loadingCount++ }
        Glide.with(applicationContext).load(path)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .priority(Priority.IMMEDIATE).skipMemoryCache(false)
            .dontAnimate().dontTransform()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, m: Any?, t: Target<Drawable>?, f: Boolean): Boolean {
                    synchronized(loadingLock) { loadingCount-- }
                    checkSaveReady(); return false
                }
                override fun onResourceReady(r: Drawable?, m: Any?, t: Target<Drawable>?, d: DataSource?, f: Boolean): Boolean {
                    synchronized(loadingLock) { loadingCount-- }
                    checkSaveReady(); return false
                }
            }).into(view)
    }

    private fun checkSaveReady() {
        synchronized(loadingLock) {
            if (loadingCount <= 0) {
                loadingCount = 0
                binding.root.post { canSave = true; binding.btnSave.alpha = 1f }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT PART LIST
    // ─────────────────────────────────────────────────────────────────────────
    private fun submitPartList(navPos: Int = adapterNav.posNav,
                               colorPos: Int = adapterColor.posColor,
                               commitCallback: (() -> Unit)? = null) {
        val actualList = listData[navPos].listPath[colorPos].listPath
        val thumbList  = listData[navPos].listThumbPath
        var ti = 0
        adapterPart.listThumb = actualList.map { (if (it == "none" || it == "dice") it else thumbList.getOrElse(ti++) { it }).toString() }
        adapterPart.submitList(actualList, commitCallback)
    }

    private fun submitPartList2(navPos: Int = adapterNav2.posNav,
                                colorPos: Int = adapterColor2.posColor,
                                commitCallback: (() -> Unit)? = null) {
        val actualList = listData2[navPos].listPath[colorPos].listPath
        val thumbList  = listData2[navPos].listThumbPath
        var ti = 0
        adapterPart2.listThumb = actualList.map { (if (it == "none" || it == "dice") it else thumbList.getOrElse(ti++) { it }).toString() }
        adapterPart2.submitList(actualList, commitCallback)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────────────────────────
    override fun initAction() {

        binding.btnSwitchCharacter.onSingleClick {
            switchToChar(if (editingChar == 1) 2 else 1)
        }

        binding.imvBack.onSingleClick {
            DialogExit(this, "exit").apply { onClick = { finish() } }.show()
        }

        binding.btnRevert.onSingleClick {
            checkRevert = !checkRevert
            val scale = if (checkRevert) 1f else -1f
            listImg.forEach  { it.scaleX = scale }
            listImg2.forEach { it.scaleX = scale }
        }

        // Nav char1
        adapterNav.onClick = { newPos ->
            lifecycleScope.launch {
                if (!checkNetwork()) return@launch
                val bp = listData[newPos]
                val safeColor = arrInt[newPos][1].coerceIn(0, bp.listPath.size - 1)
                val safePart  = arrInt[newPos][0].coerceIn(0, bp.listPath[safeColor].listPath.size - 1)
                arrInt[newPos][1] = safeColor; arrInt[newPos][0] = safePart
                adapterNav.setPos(newPos); adapterNav.submitList(listData)
                adapterColor.setPos(safeColor)
                updateColorSectionVisibility(newPos)
                if (bp.listPath.size > 1) {
                    adapterColor.submitList(bp.listPath)
                    if (arrShowColor[newPos])
                        binding.root.postDelayed({ binding.rcvColor.smoothScrollToPosition(safeColor) }, 100)
                }
                adapterPart.setPos(safePart)
                submitPartList(navPos = newPos, colorPos = safeColor)
                updateMatchUI()
                binding.root.postDelayed({ binding.rcvPart.smoothScrollToPosition(safePart) }, 100)
            }
        }

        // Nav char2
        adapterNav2.onClick = { newPos ->
            val bp = listData2[newPos]
            val safeColor = arrInt2[newPos][1].coerceIn(0, bp.listPath.size - 1)
            val safePart  = arrInt2[newPos][0].coerceIn(0, bp.listPath[safeColor].listPath.size - 1)
            arrInt2[newPos][1] = safeColor; arrInt2[newPos][0] = safePart
            adapterNav2.setPos(newPos); adapterNav2.submitList(listData2)
            adapterColor2.setPos(safeColor)
            adapterColor2.submitList(bp.listPath)
            adapterPart2.setPos(safePart)
            submitPartList2(navPos = newPos, colorPos = safeColor)
            updateColorVisibility2(newPos)
            updateMatchUI()
        }

        // Color char1
        adapterColor.onClick = { colorPos ->
            lifecycleScope.launch {
                if (!checkNetwork()) return@launch
                val state = binding.rcvPart.layoutManager?.onSaveInstanceState()
                adapterColor.setPos(colorPos)
                adapterColor.submitList(listData[adapterNav.posNav].listPath)
                arrInt[adapterNav.posNav][1] = colorPos
                submitPartList(commitCallback = { binding.rcvPart.layoutManager?.onRestoreInstanceState(state) })
                putImage(listData[adapterNav.posNav].icon, adapterPart.posPath)
                updateMatchUI()
            }
        }

        // Color char2
        adapterColor2.onClick = { colorPos ->
            val state = binding.rcvPart2.layoutManager?.onSaveInstanceState()
            adapterColor2.setPos(colorPos)
            adapterColor2.submitList(listData2[adapterNav2.posNav].listPath)
            arrInt2[adapterNav2.posNav][1] = colorPos
            submitPartList2(commitCallback = { binding.rcvPart2.layoutManager?.onRestoreInstanceState(state) })
            putImage2(listData2[adapterNav2.posNav].icon, adapterPart2.posPath)
            updateMatchUI()
        }

        // Part char1
        adapterPart.onClick = { pos, type ->
            lifecycleScope.launch {
                if (!checkNetwork()) return@launch
                handlePartClick(pos, type, listData, arrInt, adapterNav.posNav,
                    adapterColor.posColor, adapterPart, ::submitPartList, ::putImage)
                updateMatchUI()
            }
        }

        // Part char2
        adapterPart2.onClick = { pos, type ->
            handlePartClick(pos, type, listData2, arrInt2, adapterNav2.posNav,
                adapterColor2.posColor, adapterPart2, ::submitPartList2, ::putImage2)
            updateMatchUI()
        }

        binding.apply {

            imvShowColor.onSingleClick {
                if (editingChar == 1) {
                    val navPos = adapterNav.posNav
                    if ((listData.getOrNull(navPos)?.listPath?.size ?: 0) <= 1) return@onSingleClick
                    if (arrShowColor[navPos] && binding.llColor.visibility == View.VISIBLE) return@onSingleClick
                    arrShowColor[navPos] = true
                    binding.relativeLayout2.visibility = View.GONE
                    binding.relativeLayout.visibility = View.VISIBLE
                    binding.relativeLayout.alpha = 0f
                    binding.llColor.visibility = View.VISIBLE
                    binding.llColor.alpha = 0f
                    binding.llColor.animate().alpha(1f).setDuration(200).start()
                    binding.relativeLayout.animate().alpha(1f).setDuration(200).start()
                } else {
                    val navPos = adapterNav2.posNav
                    if ((listData2.getOrNull(navPos)?.listPath?.size ?: 0) <= 1) return@onSingleClick
                    if (arrShowColor2[navPos] && binding.llColor.visibility == View.VISIBLE) return@onSingleClick
                    arrShowColor2[navPos] = true
                    binding.relativeLayout.visibility = View.GONE
                    binding.relativeLayout2.visibility = View.VISIBLE
                    binding.relativeLayout2.alpha = 0f
                    binding.llColor.visibility = View.VISIBLE
                    binding.llColor.alpha = 0f
                    binding.llColor.animate().alpha(1f).setDuration(200).start()
                    binding.relativeLayout2.animate().alpha(1f).setDuration(200).start()
                    adapterColor2.submitList(listData2[navPos].listPath)
                }
            }

            imvEndColor.onSingleClick {
                val navPos = adapterNav.posNav
                if (navPos < arrShowColor.size) arrShowColor[navPos] = false
                binding.llColor.animate().alpha(0f).setDuration(200)
                    .withEndAction { binding.llColor.visibility = View.GONE }.start()
            }

            imvEndColor2.onSingleClick {
                val navPos = adapterNav2.posNav
                if (navPos < arrShowColor2.size) arrShowColor2[navPos] = false
                binding.llColor.animate().alpha(0f).setDuration(200)
                    .withEndAction { binding.llColor.visibility = View.GONE }.start()
            }

            btnReset.onSingleClick {
                lifecycleScope.launch {
                    if (!checkNetwork()) return@launch
                    DialogExit(this@ShowActivity, "reset").apply {
                        onClick = {
                            listData.forEach { putImage(it.icon, 0, true) }
                            arrInt.forEachIndexed { i, arr ->
                                arr[0] = if (i == 0) 1 else 0; arr[1] = 0
                            }
                            adapterPart.setPos(arrInt[adapterNav.posNav][0])
                            adapterColor.setPos(arrInt[adapterNav.posNav][1])
                            submitPartList(); updateColorSectionVisibility()
                            putImage(listData[0].icon, 1, false, 0, 0)
                            updateMatchUI()
                        }
                    }.show()
                }
            }

            btnDice.onSingleClick {
                lifecycleScope.launch {
                    if (!checkNetwork()) return@launch
                    canSave = false; btnSave.alpha = 0.5f; countRandom++

                    val data    = if (editingChar == 1) listData  else listData2
                    val arr     = if (editingChar == 1) arrInt    else arrInt2
                    val nav     = if (editingChar == 1) adapterNav    else adapterNav2
                    val color   = if (editingChar == 1) adapterColor  else adapterColor2
                    val part    = if (editingChar == 1) adapterPart   else adapterPart2
                    val submit  = if (editingChar == 1) ::submitPartList  else ::submitPartList2
                    val putFn   = if (editingChar == 1) ::putImage    else ::putImage2
                    val rcvPart  = if (editingChar == 1) binding.rcvPart  else binding.rcvPart2
                    val rcvColor = if (editingChar == 1) binding.rcvColor else binding.rcvColor2

                    data.forEachIndexed { i, bp ->
                        arr[i][1] = if (bp.listPath.size > 1) (0 until bp.listPath.size).random() else 0
                        val paths = bp.listPath[arr[i][1]].listPath
                        arr[i][0] = when {
                            paths[0] == "none" -> if (paths.size > 3) (2 until paths.size).random() else 2
                            else               -> if (paths.size > 2) (1 until paths.size).random() else 1
                        }
                        putFn(bp.icon, arr[i][0], false, i, arr[i][1])
                    }

                    part.setPos(arr[nav.posNav][0])
                    color.setPos(arr[nav.posNav][1])
                    submit(nav.posNav, arr[nav.posNav][1], null)
                    updateColorSectionVisibility(nav.posNav)
                    rcvPart.post  { rcvPart.smoothScrollToPosition(part.posPath) }
                    rcvColor.post { rcvColor.smoothScrollToPosition(color.posColor) }
                    updateMatchUI()
                    root.postDelayed({ canSave = true; btnSave.alpha = 1f }, 2000)
                }
            }

            llLoading.onSingleClick {
                showToast(applicationContext, R.string.please_wait_a_few_seconds_for_data_to_load)
            }

            btnSave.onSingleClick {
                if (!canSave) return@onSingleClick
                llLoading.visibility = View.VISIBLE
                val bmp  = viewToBitmap(rl)
                val file = java.io.File(cacheDir, "show_preview_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(file).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.flush()
                }
                llLoading.visibility = View.GONE
                startActivityForResult(
                    Intent(this@ShowActivity, SuccessCosplayActivity::class.java)
                        .putExtra("cosplayBitmapPath", imgCoslay)
                        .putExtra("currentBitmapPath", file.absolutePath)
                        .putExtra("matchPercent", calculateMatchPercent()),
                    REQUEST_COSPLAY
                )
            }

            btnSee.onSingleClick {
                if (btnRevert.isInvisible) {
                    btnRevert.show(); btnReset.show(); btnSave.show()
                    if (listData[adapterNav.posNav].listPath.size > 1) {
                        if (arrShowColor[adapterNav.posNav]) llColor.show()
                        imvShowColor.show()
                    }
                    btnDice.show(); llPart.show(); llNav.show()
                    btnSee.setImageResource(R.drawable.ic_show)
                } else {
                    btnRevert.inhide(); btnReset.inhide(); btnSave.inhide()
                    imvShowColor.inhide(); llColor.inhide(); btnDice.inhide()
                    llPart.inhide(); llNav.inhide()
                    btnSee.setImageResource(R.drawable.imv_see_false)
                }
            }

            frameImageSmall.onSingleClick {
                val sl = IntArray(2); frameImageSmall.getLocationOnScreen(sl)
                val fl = IntArray(2); frameShowImage.getLocationOnScreen(fl)
                frameShowImage.pivotX = (sl[0] + frameImageSmall.width  / 2f) - fl[0]
                frameShowImage.pivotY = (sl[1] + frameImageSmall.height / 2f) - fl[1]
                frameShowImage.scaleX = 0f; frameShowImage.scaleY = 0f; frameShowImage.alpha = 0f
                viewShowImage.visibility = View.VISIBLE
                viewShowImage.isClickable = true; viewShowImage.isFocusable = true
                frameShowImage.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(350).setInterpolator(DecelerateInterpolator()).start()
            }

            closeImage.onSingleClick {
                val sl = IntArray(2); frameImageSmall.getLocationOnScreen(sl)
                val fl = IntArray(2); frameShowImage.getLocationOnScreen(fl)
                frameShowImage.pivotX = (sl[0] + frameImageSmall.width  / 2f) - fl[0]
                frameShowImage.pivotY = (sl[1] + frameImageSmall.height / 2f) - fl[1]
                frameShowImage.animate().scaleX(0f).scaleY(0f).alpha(0f)
                    .setDuration(250).setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        viewShowImage.visibility = View.INVISIBLE
                        viewShowImage.isClickable = false; viewShowImage.isFocusable = false
                        frameShowImage.scaleX = 1f; frameShowImage.scaleY = 1f; frameShowImage.alpha = 1f
                    }.start()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun checkNetwork(): Boolean {
        if (!DataHelper.arrBlackCentered[blackCentered].checkDataOnline) return true
        if (!isInternetAvailable(applicationContext)) {
            DialogExit(this, "loadingnetwork").show(); return false
        }
        val ok = withContext(Dispatchers.IO) { isNetworkConnected(this@ShowActivity) }
        if (!ok) { DialogExit(this, "networked").show(); return false }
        return true
    }

    private fun handlePartClick(
        pos: Int, type: String,
        data: ArrayList<BodyPartModel>, arr: ArrayList<ArrayList<Int>>,
        navPos: Int, colorPos: Int, adapterP: PartAdapter,
        submitFn: (Int, Int, (() -> Unit)?) -> Unit,
        putFn: (String, Int, Boolean, Int?, Int?) -> Unit
    ) {
        val paths = data[navPos].listPath[colorPos].listPath
        when (type) {
            "none" -> {
                adapterP.setPos(pos); submitFn(navPos, colorPos, null)
                arr[navPos][0] = pos; arr[navPos][1] = colorPos
                putFn(data[navPos].icon, pos, true, null, null)
            }
            "dice" -> {
                val x = when (paths[0]) {
                    "none" -> if (paths.size > 3) (2 until paths.size).random() else 2
                    else   -> if (paths.size > 2) (1 until paths.size).random() else 1
                }
                adapterP.setPos(x); submitFn(navPos, colorPos, null)
                arr[navPos][0] = x; arr[navPos][1] = colorPos
                putFn(data[navPos].icon, x, false, null, null)
            }
            else -> {
                adapterP.setPos(pos); submitFn(navPos, colorPos, null)
                arr[navPos][0] = pos; arr[navPos][1] = colorPos
                putFn(data[navPos].icon, pos, false, null, null)
            }
        }
    }
    private fun switchToChar(char: Int) {
        editingChar = char
        val isChar1 = char == 1
        binding.apply {
            btnSwitchCharacter.setImageResource(
                if (isChar1) R.drawable.ic_switch_character_male
                else R.drawable.ic_switch_character_female
            )
            rcvNav.visibility   = if (isChar1) View.VISIBLE else View.GONE
            rcvNav2.visibility  = if (isChar1) View.GONE    else View.VISIBLE
            rcvPart.visibility  = if (isChar1) View.VISIBLE else View.GONE
            rcvPart2.visibility = if (isChar1) View.GONE    else View.VISIBLE

            if (isChar1) {
                adapterNav.submitList(listData)
                adapterColor.setPos(arrInt[adapterNav.posNav][1])
                adapterColor.submitList(listData[adapterNav.posNav].listPath)
                adapterPart.setPos(arrInt[adapterNav.posNav][0])
                submitPartList()
                updateColorSectionVisibility(adapterNav.posNav)
            } else {
                adapterNav2.submitList(listData2)
                adapterColor2.setPos(arrInt2[adapterNav2.posNav][1])
                adapterColor2.submitList(listData2[adapterNav2.posNav].listPath)
                adapterPart2.setPos(arrInt2[adapterNav2.posNav][0])
                submitPartList2()
                updateColorVisibility2(adapterNav2.posNav)
            }
        }
    }

    private fun updateColorSectionVisibility(posNav: Int = adapterNav.posNav) {
        if (posNav < 0 || posNav >= listData.size) return
        val hasMultipleColors = listData[posNav].listPath.size > 1

        // Luôn ẩn char2 khi đang ở char1
        binding.relativeLayout2.visibility = View.GONE

        if (!hasMultipleColors) {
            // Nav không có màu → ẩn toàn bộ color row, reset flag
            binding.imvShowColor.visibility = View.INVISIBLE
            binding.llColor.visibility = View.GONE
            if (posNav < arrShowColor.size) arrShowColor[posNav] = true
            return
        }

        // Nav có màu → show imvShowColor + relativeLayout
        binding.imvShowColor.visibility = View.VISIBLE
        binding.imvShowColor.alpha = 1f

        if (arrShowColor[posNav]) {
            binding.llColor.visibility = View.VISIBLE
            binding.llColor.alpha = 1f
            binding.relativeLayout.visibility = View.VISIBLE
            binding.relativeLayout.alpha = 1f
        } else {
            // User đã đóng → ẩn llColor nhưng giữ flag
            binding.llColor.visibility = View.GONE
            binding.relativeLayout.visibility = View.GONE
        }
    }

    private fun updateColorVisibility2(posNav: Int) {
        if (posNav < 0 || posNav >= listData2.size) return
        val hasMultipleColors = listData2[posNav].listPath.size > 1

        // Luôn ẩn char1 khi đang ở char2
        binding.relativeLayout.visibility = View.GONE

        if (!hasMultipleColors) {
            binding.imvShowColor.visibility = View.INVISIBLE
            binding.llColor.visibility = View.GONE
            if (posNav < arrShowColor2.size) arrShowColor2[posNav] = true
            return
        }

        binding.imvShowColor.visibility = View.VISIBLE
        binding.imvShowColor.alpha = 1f

        if (arrShowColor2[posNav]) {
            binding.llColor.visibility = View.VISIBLE
            binding.llColor.alpha = 1f
            binding.relativeLayout2.visibility = View.VISIBLE
            binding.relativeLayout2.alpha = 1f
            adapterColor2.submitList(listData2[posNav].listPath)
        } else {
            binding.llColor.visibility = View.GONE
            binding.relativeLayout2.visibility = View.GONE
        }
    }



    // ─────────────────────────────────────────────────────────────────────────
    // MATCH %
    // ─────────────────────────────────────────────────────────────────────────
    private fun calculateMatchPercent(): Int {
        val orig1 = originalPaths ?: return 0
        val total = listData.size + listData2.size
        if (total == 0) return 0
        var match = 0

        listData.forEachIndexed { i, bp ->
            val vi   = iconToIndexMap[bp.icon] ?: return@forEachIndexed
            val orig = orig1.getOrNull(vi)?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
            val cur  = bp.listPath.getOrNull(arrInt[i][1])?.listPath?.getOrNull(arrInt[i][0])
                ?.takeIf { it != "none" && it != "dice" } ?: return@forEachIndexed
            if (cur == orig) match++
        }

        originalPaths2?.let { orig2 ->
            listData2.forEachIndexed { i, bp ->
                val vi   = iconToIndexMap2[bp.icon] ?: return@forEachIndexed
                val orig = orig2.getOrNull(vi)?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
                val cur  = bp.listPath.getOrNull(arrInt2[i][1])?.listPath?.getOrNull(arrInt2[i][0])
                    ?.takeIf { it != "none" && it != "dice" } ?: return@forEachIndexed
                if (cur == orig) match++
            }
        }

        return (match * 100f / total).roundToInt()
    }

    private fun updateMatchUI() {
        if (originalPaths == null) {
            binding.layoutProgress.visibility = View.GONE
            binding.tvMatchPercent.visibility  = View.GONE
            return
        }
        val percent = calculateMatchPercent()
        binding.tvMatchPercent.text = "$percent%"
        val scale = percent / 100f
        binding.progressTrack.post {
            val trackH = binding.progressTrack.height.toFloat()
            val marginPx = 10 * resources.displayMetrics.density
            val fillH = trackH - marginPx
            binding.progressFill.pivotX = binding.progressFill.width / 2f
            binding.progressFill.pivotY = fillH
            ObjectAnimator.ofFloat(binding.progressFill, "scaleY",
                binding.progressFill.scaleY, scale * fillH / trackH).apply {
                duration = 400; interpolator = DecelerateInterpolator(); start()
            }
            val starH = binding.imgStar.height.toFloat()
            ObjectAnimator.ofFloat(binding.imgStar, "translationY",
                binding.imgStar.translationY, -(fillH * scale) - marginPx + starH / 2f).apply {
                duration = 400; interpolator = DecelerateInterpolator(); start()
            }
        }
    }

    override fun onBackPressed() {
        DialogExit(this, "exit").apply { onClick = { finish() } }.show()
    }
}