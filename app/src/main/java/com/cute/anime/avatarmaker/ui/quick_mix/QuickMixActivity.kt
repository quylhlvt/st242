package com.cute.anime.avatarmaker.ui.quick_mix

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.ConnectivityManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.data.model.CustomModel
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.ui.customview.CustomviewActivity
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.isInternetAvailable
import com.cute.anime.avatarmaker.utils.newIntent
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ActivityQuickMixBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.collections.set
import kotlin.coroutines.coroutineContext
import kotlin.math.abs


@AndroidEntryPoint
class QuickMixActivity : AbsBaseActivity<ActivityQuickMixBinding>() {
    private var sizeMix = 30
    private var isLoading = false
    private var isOfflineMode = false
    private val arrMix = arrayListOf<CustomModel>()
    @Inject
    lateinit var apiRepository: ApiRepository
    val adapter by lazy { QuickAdapter(this@QuickMixActivity) }

    // Thread-safe cache
    private val layerCache = ConcurrentHashMap<String, Bitmap>()
    val arrBitmap = ConcurrentHashMap<Int, Bitmap>()

    // Quản lý loading jobs
    private val loadingJobs = ConcurrentHashMap<Int, Job>()
    private val loadingPositions = ConcurrentHashMap<Int, Boolean>()

    // Giới hạn số lượng load đồng thời
    private val maxConcurrentLoads = 60
    private val currentLoadingCount = AtomicInteger(0)

    // Dispatcher với thread pool tối ưu
    private val bitmapDispatcher = Dispatchers.IO.limitedParallelism(maxConcurrentLoads)

    // Network state tracking
    @Volatile
    private var isNetworkAvailable = true

    // Track visible range để ưu tiên
    @Volatile
    private var currentVisibleRange = IntRange.EMPTY

    // Cache size limit
    private val maxCacheSize = 60

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val wasAvailable = isNetworkAvailable
            isNetworkAvailable = isInternetAvailable(this@QuickMixActivity)

