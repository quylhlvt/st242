package com.anime.couple.couplemaker.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.anime.couple.couplemaker.data.callapi.reponse.DataResponse
import com.anime.couple.couplemaker.data.callapi.reponse.LoadingStatus
import com.anime.couple.couplemaker.data.model.BodyPartModel
import com.anime.couple.couplemaker.data.model.CharacterResponse
import com.anime.couple.couplemaker.data.model.ColorModel
import com.anime.couple.couplemaker.data.model.CustomModel
import com.anime.couple.couplemaker.data.model.LanguageModel
import com.anime.couple.couplemaker.data.model.SelectedModel
import com.anime.couple.couplemaker.data.repository.ApiRepository
import com.anime.couple.couplemaker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.collections.addAll
import kotlin.collections.get
import kotlin.div
import kotlin.math.roundToInt
data class LoadingProgress(val loaded: Int, val total: Int)

object DataHelper {
    val TAG = "quylh"
    val ASSET: String = "file:///android_asset/"
    var arrBlackCentered = arrayListOf<CustomModel>()
    var positionLanguageOld: Int = 0
    var check = true

    var listLanguage = arrayListOf<LanguageModel>(
        LanguageModel(
            "Spanish",
            "es",
            R.drawable.ic_flag_spanish
        ),
        LanguageModel(
            "French",
            "fr",
            R.drawable.ic_flag_french
        ),
        LanguageModel(
            "Hindi",
            "hi",
            R.drawable.ic_flag_hindi
        ),
        LanguageModel(
            "English",
            "en",
            R.drawable.ic_flag_english
        ),
        LanguageModel(
            "Portugeese",
            "pt",
            R.drawable.ic_flag_portugeese
        ),
        LanguageModel(
            "German",
            "de",
            R.drawable.ic_flag_germani
        ),
        LanguageModel(
            "Indonesian",
            "in",
            R.drawable.ic_flag_indo
        )
    )
    fun View.updateMargin( context: Context, leftDp: Int? = null, topDp: Int? = null, rightDp: Int? = null, bottomDp: Int? = null ) {
        val params = this.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        leftDp?.let { params.leftMargin = it.dp(context) }
        topDp?.let { params.topMargin = it.dp(context) }
        rightDp?.let { params.rightMargin = it.dp(context) }
        bottomDp?.let { params.bottomMargin = it.dp(context) }
        this.layoutParams = params }
    fun View.dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }
    fun Context.dp(value: Float): Float = value * resources.displayMetrics.density
    fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).roundToInt()
    fun View.setMargins(
        left: Int? = null,
        top: Int? = null,
        right: Int? = null,
        bottom: Int? = null
    ) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            left ?: params.leftMargin,
            top ?: params.topMargin,
            right ?: params.rightMargin,
            bottom ?: params.bottomMargin
        )
        layoutParams = params
    }
    var arrBg = arrayListOf<String>()
    var arrBgText = arrayListOf<String>()
    var arrStiker = arrayListOf<String>()
    var activeCharacter = 1
    var arrIntChar1 = arrayListOf<ArrayList<Int>>()  // state nhân vật 1
    var arrIntChar2 = arrayListOf<ArrayList<Int>>()
    //lớp view
    var listImageSortView = arrayListOf<String>()
    var assetsLoadProgress = MutableLiveData<LoadingProgress>()

    //thứ tự navigation
    var listImage = arrayListOf<String>()
    // Trả về nav items cho rcvNav theo activeCharacter
    // DataHelper.kt
    fun getNavItemsForActiveCharacter(): ArrayList<CustomModel> {
        val result = arrayListOf<CustomModel>()

        for (catModel in arrBlackCentered) {
            val filteredCat = CustomModel(catModel.avt, arrayListOf(), catModel.checkDataOnline)

            fun getY(bodyPart: BodyPartModel): Int {
                val folder = bodyPart.icon
                    .substringBeforeLast("/").substringAfterLast("/")
                return folder.split("-").getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
            }

            // Group theo y-position
            val groupedByY = catModel.bodyPart.groupBy { getY(it) }

            // Với mỗi y-position, ưu tiên lấy activeCharacter,
            // nếu không có thì fallback về charType=1
            for ((_, partsAtY) in groupedByY.entries.sortedBy { it.key }) {
                val activePart = partsAtY.firstOrNull { it.charType == activeCharacter }
                val fallbackPart = partsAtY.firstOrNull { it.charType == 1 }
                val chosen = activePart ?: fallbackPart
                if (chosen != null) {
                    filteredCat.bodyPart.add(chosen)
                }
            }

            if (filteredCat.bodyPart.isNotEmpty()) result.add(filteredCat)
        }

        return result
    }

    suspend fun Context.getData(apiRepository: ApiRepository) = coroutineScope {
        val job1 = async(Dispatchers.IO) {
          DataHelper.arrBlackCentered.clear()
            DataHelper.arrDataOnline.postValue(
                DataResponse.DataLoading(
                    LoadingStatus.Loading
                )
            )
            var assetManager = assets
            val data = assetManager.list("data")
            for (mData in data!!) {     //mData - cat1
                val subFolders = assetManager.list("data/$mData") ?: continue
                val catModel = CustomModel(
                    "",
                    arrayListOf()
                )
                for (bodypart in subFolders) {
                    //bodypart: 1_3
                    val subBodyPart = assetManager.list("data/$mData/$bodypart")
                        ?.map { "data/$mData/$bodypart/$it" }
                    if (subBodyPart == null || subBodyPart.size == 0) {
                        catModel.avt = DataHelper.ASSET + "data/$mData/$bodypart"
                    } else {
                        var icon = DataHelper.ASSET + subBodyPart.find { it.contains("nav.") }
                        val parts = bodypart.split("-")
                        val x = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val y = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val charType = parts.getOrNull(2)?.toIntOrNull() ?: 1
                        var mbodyPathModel = BodyPartModel(icon, arrayListOf(), charType = charType)

                        subBodyPart.forEach { mSubBodyPart ->
                            if (!mSubBodyPart.contains("nav.")) {
                                val itemColor = assetManager.list("$mSubBodyPart")
                                    ?.map { "$mSubBodyPart/$it" }
                                if (itemColor == null || itemColor.isEmpty()) {
                                    val fileName = mSubBodyPart.substringAfterLast("/")
                                    if (fileName.startsWith("thumb_")) {
                                        mbodyPathModel.listThumbPath.add("${DataHelper.ASSET}$mSubBodyPart")
                                    } else {
                                        // This is a color folder (e.g. "FFFFFF") — check if it has images
                                        // This branch is for flat single images, color folders go to listPath below
                                        mbodyPathModel.listSinglePath.add("${DataHelper.ASSET}$mSubBodyPart")
                                    }
                                } else {
                                    // itemColor is non-empty → this is a color subfolder
                                    val fileName = mSubBodyPart.substringAfterLast("/")
                                    if (fileName.startsWith("thumb_")) {
                                        // thumb_ folder — shouldn't happen, but guard anyway
                                        mbodyPathModel.listThumbPath.add("${DataHelper.ASSET}$mSubBodyPart")
                                    } else {
                                        mbodyPathModel.listPath.add(
                                            ColorModel(
                                                fileName,
                                                itemColor.map { "${DataHelper.ASSET}$it" } as ArrayList<String>
                                            )
                                        )
                                    }
                                }
                            }
                        }

// Sau forEach:
                        if (mbodyPathModel.listPath.isEmpty() && mbodyPathModel.listSinglePath.isNotEmpty()) {
                            if (mbodyPathModel.listThumbPath.isEmpty()) {
                                // Không có folder màu, không có thumb_ → flat list từ API → tách đôi
                                mbodyPathModel.listSinglePath.sort()
                                val half = mbodyPathModel.listSinglePath.size / 2
                                val realImages = ArrayList(mbodyPathModel.listSinglePath.subList(0, half))
                                val thumbImages = ArrayList(mbodyPathModel.listSinglePath.subList(half, mbodyPathModel.listSinglePath.size))
                                mbodyPathModel.listThumbPath.addAll(thumbImages)
                                mbodyPathModel.listPath = arrayListOf(ColorModel("", realImages))
                            } else {
                                // Có thumb_ → data local, dùng bình thường
                                mbodyPathModel.listThumbPath.sortBy { it.substringAfterLast("thumb_").substringBeforeLast(".").toIntOrNull() ?: 0 }
                                mbodyPathModel.listSinglePath.sortBy { it.substringAfterLast("/").substringBeforeLast(".").toIntOrNull() ?: 0 }
                                mbodyPathModel.listPath = arrayListOf(ColorModel("", ArrayList(mbodyPathModel.listSinglePath)))
                            }
                        }
                        catModel.bodyPart.add(mbodyPathModel)
                    }
                }
                // ✅ THAY BẰNG đoạn này:
// Tìm minY của từng charType để xác định nav đầu tiên
                val minYPerCharType = catModel.bodyPart
                    .groupBy { it.charType }
                    .mapValues { (_, parts) ->
                        parts.mapNotNull { bp ->
                            bp.icon.substringBeforeLast("/").substringAfterLast("/")
                                .split("-").getOrNull(1)?.toIntOrNull()
                        }.minOrNull() ?: Int.MAX_VALUE
                    }

                catModel.bodyPart.forEach { bodyPart ->
                    try {
                        val folderY = bodyPart.icon
                            .substringBeforeLast("/").substringAfterLast("/")
                            .split("-").getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE

                        val minYForThisChar = minYPerCharType[bodyPart.charType] ?: Int.MAX_VALUE
                        val isFirstNav = (folderY == minYForThisChar)

                        bodyPart.listPath.forEach { colorModel ->
                            if (isFirstNav) {
                                // Nav đầu tiên của mỗi charType: chỉ dice
                                if (colorModel.listPath.firstOrNull() != "dice") {
                                    colorModel.listPath.add(0, "dice")
                                }
                            } else {
                                // Nav còn lại: none + dice
                                if (colorModel.listPath.firstOrNull() != "none") {
                                    colorModel.listPath.add(0, "none")
                                    colorModel.listPath.add(1, "dice")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "getData prepend error: $e")
                    }
                }
                DataHelper.arrBlackCentered.add(catModel)
            }
        }
        val job2 = async(Dispatchers.IO) {
            getPathBG()
        }
        val job3 = async(Dispatchers.IO) {
            getPathBGText()
        }
        val job4 = async(Dispatchers.IO) {
            getPathStiker()
        }
        awaitAll(job1, job2, job3, job4)
            DataHelper.callApi(apiRepository)

    }

    //    var arrDataOnline = MutableLiveData<CharacterResponse>()
    var arrDataOnline = MutableLiveData<DataResponse<CharacterResponse?>>()
    fun callApi(apiRepository: ApiRepository) {
        GlobalScope.launch {
            arrDataOnline.postValue(DataResponse.DataSuccess(apiRepository.getFigure()))
        }
    }

    fun Context.getPathBGText() {
        arrBgText.clear()
        var assetManager = assets
        var subFolders = assetManager.list("BG_Text")
        arrBgText = subFolders?.map { "${ASSET}BG_Text/$it" }!! as ArrayList<String>
    //  arrBgText.add(0, "")
    }

    fun Context.getPathStiker() {
        arrStiker.clear()
        var assetManager = assets
        var subFolders = assetManager.list("sticker")
        arrStiker = subFolders?.map { "${ASSET}sticker/$it" }!! as ArrayList<String>
//        arrStiker.add(0, "")
    }

    fun Context.getPathBG() {
        arrBg.clear()
        var assetManager = assets
        val subFolders = assetManager.list("bg") ?: emptyArray()

        arrBg = subFolders
            .sortedWith { a, b ->
                val numA = a.substringBeforeLast(".").toIntOrNull() ?: 0
                val numB = b.substringBeforeLast(".").toIntOrNull() ?: 0

                when {
                    numA == 1 && numB == 1 -> 0
                    numA == 1 -> 1           // 1 xuống cuối
                    numB == 1 -> -1
                    else -> numB - numA      // sort giảm dần
                }
            }
            .map { "${ASSET}bg/$it" }
            .toCollection(ArrayList())
        arrBg.add(0, "")
    }

    fun extractNumberFromPath(path: String): Int {
        val regex = Regex("character_(\\d+)")
        val match = regex.find(path)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
    }

    fun getBackgroundColorDefault(context: Context): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color.color_1
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._4ba6ac
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._dcd5ff
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._b7e9f8
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._ffb4b9
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._ebaef1
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._f5d0c8
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._fde6c4
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._d2ece9
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._eae5e1
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._e1faf7
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._ffcdfe
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._fffed2
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._afcffe
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._cbe7db
                )
            ),
        )
    }

    fun getTextFontDefault(): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(color = R.font.itim_regular),
            SelectedModel(color = R.font.italianno_regular),
            SelectedModel(color = R.font.kranky_regular),
            SelectedModel(color = R.font.damion_regular),
            SelectedModel(color = R.font.dynalight_regular),
            SelectedModel(color = R.font.baloo2_regular),
            SelectedModel(color = R.font.bubblegum_sans_regular),
            SelectedModel(color = R.font.cherry_bomb_regular),
            SelectedModel(color = R.font.carattere),
            SelectedModel(color = R.font.digital_numbers),
            SelectedModel(color = R.font.dynalight),
            SelectedModel(color = R.font.edwardian_script_itc),
            SelectedModel(color = R.font.vni_ongdo)
        )
    }

    fun getTextColorDefault(context: Context): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color.color_9
                )
            ),

            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._ffa843
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._deea88
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color.FF3F3F
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._d37728
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._98ffec
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._ffa6a6
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._95ce9a
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._f4ff79
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._ff9efb
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._989cf3
                )
            ),
            SelectedModel(
                color = ContextCompat.getColor(
                    context,
                    R.color._4ba6ac
                )
            )
        )
    }
}