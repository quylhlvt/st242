package com.anime.couple.couplemaker.ui.customview

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
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
import com.anime.couple.couplemaker.data.model.AvatarModel
import com.anime.couple.couplemaker.data.model.BodyPartModel
import com.anime.couple.couplemaker.databinding.ActivityCustomizeBinding
import com.anime.couple.couplemaker.dialog.DialogExit
import com.anime.couple.couplemaker.ui.background.BackgroundActivity
import com.anime.couple.couplemaker.utils.DataHelper
import com.anime.couple.couplemaker.utils.DataHelper.arrBlackCentered
import com.anime.couple.couplemaker.utils.fromList
import com.anime.couple.couplemaker.utils.inhide
import com.anime.couple.couplemaker.utils.isInternetAvailable
import com.anime.couple.couplemaker.utils.isNetworkConnected
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.utils.saveBitmap
import com.anime.couple.couplemaker.utils.show
import com.anime.couple.couplemaker.utils.showToast
import com.anime.couple.couplemaker.utils.toList
import com.anime.couple.couplemaker.utils.viewToBitmap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CustomviewActivity : AbsBaseActivity<ActivityCustomizeBinding>() {
    val viewModel: CustomviewViewModel by viewModels()
    var arrShowColor = arrayListOf<Boolean>()
    var arrShowColor2 = arrayListOf<Boolean>()
    var countRandom = 0
    var editingChar = 1

    val adapterColor by lazy { ColorAdapter() }
    val adapterNav by lazy { NavAdapter(this@CustomviewActivity) }
    val adapterPart by lazy { PartAdapter() }
    val adapterNav2 by lazy { NavAdapter(this@CustomviewActivity) }
    val adapterPart2 by lazy { PartAdapter() }
    val adapterColor2 by lazy { ColorAdapter() }

    var listImg = arrayListOf<AppCompatImageView>()
    var listImg2 = arrayListOf<AppCompatImageView>()
    var listData = arrayListOf<BodyPartModel>()
    var listData2 = arrayListOf<BodyPartModel>()
    var arrInt = arrayListOf<ArrayList<Int>>()
    var arrInt2 = arrayListOf<ArrayList<Int>>()
    var blackCentered = 0
    var arrIntHottrend: ArrayList<ArrayList<Int>>? = null
    var checkRevert = true
    var checkHide = false

    private val iconToIndexMap = mutableMapOf<String, Int>()
    private val iconToIndexMap2 = mutableMapOf<String, Int>()
    private val loadingLock = Any()
    private var canSave = true
    private var loadingImagesCount = 0

    override fun getLayoutId(): Int = R.layout.activity_customize



    // ─── INIT ─────────────────────────────────────────────────────────────────
    override fun onRestart() {
        super.onRestart()
    }

    override fun initView() {
        binding.txtTitle.post { binding.txtTitle.isSelected = true }
        val isFlipped = intent.getBooleanExtra("isFlipped", false)
        binding.btnSave.isSelected = true
        binding.imvBack.isSelected = true

        if (DataHelper.arrBlackCentered.size > 0) {
            binding.apply {
                rcvPart.adapter = adapterPart; rcvPart.itemAnimator = null
                rcvColor.adapter = adapterColor; rcvColor.itemAnimator = null
                rcvNav.adapter = adapterNav; rcvNav.itemAnimator = null
                rcvNav2.adapter = adapterNav2; rcvNav2.itemAnimator = null
                rcvPart2.adapter = adapterPart2; rcvPart2.itemAnimator = null
                rcvColor2.adapter = adapterColor2; rcvColor2.itemAnimator = null
            }

            getData1()

            if (isFlipped) {
                checkRevert = false
                listImg.forEach { it.scaleX = -1f }
                listImg2.forEach { it.scaleX = -1f }
            }

            adapterNav.posNav = 0
            adapterNav2.posNav = 0
            switchToChar(1)

            adapterColor.setPos(arrInt[0][1])
            adapterColor.submitList(listData[0].listPath)
            adapterPart.setPos(arrInt[0][0])
            submitPartList()
            updateColorSectionVisibility(0)

            if (arrIntHottrend != null) {
                adapterPart.setPos(arrInt[adapterNav.posNav][0])
                adapterColor.setPos(arrInt[adapterNav.posNav][1])
                submitPartList()
                updateColorSectionVisibility(adapterNav.posNav)
                if (listData[adapterNav.posNav].listPath.size == 1)
                    binding.llColor.visibility = View.GONE
                else {
                    binding.llColor.visibility = View.VISIBLE
                    adapterColor.submitList(listData[adapterNav.posNav].listPath)
                }
            }

            binding.llLoading.visibility = View.VISIBLE
            canSave = false
            binding.btnSave.alpha = 0.5f

            preloadInitialImages {
                binding.llLoading.visibility = View.GONE
                canSave = true
                binding.btnSave.alpha = 1f
            }
        } else {
            finish()
        }
    }

    // ─── DATA ─────────────────────────────────────────────────────────────────
    private fun getData1() {
        binding.rl.removeAllViews()
        listData.clear(); arrInt.clear(); arrShowColor.clear(); iconToIndexMap.clear()
        listData2.clear(); arrInt2.clear(); arrShowColor2.clear(); iconToIndexMap2.clear()
        listImg.clear(); listImg2.clear()
        checkRevert = true

        blackCentered = intent.getIntExtra("data", 0)

        // ✅ Dùng null-safe: nếu arr null thì rawArr = null
        val rawArr = try {
            intent.getStringExtra("arr")?.let { toList(it) }
                ?: (intent.getSerializableExtra("arr") as? ArrayList<ArrayList<Int>>)
        } catch (e: Exception) {
            intent.getSerializableExtra("arr") as? ArrayList<ArrayList<Int>>
        }

        var arrIntHottrend2: ArrayList<ArrayList<Int>>? = null
        if (rawArr != null) {
            val sepIndex = rawArr.indexOfFirst { it.size == 2 && it[0] == -1 && it[1] == -1 }
            if (sepIndex >= 0) {
                arrIntHottrend = ArrayList(rawArr.subList(0, sepIndex))
                arrIntHottrend2 = ArrayList(rawArr.subList(sepIndex + 1, rawArr.size))
            } else {
                arrIntHottrend = rawArr
                arrIntHottrend2 = null
            }
        } else {
            arrIntHottrend = null
            arrIntHottrend2 = null
        }

        val currentCat = DataHelper.arrBlackCentered.getOrNull(blackCentered) ?: return

        loadCharData(
            currentCat.bodyPart.filter { it.charType == 1 },
            listData, arrInt, arrShowColor, iconToIndexMap, listImg
        )
        loadCharData(
            currentCat.bodyPart.filter { it.charType == 2 },
            listData2, arrInt2, arrShowColor2, iconToIndexMap2, listImg2
        )

        arrIntHottrend?.forEachIndexed { index, arr ->
            if (index < arrInt.size) {
                arrInt[index][0] = arr.getOrElse(0) { arrInt[index][0] }
                arrInt[index][1] = arr.getOrElse(1) { arrInt[index][1] }
            }
        }

        arrIntHottrend2?.forEachIndexed { index, arr ->
            if (index < arrInt2.size) {
                arrInt2[index][0] = arr.getOrElse(0) { arrInt2[index][0] }
                arrInt2[index][1] = arr.getOrElse(1) { arrInt2[index][1] }
            }
        }
    }

    private fun loadCharData(
        bodyParts: List<BodyPartModel>,
        targetListData: ArrayList<BodyPartModel>,
        targetArrInt: ArrayList<ArrayList<Int>>,
        targetArrShowColor: ArrayList<Boolean>,
        targetIconMap: MutableMap<String, Int>,
        targetListImg: ArrayList<AppCompatImageView>
    ) {
        val entries = bodyParts.mapNotNull { bp ->
            val parts = bp.icon.substringBeforeLast("/").substringAfterLast("/").split("-")
            val x = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val y = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            Triple(bp, x, y)
        }.sortedBy { it.second }

        val xToLocal = entries.map { it.second }.distinct().sorted()
            .mapIndexed { i, v -> v to i }.toMap()
        val yToLocal = entries.map { it.third }.distinct().sorted()
            .mapIndexed { i, v -> v to i }.toMap()

        val localListImage = Array(entries.size) { "" }
        entries.forEach { (bp, x, y) ->
            val lx = xToLocal[x] ?: return@forEach
            val ly = yToLocal[y] ?: return@forEach
            if (ly < localListImage.size) localListImage[ly] = bp.icon
            targetIconMap[bp.icon] = lx
        }

        var checkFirst = true
        localListImage.forEach { icon ->
            if (icon.isEmpty()) return@forEach
            val bp = bodyParts.firstOrNull { it.icon == icon } ?: return@forEach
            targetArrShowColor.add(true)
            targetListData.add(bp)
            targetArrInt.add(if (checkFirst) arrayListOf(1, 0).also { checkFirst = false }
            else arrayListOf(0, 0))
        }

        repeat(targetIconMap.size) {
            targetListImg.add(AppCompatImageView(applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                binding.rl.addView(this)
            })
        }
    }

    // ─── SUBMIT PART ──────────────────────────────────────────────────────────
    private fun submitPartList(
        navPos: Int = adapterNav.posNav,
        colorPos: Int = adapterColor.posColor,
        commitCallback: (() -> Unit)? = null
    ) {
        val actualList = listData[navPos].listPath[colorPos].listPath
        adapterPart.listThumb = getThumbList(listData[navPos].listThumbPath, actualList)
        adapterPart.submitList(actualList, commitCallback)
    }

    private fun submitPartList2(
        navPos: Int = adapterNav2.posNav,
        colorPos: Int = adapterColor2.posColor,
        commitCallback: (() -> Unit)? = null
    ) {
        val actualList = listData2[navPos].listPath[colorPos].listPath
        adapterPart2.listThumb = getThumbList(listData2[navPos].listThumbPath, actualList)
        adapterPart2.submitList(actualList, commitCallback)
    }

    private fun getThumbList(thumbList: ArrayList<String>, actualList: List<String>): List<String> {
        var thumbIndex = 0
        return actualList.map { path ->
            when (path) {
                "none", "dice" -> path
                else -> thumbList.getOrElse(thumbIndex++) { path }
            }
        }
    }

    // ─── PUT IMAGE ────────────────────────────────────────────────────────────
    fun putImage(
        icon: String, pos: Int, checkRestart: Boolean = false,
        posNav: Int? = null, posColor: Int? = null
    ) {
        iconToIndexMap[icon]?.let { _pos ->
            handleVisibility(
                listImg[_pos], pos, checkRestart, posNav, posColor,
                listData, adapterNav.posNav, adapterColor.posColor
            )
        }
    }

    fun putImage2(
        icon: String, pos: Int, checkRestart: Boolean = false,
        posNav: Int? = null, posColor: Int? = null
    ) {
        iconToIndexMap2[icon]?.let { _pos ->
            val view = listImg2.getOrNull(_pos) ?: return
            handleVisibility(
                view, pos, checkRestart, posNav, posColor,
                listData2, adapterNav2.posNav, adapterColor2.posColor
            )
        }
    }

    private fun handleVisibility(
        view: ImageView, pos: Int, checkRestart: Boolean,
        posNav: Int?, posColor: Int?,
        data: ArrayList<BodyPartModel>, defaultNav: Int, defaultColor: Int
    ) {
        if (checkRestart) {
            view.visibility = View.GONE
            view.tag = null
            return
        }
        val path = data[posNav ?: defaultNav].listPath[posColor ?: defaultColor].listPath[pos]
        if (view.tag == path && view.visibility == View.VISIBLE) return
        view.tag = path
        view.visibility = View.VISIBLE
        synchronized(loadingLock) { loadingImagesCount++ }
        Glide.with(applicationContext)
            .load(path)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .priority(Priority.IMMEDIATE)
            .skipMemoryCache(false)
            .dontAnimate().dontTransform()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>?, isFirstResource: Boolean
                ): Boolean {
                    loadingImagesCount--; checkAllImagesLoaded(); return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?,
                    target: Target<Drawable>?, dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    loadingImagesCount--; checkAllImagesLoaded(); return false
                }
            }).into(view)
    }

    private fun checkAllImagesLoaded() {
        synchronized(loadingLock) {
            if (loadingImagesCount <= 0) {
                loadingImagesCount = 0
                binding.root.post { canSave = true; binding.btnSave.alpha = 1f }
            }
        }
    }

    // ─── PRELOAD ──────────────────────────────────────────────────────────────
    private fun preloadInitialImages(onComplete: () -> Unit) {
        val pathsToLoad = mutableListOf<Pair<AppCompatImageView, String>>()

        fun collectPaths(
            data: ArrayList<BodyPartModel>, arr: ArrayList<ArrayList<Int>>,
            map: Map<String, Int>, imgs: ArrayList<AppCompatImageView>
        ) {
            data.forEachIndexed { index, bodyPart ->
                val path = bodyPart.listPath.getOrNull(arr[index][1])?.listPath
                    ?.getOrNull(arr[index][0]) ?: return@forEachIndexed
                if (path == "none" || path == "dice" || path.isEmpty()) return@forEachIndexed
                val view = imgs.getOrNull(map[bodyPart.icon] ?: return@forEachIndexed)
                    ?: return@forEachIndexed
                pathsToLoad.add(view to path)
            }
        }

        collectPaths(listData, arrInt, iconToIndexMap, listImg)
        collectPaths(listData2, arrInt2, iconToIndexMap2, listImg2)

        if (pathsToLoad.isEmpty()) {
            onComplete(); return
        }

        val remaining = java.util.concurrent.atomic.AtomicInteger(pathsToLoad.size)
        pathsToLoad.forEach { (view, path) ->
            view.tag = path; view.visibility = View.VISIBLE
            Glide.with(applicationContext).load(path)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .priority(Priority.IMMEDIATE).skipMemoryCache(false)
                .dontAnimate().dontTransform()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?,
                        target: Target<Drawable>?, isFirstResource: Boolean
                    ): Boolean {
                        if (remaining.decrementAndGet() == 0) binding.root.post { onComplete() }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?, model: Any?,
                        target: Target<Drawable>?, dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (remaining.decrementAndGet() == 0) binding.root.post { onComplete() }
                        return false
                    }
                }).into(view)
        }
    }

    // ─── ACTIONS ──────────────────────────────────────────────────────────────
    override fun initAction() {
        binding.btnSwitchCharacter.onSingleClick {
            switchToChar(if (editingChar == 1) 2 else 1)
        }

        adapterNav.onClick = {
            lifecycleScope.launch {
                if (!checkNetwork()) return@launch
                val newPos = it
                val bp = listData[newPos]
                val safeColor = arrInt[newPos][1].coerceIn(0, bp.listPath.size - 1)
                val safePart =
                    arrInt[newPos][0].coerceIn(0, bp.listPath[safeColor].listPath.size - 1)
                arrInt[newPos][1] = safeColor; arrInt[newPos][0] = safePart
                adapterNav.setPos(newPos); adapterNav.submitList(listData)
                adapterColor.setPos(safeColor)
                updateColorSectionVisibility(newPos)
                if (bp.listPath.size > 1) {
                    adapterColor.submitList(bp.listPath)
                    if (arrShowColor[newPos])
                        binding.root.postDelayed(
                            { binding.rcvColor.smoothScrollToPosition(safeColor) },
                            100
                        )
                }
                adapterPart.setPos(if (adapterColor.posColor == safeColor) safePart else -1)
                submitPartList(navPos = newPos, colorPos = safeColor)
                binding.root.postDelayed({ binding.rcvPart.smoothScrollToPosition(safePart) }, 100)
            }
        }

        adapterNav2.onClick = {
            val newPos = it
            val bp = listData2[newPos]
            val safeColor = arrInt2[newPos][1].coerceIn(0, bp.listPath.size - 1)
            val safePart = arrInt2[newPos][0].coerceIn(0, bp.listPath[safeColor].listPath.size - 1)
            arrInt2[newPos][1] = safeColor; arrInt2[newPos][0] = safePart
            adapterNav2.setPos(newPos); adapterNav2.submitList(listData2)
            adapterColor2.setPos(safeColor)
            adapterColor2.submitList(bp.listPath)
            adapterPart2.setPos(safePart)
            submitPartList2(navPos = newPos, colorPos = safeColor)
            updateColorVisibility2(newPos)
        }

        adapterColor.onClick = {
            lifecycleScope.launch {
                if (!checkNetwork()) return@launch
                val state = binding.rcvPart.layoutManager?.onSaveInstanceState()
                adapterColor.setPos(it)
                adapterColor.submitList(listData[adapterNav.posNav].listPath)
                arrInt[adapterNav.posNav][1] = it
                submitPartList(commitCallback = {
                    binding.rcvPart.layoutManager?.onRestoreInstanceState(state)
                })
                putImage(listData[adapterNav.posNav].icon, adapterPart.posPath)
            }
        }

        adapterColor2.onClick = {
            val state = binding.rcvPart2.layoutManager?.onSaveInstanceState()
            adapterColor2.setPos(it)
            adapterColor2.submitList(listData2[adapterNav2.posNav].listPath)
            arrInt2[adapterNav2.posNav][1] = it
            submitPartList2(commitCallback = {
                binding.rcvPart2.layoutManager?.onRestoreInstanceState(state)
            })
            putImage2(listData2[adapterNav2.posNav].icon, adapterPart2.posPath)
        }

        adapterPart.onClick = { it, type ->
            lifecycleScope.launch {
                if (!checkNetwork()) return@launch
                handlePartClick(
                    it, type, listData, arrInt, adapterNav.posNav,
                    adapterColor.posColor, adapterPart, ::submitPartList, ::putImage
                )
            }
        }

        adapterPart2.onClick = { it, type ->
            handlePartClick(
                it, type, listData2, arrInt2, adapterNav2.posNav,
                adapterColor2.posColor, adapterPart2, ::submitPartList2, ::putImage2
            )
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
                    val dialog = DialogExit(this@CustomviewActivity, "reset")
                    dialog.onClick = {
                        listData.forEach { putImage(it.icon, 0, true) }
                        arrInt.forEachIndexed { i, arr ->
                            arr[0] = if (i == 0) 1 else 0; arr[1] = 0
                        }
                        adapterPart.setPos(arrInt[adapterNav.posNav][0])
                        adapterColor.setPos(arrInt[adapterNav.posNav][1])
                        submitPartList(); updateColorSectionVisibility()
                        putImage(listData[0].icon, 1, false, 0, 0)
                    }
                    dialog.show()
                }
            }

            imvBack.onSingleClick {
                DialogExit(this@CustomviewActivity, "exit").apply {
                    onClick = { finish() }
                }.show()
            }

            btnRevert.onSingleClick {
                checkRevert = !checkRevert
                val scale = if (checkRevert) 1f else -1f
                listImg.forEach { it.scaleX = scale }
                listImg2.forEach { it.scaleX = scale }
            }

            btnDice.onSingleClick {
                lifecycleScope.launch {
                    if (!checkNetwork()) return@launch
                    canSave = false; btnSave.alpha = 0.5f; countRandom++

                    // ✅ Chọn đúng data theo editingChar
                    val currentData = if (editingChar == 1) listData else listData2
                    val currentArrInt = if (editingChar == 1) arrInt else arrInt2
                    val currentNav = if (editingChar == 1) adapterNav else adapterNav2
                    val currentColor = if (editingChar == 1) adapterColor else adapterColor2
                    val currentPart = if (editingChar == 1) adapterPart else adapterPart2
                    val submitFn = if (editingChar == 1) ::submitPartList else ::submitPartList2
                    val putFn = if (editingChar == 1) ::putImage else ::putImage2

                    currentData.forEachIndexed { index, partBody ->
                        currentArrInt[index][1] = if (partBody.listPath.size > 1)
                            (0..<partBody.listPath.size).random() else 0
                        currentArrInt[index][0] = when {
                            partBody.listPath[currentArrInt[index][1]].listPath[0] == "none" ->
                                if (partBody.listPath[currentArrInt[index][1]].listPath.size > 3)
                                    (2..<partBody.listPath[currentArrInt[index][1]].listPath.size).random() else 2

                            else ->
                                if (partBody.listPath[currentArrInt[index][1]].listPath.size > 2)
                                    (1..<partBody.listPath[currentArrInt[index][1]].listPath.size).random() else 1
                        }
                        putFn(
                            partBody.icon,
                            currentArrInt[index][0],
                            false,
                            index,
                            currentArrInt[index][1]
                        )
                    }

                    currentPart.setPos(currentArrInt[currentNav.posNav][0])
                    currentColor.setPos(currentArrInt[currentNav.posNav][1])
                    submitFn(currentNav.posNav, currentArrInt[currentNav.posNav][1], null)
                    updateColorSectionVisibility(currentNav.posNav)

                    val rcvPart = if (editingChar == 1) binding.rcvPart else binding.rcvPart2
                    val rcvColor = if (editingChar == 1) binding.rcvColor else binding.rcvColor2
                    rcvPart.post { rcvPart.smoothScrollToPosition(currentPart.posPath) }
                    rcvColor.post { rcvColor.smoothScrollToPosition(currentColor.posColor) }
                    root.postDelayed({ canSave = true; btnSave.alpha = 1f }, 2000)
                }
            }

            llLoading.onSingleClick {
                showToast(applicationContext, R.string.please_wait_a_few_seconds_for_data_to_load)
            }

            btnSave.onSingleClick {
                if (!canSave) return@onSingleClick
                llLoading.visibility = View.VISIBLE
                saveBitmap(
                    this@CustomviewActivity, viewToBitmap(rl),
                    intent.getStringExtra("fileName") ?: "", true
                ) { success, path, pathOld ->
                    llLoading.visibility = View.GONE
                    if (success) {
                        viewModel.deleteAvatar(pathOld)
                        // ✅ Gộp: phần tử đầu [-1,-1] là separator ngăn cách arrInt và arrInt2
                        val combined = ArrayList<ArrayList<Int>>()
                        combined.addAll(arrInt)
                        combined.add(arrayListOf(-1, -1))  // separator
                        combined.addAll(arrInt2)
                        viewModel.addAvatar(
                            AvatarModel(
                                path,
                                arrBlackCentered[blackCentered].avt,
                                arrBlackCentered[blackCentered].checkDataOnline,
                                fromList(combined), isFlipped = !checkRevert
                            )
                        )
                        startActivity(
                            Intent(this@CustomviewActivity, BackgroundActivity::class.java)
                                .putExtra("path", path)
                        )
                    } else {
                        showToast(this@CustomviewActivity, R.string.save_failed)
                    }
                }
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
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    private suspend fun checkNetwork(): Boolean {
        if (!DataHelper.arrBlackCentered[blackCentered].checkDataOnline) return true
        if (!isInternetAvailable(applicationContext)) {
            DialogExit(this@CustomviewActivity, "loadingnetwork").show(); return false
        }
        val hasInternet =
            withContext(Dispatchers.IO) { isNetworkConnected(this@CustomviewActivity) }
        if (!hasInternet) {
            DialogExit(this@CustomviewActivity, "networked").show(); return false
        }
        return true
    }

    private fun handlePartClick(
        pos: Int, type: String,
        data: ArrayList<BodyPartModel>, arr: ArrayList<ArrayList<Int>>,
        navPos: Int, colorPos: Int,
        adapterP: PartAdapter,
        submitFn: (Int, Int, (() -> Unit)?) -> Unit,
        putImageFn: (String, Int, Boolean, Int?, Int?) -> Unit
    ) {
        val listPath = data[navPos].listPath[colorPos].listPath
        when (type) {
            "none" -> {
                adapterP.setPos(pos); submitFn(navPos, colorPos, null)
                arr[navPos][0] = pos; arr[navPos][1] = colorPos
                putImageFn(data[navPos].icon, pos, true, null, null)
            }

            "dice" -> {
                val x = when (listPath[0]) {
                    "none" -> if (listPath.size > 3) (2..<listPath.size).random() else 2
                    else -> if (listPath.size > 2) (1..<listPath.size).random() else 1
                }
                adapterP.setPos(x); submitFn(navPos, colorPos, null)
                arr[navPos][0] = x; arr[navPos][1] = colorPos
                putImageFn(data[navPos].icon, x, false, null, null)
            }

            else -> {
                adapterP.setPos(pos); submitFn(navPos, colorPos, null)
                arr[navPos][0] = pos; arr[navPos][1] = colorPos
                putImageFn(data[navPos].icon, pos, false, null, null)
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

    override fun onBackPressed() {
        DialogExit(this@CustomviewActivity, "exit").apply {
            onClick = { finish() }
        }.show()
    }
}
