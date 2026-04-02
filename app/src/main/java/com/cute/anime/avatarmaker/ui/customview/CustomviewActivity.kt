package com.cute.anime.avatarmaker.ui.customview

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
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.data.model.AvatarModel
import com.cute.anime.avatarmaker.data.model.BodyPartModel
import com.cute.anime.avatarmaker.databinding.ActivityCustomizeBinding
import com.cute.anime.avatarmaker.dialog.DialogExit
import com.cute.anime.avatarmaker.ui.background.BackgroundActivity
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.DataHelper.arrBlackCentered
import com.cute.anime.avatarmaker.utils.fromList
import com.cute.anime.avatarmaker.utils.inhide
import com.cute.anime.avatarmaker.utils.isInternetAvailable
import com.cute.anime.avatarmaker.utils.isNetworkConnected
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.saveBitmap
import com.cute.anime.avatarmaker.utils.show
import com.cute.anime.avatarmaker.utils.showToast
import com.cute.anime.avatarmaker.utils.viewToBitmap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CustomviewActivity : AbsBaseActivity<ActivityCustomizeBinding>() {
    val viewModel: CustomviewViewModel by viewModels()
    var arrShowColor = arrayListOf<Boolean>()
    var countRandom = 0
    val adapterColor by lazy {
        ColorAdapter()
    }
    val adapterNav by lazy {
        NavAdapter(this@CustomviewActivity)
    }
    val adapterPart by lazy {
        PartAdapter()
    }

    private fun submitPartList(
        navPos: Int = adapterNav.posNav,
        colorPos: Int = adapterColor.posColor,
        commitCallback: (() -> Unit)? = null
    ) {
        val actualList = listData[navPos].listPath[colorPos].listPath
        adapterPart.listThumb = getThumbList(navPos, actualList)
        adapterPart.submitList(actualList, commitCallback)
    }

    private val iconToIndexMap = mutableMapOf<String, Int>()
    private fun getThumbList(navPos: Int, actualList: List<String>): List<String> {
        val thumbList = listData[navPos].listThumbPath
        // map 1-1: special item giữ nguyên, còn lại lấy thumb tương ứng
        var thumbIndex = 0
        return actualList.map { path ->
            when (path) {
                "none", "dice" -> path // special item không có thumb
                else -> thumbList.getOrElse(thumbIndex++) { path } // lấy thumb, fallback về path gốc
            }
        }
    }

    private val loadingLock = Any()
    private var canSave = true
    override fun getLayoutId(): Int = R.layout.activity_customize

    // Thêm map để cache index của icon (từ DataHelper.listImageSortView)
//    private val iconToIndexMap = mutableMapOf<String, Int>()
//    private fun applyGradientToLoadingText() {
//        binding.txtContent.post {
//            binding.txtContent.gradientHorizontal(
//                "#01579B".toColorInt(),
//                "#2686C6".toColorInt()
//            )
//        }
//        binding.txtTitle.setTextColor(ContextCompat.getColor(this,R.color.white))
//
//    }

    // Call this when you show loading
    override fun onRestart() {
        super.onRestart()
    }

    override fun initView() {
//        binding.txtContent.post {
//            binding.txtContent.gradientHorizontal(
//                startColor = "#01579B".toColorInt(),
//                endColor   = "#2686C6".toColorInt()
//            )
//        }

        binding.txtTitle.post { binding.txtTitle.isSelected = true }
        val isFlipped = intent.getBooleanExtra("isFlipped", false)
//        binding.txtTitle.setTextColor(ContextCompat.getColor(this,R.color.white))
        binding.btnSave.isSelected = true
        binding.imvBack.isSelected = true
        if (DataHelper.arrBlackCentered.size > 0) {
            binding.apply {
                rcvPart.adapter = adapterPart
                rcvPart.itemAnimator = null
                rcvColor.adapter = adapterColor
                rcvColor.itemAnimator = null
                rcvNav.adapter = adapterNav
                rcvNav.itemAnimator = null
                getData1()
                repeat(DataHelper.listImageSortView.size) {
                    listImg.add(AppCompatImageView(applicationContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        binding.rl.addView(this)
                    })
                }
                if (isFlipped) {
                    checkRevert = false
                    listImg.forEach { it.scaleX = -1f }
                }

                adapterNav.posNav = 0
                adapterNav.submitList(listData)

                adapterColor.setPos(arrInt[0][1])
                updateColorSectionVisibility(0)
                if (listData[adapterNav.posNav].listPath.size == 1) {
                    binding.llColor.visibility = View.GONE
//                    binding.imvShowColor.visibility = View.INVISIBLE
                } else {
                    binding.llColor.visibility = View.VISIBLE
//                    binding.imvShowColor.visibility = View.VISIBLE
                    adapterColor.submitList(listData[adapterNav.posNav].listPath)
                }
                adapterPart.setPos(arrInt[0][0])
                submitPartList()

                // ✅ Hiện loading, block save
                llLoading.visibility = View.VISIBLE
                canSave = false
                btnSave.alpha = 0.5f
            }

            if (arrIntHottrend != null) {
                adapterPart.setPos(arrInt[adapterNav.posNav][0])
                adapterColor.setPos(arrInt[adapterNav.posNav][1])
                submitPartList()
                if (listData[adapterNav.posNav].listPath.size == 1) {
                    binding.llColor.visibility = View.GONE
                } else {
                    binding.llColor.visibility = View.VISIBLE
                    adapterColor.submitList(listData[adapterNav.posNav].listPath)
                }
                updateColorSectionVisibility(adapterNav.posNav)
            }

            // ✅ Load tất cả ảnh song song, ẩn loading khi xong
            preloadInitialImages {
                binding.llLoading.visibility = View.GONE
                canSave = true
                binding.btnSave.alpha = 1f
            }
        } else {
            finish()
        }
    }

    private fun preloadInitialImages(onComplete: () -> Unit) {
        val pathsToLoad = mutableListOf<Pair<AppCompatImageView, String>>()

        listData.forEachIndexed { index, bodyPart ->
            val colorIdx = arrInt[index][1]
            val partIdx = arrInt[index][0]
            val path = bodyPart.listPath
                .getOrNull(colorIdx)?.listPath
                ?.getOrNull(partIdx) ?: return@forEachIndexed
            if (path == "none" || path == "dice" || path.isEmpty()) return@forEachIndexed

            val imgIndex = iconToIndexMap[bodyPart.icon] ?: return@forEachIndexed
            val view = listImg.getOrNull(imgIndex) ?: return@forEachIndexed
            pathsToLoad.add(view to path)
        }

        if (pathsToLoad.isEmpty()) {
            onComplete()
            return
        }

        val remaining = java.util.concurrent.atomic.AtomicInteger(pathsToLoad.size)

        pathsToLoad.forEach { (view, path) ->
            view.tag = path
            view.visibility = View.VISIBLE

            Glide.with(applicationContext)
                .load(path)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .priority(Priority.IMMEDIATE)
                .skipMemoryCache(false)
                .dontAnimate()
                .dontTransform()
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
                })
                .into(view)

        }

    }

    var listImg = arrayListOf<AppCompatImageView>()
    fun putImage(
        icon: String,
        pos: Int,
        checkRestart: Boolean = false,
        posNav: Int? = null,
        posColor: Int? = null
    ) {
        iconToIndexMap[icon]?.let { _pos ->
            handleVisibility(
                listImg[_pos],
                pos,
                checkRestart,
                posNav,
                posColor
            )
        }
    }

    // Thêm biến đếm số ảnh đang load
    private var loadingImagesCount = 0

    // Sửa lại handleVisibility
    private fun handleVisibility(
        view: ImageView, pos: Int, checkRestart: Boolean = false,
        posNav: Int? = null,
        posColor: Int? = null
    ) {
        if (checkRestart) {
            view.visibility = View.GONE
            view.tag = null
        } else {
            val navIndex = posNav ?: adapterNav.posNav
            val colorIndex = posColor ?: adapterColor.posColor
            val path = listData[navIndex].listPath[colorIndex].listPath[pos]

            // ← Thêm: nếu đang hiển thị đúng ảnh này rồi thì bỏ qua
            if (view.tag == path && view.visibility == View.VISIBLE) return

            view.tag = path
            view.visibility = View.VISIBLE

            synchronized(loadingLock) { loadingImagesCount++ }

            Glide.with(applicationContext)
                .load(path)                                      // ← dùng path đã tính sẵn
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)   // ← đổi từ ALL → RESOURCE
                .priority(Priority.IMMEDIATE)                    // ← đổi từ HIGH → IMMEDIATE
                .skipMemoryCache(false)
                .dontAnimate()
                .dontTransform()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingImagesCount--
                        checkAllImagesLoaded()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingImagesCount--
                        checkAllImagesLoaded()
                        return false
                    }
                })
                .into(view)
        }
    }

    private fun checkAllImagesLoaded() {
        synchronized(loadingLock) {
            if (loadingImagesCount <= 0) {
                loadingImagesCount = 0
                binding.root.post {
                    canSave = true
                    binding.btnSave.alpha = 1f
                }
            }
        }
    }

    var listData = arrayListOf<BodyPartModel>()

    //0 - path, 1 - color
    var arrInt = arrayListOf<ArrayList<Int>>()
    var blackCentered = 0
    var arrIntHottrend: ArrayList<ArrayList<Int>>? = null
    private fun getData1() {
        listImg.clear()
        binding.rl.removeAllViews()
        listData.clear()
        arrInt.clear()
        arrShowColor.clear()
        iconToIndexMap.clear()
        checkRevert = true

        DataHelper.listImageSortView.clear()
        DataHelper.listImage.clear()
        blackCentered = intent.getIntExtra("data", 0)
        arrIntHottrend = intent.getSerializableExtra("arr") as? ArrayList<ArrayList<Int>>
        var checkFirst = true

        repeat(DataHelper.arrBlackCentered[blackCentered].bodyPart.size) {
            DataHelper.listImageSortView.add("")
            DataHelper.listImage.add("")
        }

        val listSize = DataHelper.listImageSortView.size  // ✅ cache size

        DataHelper.arrBlackCentered[blackCentered].bodyPart.forEach {
            val (x, y) = it.icon.substringBeforeLast("/").substringAfterLast("/").split("-")
                .map { it.toInt() }

            // ✅ Chỉ set nếu index hợp lệ
            if (x - 1 < listSize) DataHelper.listImageSortView[x - 1] = it.icon
            if (y - 1 < listSize) DataHelper.listImage[y - 1] = it.icon
            iconToIndexMap[it.icon] = x - 1
        }

        DataHelper.listImage.forEachIndexed { index, icon ->
            val x = arrBlackCentered[blackCentered].bodyPart.indexOfFirst { it.icon == icon }
            val y = DataHelper.listImageSortView.indexOf(icon)
            if (x != -1) {
                arrShowColor.add(true)
                listData.add(arrBlackCentered[blackCentered].bodyPart[x])
                if (checkFirst) {
                    checkFirst = false
                    if (arrIntHottrend != null && y >= 0 && y < arrIntHottrend!!.size) {
                        arrInt.add(arrayListOf(arrIntHottrend!![y][0], arrIntHottrend!![y][1]))
                    } else {
                        arrInt.add(arrayListOf(1, 0))
                    }
                } else {
                    if (arrIntHottrend != null && y >= 0 && y < arrIntHottrend!!.size) {
                        arrInt.add(arrayListOf(arrIntHottrend!![y][0], arrIntHottrend!![y][1]))
                    } else {
                        arrInt.add(arrayListOf(0, 0))
                    }
                }
            }
        }
    }

    var checkRevert = true
    var checkHide = false
    override fun initAction() {
        adapterColor.onClick = {
            if (!DataHelper.arrBlackCentered[blackCentered].checkDataOnline || isInternetAvailable(
                    applicationContext
                )
            ) {
                lifecycleScope.launch {
                    val hasInternet = withContext(Dispatchers.IO) {
                        isNetworkConnected(this@CustomviewActivity)
                    }
                    if (DataHelper.arrBlackCentered[blackCentered].checkDataOnline &&!hasInternet) {
                        DialogExit(this@CustomviewActivity, "networked").show()
                    } else {
                        val recyclerState = binding.rcvPart.layoutManager?.onSaveInstanceState()
                        adapterColor.setPos(it)
                        adapterColor.submitList(listData[adapterNav.posNav].listPath)
                        arrInt[adapterNav.posNav][1] = it
                        submitPartList(commitCallback = {
                            binding.rcvPart.layoutManager?.onRestoreInstanceState(recyclerState)
                        })
                        putImage(listData[adapterNav.posNav].icon, adapterPart.posPath)
                    }
                }
            } else {
                DialogExit(
                    this@CustomviewActivity,
                    "loadingnetwork"
                ).show()
            }
        }
        adapterNav.onClick = {
            if (!DataHelper.arrBlackCentered[blackCentered].checkDataOnline || isInternetAvailable(
                    applicationContext
                )
            ) {
                lifecycleScope.launch {
                    val hasInternet = withContext(Dispatchers.IO) {
                        isNetworkConnected(this@CustomviewActivity)
                    }
                    if (DataHelper.arrBlackCentered[blackCentered].checkDataOnline &&!hasInternet) {
                        DialogExit(this@CustomviewActivity, "networked").show()
                    } else {
                        val newPos = it

                        // Validate và chuẩn bị indices
                        val currentBodyPart = listData[newPos]
                        val maxColorIndex = currentBodyPart.listPath.size - 1
                        val safeColorIndex = arrInt[newPos][1].coerceIn(0, maxColorIndex)

                        // Cập nhật arrInt nếu cần
                        if (arrInt[newPos][1] != safeColorIndex) {
                            arrInt[newPos][1] = safeColorIndex
                        }

                        val maxPartIndex =
                            currentBodyPart.listPath[safeColorIndex].listPath.size - 1
                        val safePartIndex = arrInt[newPos][0].coerceIn(0, maxPartIndex)

                        if (arrInt[newPos][0] != safePartIndex) {
                            arrInt[newPos][0] = safePartIndex
                        }

                        // Bắt đầu cập nhật UI
                        adapterNav.setPos(newPos)
                        adapterNav.submitList(listData)
                        // Set color adapter position trước
                        adapterColor.setPos(safeColorIndex)
                        // Cập nhật visibility
                        updateColorSectionVisibility(newPos)
                        // Submit color list nếu cần
                        val hasMultipleColors = currentBodyPart.listPath.size > 1
                        if (hasMultipleColors && arrShowColor[newPos]) {
                            adapterColor.submitList(currentBodyPart.listPath)
                            binding.root.postDelayed({
                                binding.rcvColor.smoothScrollToPosition(safeColorIndex)
                            }, 100)
                        } else if (hasMultipleColors) {
                            adapterColor.submitList(currentBodyPart.listPath)
                        }

                        // Cập nhật part adapter
                        if (adapterColor.posColor == safeColorIndex) {
                            adapterPart.setPos(safePartIndex)
                        } else {
                            adapterPart.setPos(-1)
                        }
                        submitPartList(navPos = it, colorPos = arrInt[it][1])

                        binding.root.postDelayed({
                            binding.rcvPart.smoothScrollToPosition(safePartIndex)
                        }, 100)
                    }
                }
            } else {
                DialogExit(this@CustomviewActivity, "loadingnetwork").show()
            }
        }
        adapterPart.onClick = { it, type ->
            if (!DataHelper.arrBlackCentered[blackCentered].checkDataOnline || isInternetAvailable(
                    this@CustomviewActivity
                )
            ) {
                lifecycleScope.launch {
                    val hasInternet = withContext(Dispatchers.IO) {
                        isNetworkConnected(this@CustomviewActivity)
                    }
                    if (DataHelper.arrBlackCentered[blackCentered].checkDataOnline &&!hasInternet) {
                        DialogExit(this@CustomviewActivity, "networked").show()
                    } else {
                        when (type) {
                            "none" -> {
                                adapterPart.setPos(it)
                                submitPartList()
                                arrInt[adapterNav.posNav][0] = it
                                arrInt[adapterNav.posNav][1] = adapterColor.posColor
                                putImage(listData[adapterNav.posNav].icon, it, true)
                            }

                            "dice" -> {
                                when (listData[adapterNav.posNav].listPath[adapterColor.posColor].listPath[0]) {
                                    "none" -> {
                                        if (listData[adapterNav.posNav].listPath[adapterColor.posColor].listPath.size > 3) {
                                            var x =
                                                (2..<listData[adapterNav.posNav].listPath[adapterColor.posColor].listPath.size).random()
                                            adapterPart.setPos(x)
                                            submitPartList()
                                            arrInt[adapterNav.posNav][0] = x
                                            arrInt[adapterNav.posNav][1] = adapterColor.posColor
                                            putImage(listData[adapterNav.posNav].icon, x)
                                        } else {
                                            adapterPart.setPos(2)
                                            submitPartList()
                                            arrInt[adapterNav.posNav][0] = 2
                                            arrInt[adapterNav.posNav][1] = adapterColor.posColor
                                            putImage(listData[adapterNav.posNav].icon, 2)
                                        }
                                    }

                                    "dice" -> {
                                        if (listData[adapterNav.posNav].listPath[adapterColor.posColor].listPath.size > 2) {
                                            var x =
                                                (1..<listData[adapterNav.posNav].listPath[adapterColor.posColor].listPath.size).random()
                                            adapterPart.setPos(x)
                                            submitPartList()
                                            arrInt[adapterNav.posNav][0] = x
                                            arrInt[adapterNav.posNav][1] = adapterColor.posColor
                                            putImage(listData[adapterNav.posNav].icon, x)
                                        } else {
                                            adapterPart.setPos(1)
                                            submitPartList()
                                            arrInt[adapterNav.posNav][0] = 1
                                            arrInt[adapterNav.posNav][1] = adapterColor.posColor
                                            putImage(listData[adapterNav.posNav].icon, 1)
                                            showToast(
                                                this@CustomviewActivity,
                                                R.string.the_layer_have_only_one_item
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                adapterPart.setPos(it)
                                submitPartList()
                                arrInt[adapterNav.posNav][0] = it
                                arrInt[adapterNav.posNav][1] = adapterColor.posColor
                                putImage(listData[adapterNav.posNav].icon, it)
                            }
                        }
                    }
                }
            } else {
                DialogExit(
                    this@CustomviewActivity, "network"
                ).show()
            }
        }
        binding.apply {
            imvShowColor.onSingleClick {
                val navPos = adapterNav.posNav
                if ((listData.getOrNull(navPos)?.listPath?.size ?: 0) <= 1) return@onSingleClick

                // FIX: không toggle, imvShowColor chỉ có nhiệm vụ MỞ
                if (navPos < arrShowColor.size) arrShowColor[navPos] = true

                if (llColor.visibility == View.VISIBLE) return@onSingleClick

                llColor.visibility = View.VISIBLE
                llColor.alpha = 0f
                llColor.animate().alpha(1f).setDuration(200).start()
            }

            imvEndColor.onSingleClick {
                val navPos = adapterNav.posNav
                // imvEndColor có nhiệm vụ ĐÓNG và nhớ trạng thái
                if (navPos < arrShowColor.size) arrShowColor[navPos] = false

                llColor.animate().alpha(0f).setDuration(200).withEndAction {
                    llColor.visibility = View.INVISIBLE
                }.start()
            }
            btnReset.onSingleClick {
                if (!arrBlackCentered[blackCentered].checkDataOnline || isInternetAvailable(
                        applicationContext
                    )
                ) {
                    lifecycleScope.launch {
                        val hasInternet = withContext(Dispatchers.IO) {
                            isNetworkConnected(this@CustomviewActivity)
                        }
                        if (DataHelper.arrBlackCentered[blackCentered].checkDataOnline &&!hasInternet) {
                            DialogExit(this@CustomviewActivity, "networked").show()
                        } else {
                            var dialog = DialogExit(
                                this@CustomviewActivity,
                                "reset"
                            )
                            dialog.onClick = {
                                DataHelper.listImage.forEach {
                                    putImage("0", 0, true)
                                }
                                arrInt.forEach { i ->
                                    i[0] = 0
                                    i[1] = 0
                                }
                                arrInt[0][0] = 1
                                arrInt[0][1] = 0

                                adapterPart.setPos(arrInt[adapterNav.posNav][0])
                                adapterColor.setPos(arrInt[adapterNav.posNav][1])
                                submitPartList()
                                updateColorSectionVisibility()
                                listData.forEachIndexed { index, bodyPartModel ->
                                    putImage(bodyPartModel.icon, 1, true)
                                }
                                putImage(listData[0].icon, 1, false, 0, 0)
                            }
                            dialog.show()
                        }
                    }
                } else {
                    DialogExit(
                        this@CustomviewActivity,
                        "loadingnetwork"
                    ).show()
                }
            }
            imvBack.onSingleClick {
                var dialog = DialogExit(
                    this@CustomviewActivity,
                    "exit"
                )
                dialog.onClick = {
                    finish()

                }
                dialog.show()
            }
            btnRevert.onSingleClick {
                checkRevert = !checkRevert
                if (checkRevert) {
                    listImg.forEach {
                        it.scaleX = 1f
                    }
                } else {
                    listImg.forEach {
                        it.scaleX = -1f
                    }
                }
            }
            btnDice.onSingleClick {
                if (!DataHelper.arrBlackCentered[blackCentered].checkDataOnline || isInternetAvailable(
                        applicationContext
                    )
                ) {
                    lifecycleScope.launch {
                        val hasInternet = withContext(Dispatchers.IO) {
                            isNetworkConnected(this@CustomviewActivity)
                        }
                        if (DataHelper.arrBlackCentered[blackCentered].checkDataOnline &&!hasInternet) {
                            DialogExit(this@CustomviewActivity, "networked").show()
                        } else {
                            // Disable save ngay lập tức
                            canSave = false
                            btnSave.alpha = 0.5f

                            countRandom++
//                    if (countRandom == 3) {
//                        btnDice.inhide()
//                    }
                            listData.forEachIndexed { index, partBody ->
                                if (partBody.listPath.size > 1) {
                                    arrInt[index][1] = (0..<partBody.listPath.size).random()
                                } else {
                                    arrInt[index][1] = 0
                                }

                                if (partBody.listPath[arrInt[index][1]].listPath[0] == "none") {
                                    if (partBody.listPath[arrInt[index][1]].listPath.size > 3) {
                                        arrInt[index][0] =
                                            (2..<partBody.listPath[arrInt[index][1]].listPath.size).random()
                                    } else {
                                        arrInt[index][0] = 2
                                    }
                                } else {
                                    if (partBody.listPath[arrInt[index][1]].listPath.size > 2) {
                                        arrInt[index][0] =
                                            (1..<partBody.listPath[arrInt[index][1]].listPath.size).random()
                                    } else {
                                        arrInt[index][0] = 1
                                    }
                                }

                                putImage(
                                    partBody.icon,
                                    arrInt[index][0],
                                    false,
                                    index,
                                    arrInt[index][1]
                                )
                            }

                            adapterPart.setPos(arrInt[adapterNav.posNav][0])
                            adapterColor.setPos(arrInt[adapterNav.posNav][1])
                            submitPartList()
                            updateColorSectionVisibility()

                            binding.rcvPart.post {
                                binding.rcvPart.smoothScrollToPosition(adapterPart.posPath)
                            }
                            binding.rcvColor.post {
                                binding.rcvColor.smoothScrollToPosition(adapterColor.posColor)
                            }

                            // Enable lại save sau 2 giây (thời gian load ảnh)
                            binding.root.postDelayed({
                                canSave = true
                                btnSave.alpha = 1f
                            }, 2000)
                        }
                    }
                } else {
                    DialogExit(this@CustomviewActivity, "loadingnetwork").show()
                }
            }
            llLoading.onSingleClick {
                showToast(
                    applicationContext,
                    R.string.please_wait_a_few_seconds_for_data_to_load
                )
            }
            btnSave.onSingleClick {
                if (!canSave) {
                    return@onSingleClick
                }
                llLoading.visibility = View.VISIBLE
//                applyGradientToLoadingText()
//                animationView.visibility = View.VISIBLE
                val a = arrBlackCentered[blackCentered].avt.split("/")
                var b = a[a.size - 2]

                saveBitmap(
                    this@CustomviewActivity,
                    viewToBitmap(rl),
                    intent.getStringExtra("fileName") ?: "",
                    true
                ) { it, path, pathOld ->
                    if (it) {
                        viewModel.deleteAvatar(pathOld)
                        llLoading.visibility = View.GONE
//                        animationView.visibility = View.GONE
                        //lop layer
                        var x = arrayListOf<ArrayList<Int>>()
                        DataHelper.listImageSortView.forEachIndexed { _pos, icon ->
                            var y = DataHelper.listImage.indexOf(
                                icon
                            )
                            x.add(arrInt[y])
                        }
                        viewModel.addAvatar(
                            AvatarModel(
                                path,
                                arrBlackCentered[blackCentered].avt,
                                arrBlackCentered[blackCentered].checkDataOnline,
                                fromList(x),
                                isFlipped = !checkRevert
                            )
                        )

                        startActivity(
                            Intent(
                                this@CustomviewActivity, BackgroundActivity::class.java
                            ).putExtra("path", path)
                        )


                    } else {
                        llLoading.visibility = View.GONE
//                        animationView.visibility = View.GONE
                        showToast(
                            this@CustomviewActivity, R.string.save_failed
                        )
                    }
                }
            }
            btnSee.onSingleClick {
                if (btnRevert.isInvisible) {
                    btnRevert.show()
                    btnReset.show()
                    btnSave.show()
                    if (listData[adapterNav.posNav].listPath.size > 1) {
                        if (arrShowColor[adapterNav.posNav]) {
                            binding.llColor.show()
                        }
                        imvShowColor.show()
                    }
//                    if (countRandom < 3) {
                    btnDice.show()
//                    }
                    llPart.show()
                    llNav.show()
                    btnSee.setImageResource(R.drawable.ic_show)
                } else {
                    btnRevert.inhide()
                    btnReset.inhide()
                    btnSave.inhide()
                    imvShowColor.inhide()
                    llColor.inhide()
                    btnDice.inhide()
                    llPart.inhide()
                    llNav.inhide()
                    btnSee.setImageResource(R.drawable.imv_see_false)
                }

            }
        }

    }

    private fun updateColorSectionVisibility(posNav: Int = adapterNav.posNav) {
        if (posNav < 0 || posNav >= listData.size) return

        val currentBodyPart = listData[posNav]
        val hasMultipleColors = currentBodyPart.listPath.size > 1

        // Sử dụng animate() để transition mượt mà hơn
        if (!hasMultipleColors) {
            binding.imvShowColor.animate().alpha(0f).setDuration(150).withEndAction {
                binding.imvShowColor.visibility = View.INVISIBLE
            }
            binding.llColor.animate().alpha(0f).setDuration(150).withEndAction {
                binding.llColor.visibility = View.GONE
            }
            return
        }

        // Có nhiều màu
        binding.imvShowColor.animate().alpha(1f).setDuration(150).withStartAction {
            binding.imvShowColor.visibility = View.VISIBLE
        }

        if (arrShowColor[posNav]) {
            binding.llColor.animate().alpha(1f).setDuration(150).withStartAction {
                binding.llColor.visibility = View.VISIBLE
            }
        } else {
            binding.llColor.animate().alpha(0f).setDuration(150).withEndAction {
                binding.llColor.visibility = View.GONE
            }
        }
    }

    override fun onBackPressed() {
        var dialog = DialogExit(
            this@CustomviewActivity,
            "exit"
        )
        dialog.onClick = {
            finish()
        }
        dialog.show()
    }
}