            when {
                // Từ có mạng → mất mạng
                wasAvailable && !isNetworkAvailable -> {
                    isOfflineMode = true
                    cancelOnlineLoading()
                    loadOfflineMode()
                }

                // Từ mất mạng → có mạng
                !wasAvailable && isNetworkAvailable -> {
                    isOfflineMode = false
                    lifecycleScope.launch(Dispatchers.Main) {
                        // Reset trạng thái
                        loadingJobs.clear()
                        loadingPositions.clear()
                        currentLoadingCount.set(0)
                        layerCache.clear()
                        arrBitmap.clear()

                        sizeMix = 30
                        loadAllItems()
                    }
                }
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_quick_mix


    override fun onRestart() {
        super.onRestart()
    }

    override fun initView() {
//        binding.titleQuick.isSelected = true
        binding.imvBack.isSelected = true
        registerNetworkReceiver()
        isNetworkAvailable = isInternetAvailable(this@QuickMixActivity)
        isOfflineMode = !isNetworkAvailable
        if (DataHelper.arrBg.isEmpty()) {
            finish()
        } else {
            binding.rcv.itemAnimator = null
            binding.rcv.adapter = adapter
            binding.rcv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = binding.rcv.layoutManager as? LinearLayoutManager
                    val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
                    val lastVisible = layoutManager?.findLastVisibleItemPosition() ?: RecyclerView.NO_POSITION
                    if (firstVisible != RecyclerView.NO_POSITION && lastVisible != RecyclerView.NO_POSITION) {
                        val newRange = firstVisible..lastVisible
                        if (currentVisibleRange != newRange) {
                            currentVisibleRange = newRange
                            cancelNonVisibleJobs(firstVisible, lastVisible)
                            clearDistantCache(firstVisible, lastVisible)
                        }
                    }
                    preloadVisibleAndNext()
                }
            })
            if (isOfflineMode) {
                if (DataHelper.arrBlackCentered.isEmpty()) {
                    finish()
                    return
                }
                sizeMix = 30
                loadOfflineLastCharacter()
            } else {
                if (DataHelper.arrBlackCentered.isEmpty()) {
                    finish()
                    return
                }
                sizeMix = 30
                loadAllItems()
            }
        }
    }
    private fun registerNetworkReceiver() {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
    }
    private fun cancelOnlineLoading() {
        lifecycleScope.launch(Dispatchers.Main) {
            loadingJobs.values.forEach { it.cancel() }
            loadingJobs.clear()
            loadingPositions.clear()
            currentLoadingCount.set(0)
//            DialogExit(this@QuickMixActivity, "network").show()
        }
    }
    private fun loadOfflineMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Reset cache
            arrBitmap.clear()
            layerCache.clear()
            // ✅ Luôn load 20 item
            sizeMix = 30

            loadOfflineLastCharacter()
        }
    }

    private fun cancelNonVisibleJobs(firstVisible: Int, lastVisible: Int) {
        val preloadBuffer = 3
        val keepRange = (firstVisible - preloadBuffer)..(lastVisible + preloadBuffer)

        loadingJobs.entries.removeAll { (position, job) ->
            if (position !in keepRange) {
                job.cancel()
                loadingPositions.remove(position)
                true
            } else {
                false
            }
        }
    }

    private fun clearDistantCache(firstVisible: Int, lastVisible: Int) {
        val keepRange = (firstVisible - 15)..(lastVisible + 15)

        if (arrBitmap.size > maxCacheSize) {
            val toRemove = arrBitmap.keys.filter { it !in keepRange }.sortedBy {
                minOf(abs(it - firstVisible), abs(it - lastVisible))
            }.reversed().take(arrBitmap.size - maxCacheSize)

            toRemove.forEach { position ->
                arrBitmap.remove(position)?.recycle()
            }
        }
    }

    private fun loadOfflineLastCharacter() {
        if (DataHelper.arrBlackCentered.isEmpty()) return

        val offlineModels = DataHelper.arrBlackCentered.filter { !it.checkDataOnline }

        if (offlineModels.isEmpty()) return

        // ✅ Lấy 4 nhân vật cuối cùng từ danh sách offline
        val last4Characters = if (offlineModels.size >= 3) {
            offlineModels.takeLast(3)
        } else {
            offlineModels // Nếu ít hơn 4 thì lấy hết
        }

        val tempArrMix = arrayListOf<CustomModel>()
        val tempArrListImageSortView = mutableListOf<ArrayList<String>>()
        val resultList = mutableListOf<ArrayList<ArrayList<Int>>>()

        // ✅ Tạo 20 item từ 4 nhân vật cuối (mỗi nhân vật lặp 5 lần)
        repeat(sizeMix) { index ->
            val currentModel = last4Characters[index % last4Characters.size]

            val list = ArrayList<String>().apply {
                repeat(currentModel.bodyPart.size) { add("") }
            }
            currentModel.bodyPart.forEach {
                val (x, _) = it.icon.substringBeforeLast("/")
                    .substringAfterLast("/")
                    .split("-")
                    .map { it.toInt() }
                list[x - 1] = it.icon
            }
            tempArrListImageSortView.add(list)

            val i = arrayListOf<ArrayList<Int>>()
            list.forEach { data ->
                val x = currentModel.bodyPart.find { it.icon == data }
                val pair = if (x != null) {
                    val path = x.listPath[0].listPath
                    val color = x.listPath

                    val pathLimit = (path.size / 3).coerceAtLeast(2) // tối thiểu 2 để có cái random
                    val colorLimit = (color.size / 3).coerceAtLeast(1)

                    val randomValue = if (path[0] == "none") {
                        if (pathLimit > 2) (2 until pathLimit).random() else 2
                    } else {
                        if (pathLimit > 1) (1 until pathLimit).random() else 1
                    }
                    val randomColor = (0 until colorLimit).random()
                    arrayListOf(randomValue, randomColor)
                } else {
                    arrayListOf(-1, -1)
                }
                i.add(pair)
            }
            resultList.add(i)
            tempArrMix.add(currentModel)
        }

        // ✅ Clear và update
        arrMix.clear()
        arrMix.addAll(tempArrMix)

        adapter.arrListImageSortView.clear()
        adapter.arrListImageSortView.addAll(tempArrListImageSortView)

        adapter.listArrayInt.clear()
        adapter.listArrayInt.addAll(resultList)

        // ✅ Notify adapter
        adapter.submitList(ArrayList(arrMix))
        adapter.notifyDataSetChanged()

        preloadVisibleAndNext()
    }

    private fun loadAllItems() {
        if (isLoading) return

        if (DataHelper.arrBlackCentered.isEmpty()) {
            isLoading = false
            return
        }

        isLoading = true

        lifecycleScope.launch(Dispatchers.Default) {
            val tempArrMix = arrayListOf<CustomModel>()
            val tempArrListImageSortView = mutableListOf<ArrayList<String>>()
            val resultList = mutableListOf<ArrayList<ArrayList<Int>>>()

            for (pos in 0 until sizeMix) {
                val mModel = DataHelper.arrBlackCentered[pos % DataHelper.arrBlackCentered.size]

                val list = ArrayList<String>().apply {
                    repeat(mModel.bodyPart.size) { add("") }
                }
                mModel.bodyPart.forEach {
                    val (x, _) = it.icon.substringBeforeLast("/")
                        .substringAfterLast("/")
                        .split("-")
                        .map { it.toInt() }
                    list[x - 1] = it.icon
                }
                tempArrListImageSortView.add(list)

                val i = arrayListOf<ArrayList<Int>>()
                list.forEach { data ->
                    val x = mModel.bodyPart.find { it.icon == data }
                    val pair = if (x != null) {
                        val path = x.listPath[0].listPath
                        val color = x.listPath
                        val randomValue = if (path[0] == "none") {
                            if (path.size > 3) (2 until path.size).random() else 2
                        } else {
                            if (path.size > 2) (1 until path.size).random() else 1
                        }
                        val randomColor = (0 until color.size).random()
                        arrayListOf(randomValue, randomColor)
                    } else {
                        arrayListOf(-1, -1)
                    }
                    i.add(pair)
                }
                resultList.add(i)
                tempArrMix.add(mModel)
            }

            withContext(Dispatchers.Main) {
                // ✅ Clear và update
                arrMix.clear()
                arrMix.addAll(tempArrMix)

                adapter.arrListImageSortView.clear()
                adapter.arrListImageSortView.addAll(tempArrListImageSortView)

                adapter.listArrayInt.clear()
                adapter.listArrayInt.addAll(resultList)

                // ✅ Notify adapter
                adapter.submitList(ArrayList(arrMix))
                adapter.notifyDataSetChanged()

                isLoading = false

                preloadVisibleAndNext()
            }
        }
    }

    private fun preloadVisibleAndNext() {
        val layoutManager = binding.rcv.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION) return

        val positions = (firstVisible..minOf(lastVisible + 3, arrMix.size - 1)).toList()

        val visibleFirst = positions.filter { it in firstVisible..lastVisible }
        val nextItems = positions.filter { it !in firstVisible..lastVisible }
        val prioritized = visibleFirst + nextItems

        lifecycleScope.launch {
            prioritized.map { position ->
                async(bitmapDispatcher) {
                    if (!arrBitmap.containsKey(position) && loadingPositions.putIfAbsent(position, true) == null) {
                        loadBitmapAsync(position, position in firstVisible..lastVisible)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun loadBitmapAsync(position: Int, isVisible: Boolean = false) {
        val job = coroutineContext[Job]
        job?.let { loadingJobs[position] = it }

        try {
            // ✅ Check bounds
            if (position >= arrMix.size) return

            val model = arrMix[position]

            if (model.checkDataOnline && isOfflineMode) {
                return
            }

            if (!isVisible) {
                while (currentLoadingCount.get() >= maxConcurrentLoads) {
                    delay(20)
                    if (model.checkDataOnline && isOfflineMode) {
                        return
                    }
                }
            }

            currentLoadingCount.incrementAndGet()

            // ✅ Lấy data trực tiếp từ adapter bằng position
            if (position >= adapter.arrListImageSortView.size ||
                position >= adapter.listArrayInt.size) {
                currentLoadingCount.decrementAndGet()
                loadingPositions.remove(position)
                loadingJobs.remove(position)
                return
            }

            val listImageSortView = adapter.arrListImageSortView[position]
            val coordSet = adapter.listArrayInt[position]

            if (model.checkDataOnline && isOfflineMode) {
                return
            }

            val targetSize = calculateTargetSize(model, listImageSortView, coordSet)

            val merged = mergeBitmapAsync(model, listImageSortView, coordSet, targetSize.first, targetSize.second)

            if (merged != null) {
                arrBitmap[position] = merged

                withContext(Dispatchers.Main) {
                    adapter.notifyItemChanged(position)
                }
            }
        } catch (e: CancellationException) {
            // Job canceled
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentLoadingCount.decrementAndGet()
            loadingPositions.remove(position)
            loadingJobs.remove(position)
        }
    }

    private suspend fun calculateTargetSize(
        model: CustomModel,
        listImageSortView: List<String>,
        coordSet: ArrayList<ArrayList<Int>>
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            if (model.checkDataOnline && isOfflineMode) {
                return@withContext Pair(256, 256)
            }

            for (index in listImageSortView.indices) {
                val icon = listImageSortView[index]
                val coord = coordSet[index]

                if (coord[0] > 0) {
                    val targetPath = model.bodyPart
                        .find { it.icon == icon }
                        ?.listPath?.getOrNull(coord[1])
                        ?.listPath?.getOrNull(coord[0])

                    if (!targetPath.isNullOrEmpty()) {
                        val bmp = Glide.with(this@QuickMixActivity)
                            .asBitmap()
                            .load(targetPath)
                            .submit()
                            .get()

                        return@withContext Pair(bmp.width / 3, bmp.height / 3)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext Pair(256, 256)
    }

    private suspend fun loadSingleBitmap(path: String, width: Int, height: Int, isOnlineData: Boolean): Bitmap? {
        layerCache[path]?.let { return it }

        if (isOnlineData && isOfflineMode) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                if (isOnlineData && isOfflineMode) {
                    return@withContext null
                }

                val bmp = Glide.with(this@QuickMixActivity)
                    .asBitmap()
                    .load(path)
                    .override(width, height)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .submit()
                    .get()
                layerCache[path] = bmp
                bmp
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun mergeBitmapAsync(
        blackCentered: CustomModel,
        listImageSortView: List<String>,
        coordSet: ArrayList<ArrayList<Int>>,
        width: Int,
        height: Int
    ): Bitmap? = withContext(bitmapDispatcher) {
        try {
            if (blackCentered.checkDataOnline && isOfflineMode) return@withContext null

            val layers = listImageSortView.mapIndexed { index, icon ->
                async(Dispatchers.IO) {
                    if (blackCentered.checkDataOnline && isOfflineMode) return@async null
                    val coord = coordSet[index]
                    if (coord[0] > 0) {
                        val targetPath = blackCentered.bodyPart
                            .find { it.icon == icon }
                            ?.listPath?.getOrNull(coord[1])
                            ?.listPath?.getOrNull(coord[0])
                        if (!targetPath.isNullOrEmpty()) {
                            loadSingleBitmap(targetPath, width, height, blackCentered.checkDataOnline)
                        } else null
                    } else null
                }
            }.awaitAll().filterNotNull()

            if (layers.isEmpty()) return@withContext null

            // ✅ Dùng kích thước của layer đầu tiên làm chuẩn (thay vì width/height cố định)
            val baseWidth = layers[0].width
            val baseHeight = layers[0].height

            val merged = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(merged)

            layers.forEach { bmp ->
                // ✅ Scale về baseWidth/baseHeight giữ đúng tỉ lệ
                if (bmp.width == baseWidth && bmp.height == baseHeight) {
                    canvas.drawBitmap(bmp, 0f, 0f, null)
                } else {
                    val src = Rect(0, 0, bmp.width/2, bmp.height/2)
                    val dst = Rect(0, 0, baseWidth/2, baseHeight/2)
                    canvas.drawBitmap(bmp, src, dst, null)
                }
            }

            merged
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun requestBitmap(position: Int) {
        if (arrBitmap.containsKey(position)) return
        if (loadingPositions.containsKey(position)) return

        val layoutManager = binding.rcv.layoutManager as? LinearLayoutManager
        val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
        val lastVisible = layoutManager?.findLastVisibleItemPosition() ?: RecyclerView.NO_POSITION
        val isVisible = position in firstVisible..lastVisible

        lifecycleScope.launch(bitmapDispatcher) {
            loadBitmapAsync(position, isVisible)
        }
    }

    override fun initAction() {
        binding.apply {
            imvBack.onSingleClick {  finish() }
            adapter.onCLick = { position ->
                val model = arrMix[position]
                val index = DataHelper.arrBlackCentered.indexOf(model)

                if (index != -1) {
                    startActivity(
                        newIntent(this@QuickMixActivity, CustomviewActivity::class.java)
                            .putExtra("data", index)
                            .putExtra("arr", adapter.listArrayInt[position])
                    )
                }
            }
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        try {
//            unregisterReceiver(networkReceiver)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        loadingJobs.values.forEach { it.cancel() }
//        loadingJobs.clear()
//
//        // Clean up bitmaps
//        arrBitmap.values.forEach { it.recycle() }
//        arrBitmap.clear()
//        layerCache.values.forEach { it.recycle() }
//        layerCache.clear()
//    }
}