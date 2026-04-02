package com.cute.anime.avatarmaker.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.cute.anime.avatarmaker.data.callapi.reponse.DataResponse
import com.cute.anime.avatarmaker.data.callapi.reponse.LoadingStatus
import com.cute.anime.avatarmaker.data.model.BodyPartModel
import com.cute.anime.avatarmaker.data.model.CharacterResponse
import com.cute.anime.avatarmaker.data.model.ColorModel
import com.cute.anime.avatarmaker.data.model.CustomModel
import com.cute.anime.avatarmaker.data.model.LanguageModel
import com.cute.anime.avatarmaker.data.model.SelectedModel
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.R
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

    //lớp view
    var listImageSortView = arrayListOf<String>()
    var assetsLoadProgress = MutableLiveData<LoadingProgress>()

    //thứ tự navigation
    var listImage = arrayListOf<String>()

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
                        val (x, y) = bodypart.split("-").map { it.toInt() }
                        var mbodyPathModel = BodyPartModel(icon, arrayListOf())

                        subBodyPart.forEach { mSubBodyPart ->
                            if (!mSubBodyPart.contains("nav.")) {
                                val itemColor = assetManager.list("$mSubBodyPart")?.map { "$mSubBodyPart/$it" }
                                if (itemColor == null || itemColor.isEmpty()) {
                                    val fileName = mSubBodyPart.substringAfterLast("/")
                                    if (fileName.startsWith("thumb_")) {
                                        mbodyPathModel.listThumbPath.add("${DataHelper.ASSET}$mSubBodyPart")
                                    } else {
                                        mbodyPathModel.listSinglePath.add("${DataHelper.ASSET}$mSubBodyPart")
                                    }
                                } else {
                                    mbodyPathModel.listPath.add(
                                        ColorModel(
                                            mSubBodyPart.substringAfterLast("/"),
                                            itemColor.map { "${DataHelper.ASSET}$it" } as ArrayList<String>
                                        )
                                    )
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
                catModel.bodyPart.forEach {
                    try {
                        if (it.icon.substringBeforeLast("/").substringAfterLast("/")
                                .substringAfter("-")
                                .toInt() == 1
                        ) {
                            it.listPath.forEach {
                                if (it.listPath[0] != "dice") {
                                    it.listPath.add(0, "dice")
                                }
                            }
                        } else {
                            it.listPath.forEach {
                                if (it.listPath[0] != "none") {
                                    it.listPath.add(0, "none")
                                    it.listPath.add(1, "dice")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(DataHelper.TAG, "getData: $it")
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