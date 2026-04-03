package com.cute.anime.avatarmaker.ui.my_creation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.data.callapi.reponse.DataResponse
import com.cute.anime.avatarmaker.data.callapi.reponse.LoadingStatus
import com.cute.anime.avatarmaker.data.model.BodyPartModel
import com.cute.anime.avatarmaker.data.model.ColorModel
import com.cute.anime.avatarmaker.data.model.CustomModel
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.databinding.ActivityMyCreationBinding
import com.cute.anime.avatarmaker.dialog.CreateNameDialog
import com.cute.anime.avatarmaker.dialog.DialogExit
import com.cute.anime.avatarmaker.ui.customview.CustomviewActivity
import com.cute.anime.avatarmaker.ui.customview.CustomviewViewModel
import com.cute.anime.avatarmaker.ui.main.MainActivity
import com.cute.anime.avatarmaker.ui.permision.PermissionViewModel
import com.cute.anime.avatarmaker.ui.view.ViewActivity
import com.cute.anime.avatarmaker.utils.CONST
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.DataHelper.dp
import com.cute.anime.avatarmaker.utils.DataHelper.getData
import com.cute.anime.avatarmaker.utils.DataHelper.setMargins
import com.cute.anime.avatarmaker.utils.PermissionHelper
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import com.cute.anime.avatarmaker.utils.hide
import com.cute.anime.avatarmaker.utils.isInternetAvailable
import com.cute.anime.avatarmaker.utils.isNetworkConnected
import com.cute.anime.avatarmaker.utils.newIntent
import com.cute.anime.avatarmaker.utils.onClick
import com.cute.anime.avatarmaker.utils.onClickCustom
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.saveFileToExternalStorage
import com.cute.anime.avatarmaker.utils.scanMediaFile
import com.cute.anime.avatarmaker.utils.share.telegram.TelegramSharing
import com.cute.anime.avatarmaker.utils.share.whatsapp.IdGenerator
import com.cute.anime.avatarmaker.utils.share.whatsapp.StickerBook
import com.cute.anime.avatarmaker.utils.share.whatsapp.StickerPack
import com.cute.anime.avatarmaker.utils.share.whatsapp.WhatsappSharingActivity
import com.cute.anime.avatarmaker.utils.shareListFiles
import com.cute.anime.avatarmaker.utils.show
import com.cute.anime.avatarmaker.utils.showDialogNotifiListener
import com.cute.anime.avatarmaker.utils.showSystemUI
import com.cute.anime.avatarmaker.utils.showToast
import com.cute.anime.avatarmaker.utils.toList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class MyCreationActivity : WhatsappSharingActivity<ActivityMyCreationBinding>() {
    @Inject
    lateinit var apiRepository: ApiRepository
    var checkCallingDataOnline = false

    val viewModel: CustomviewViewModel by viewModels()
    var checkAvatar = true
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var networkReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connectivityManager =
                context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            if (!checkCallingDataOnline) {
                if (networkInfo != null && networkInfo.isConnected) {
                    var checkDataOnline = false
                    DataHelper.arrBlackCentered.forEach {
                        if (it.checkDataOnline) {
                            checkDataOnline = true
                            return@forEach
                        }
                    }
                    if (!checkDataOnline) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            getData(apiRepository)
                        }
                    }
                } else {
                    if (DataHelper.arrBlackCentered.isEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            getData(apiRepository)
                        }
                    }
                }
            }
        }
    }

    @Inject
    lateinit var sharedPreference: SharedPreferenceUtils
    var arrPathAvatar = arrayListOf<String>()
    var arrPathDesign = arrayListOf<String>()
    val adapterAvatar by lazy {
        AvatarAdapter().apply {
            onClick = { pos, type ->
                when (type) {
                    "item" -> {
                        startActivity(
                            newIntent(
                                this@MyCreationActivity,
                                ViewActivity::class.java
                            ).putExtra("data", arrPathAvatar[pos]).putExtra("type", "avatar")
                        )
                    }

                    "delete" -> {
                        var dialog = DialogExit(
                            this@MyCreationActivity,
                            "delete"
                        )
                        dialog.onClick = {
                            viewModel.deleteAvatar(arrPathAvatar[pos])
                            File(arrPathAvatar[pos]).delete()
                            arrPathAvatar.removeAt(pos)
                            submitList(arrPathAvatar)
                            showToast(
                                this@MyCreationActivity,
                                R.string.file_deleted_successfully
                            )
                            checkNull()
                        }
                        dialog.show()
                    }

                    "edit" -> {
                        viewModel.getAvatar(arrPathAvatar[pos]) { avatar ->
                            if (avatar == null) {
                                File(arrPathAvatar[pos]).delete()
                                showToast(
                                    this@MyCreationActivity,
                                    R.string.image_error_please_try_again
                                )
                                finish()
                                return@getAvatar
                            }
                            // Now safe to use avatar without !! or null checks
                            if (!isInternetAvailable(this@MyCreationActivity) && avatar.online == true) {
                                DialogExit(
                                    this@MyCreationActivity,
                                    "loadingnetwork"
                                ).show()
                                return@getAvatar
                            } else {
                                lifecycleScope.launch {
                                    val hasInternet = withContext(Dispatchers.IO) {
                                        isNetworkConnected(this@MyCreationActivity)
                                    }
                                    if (!hasInternet && avatar.online == true ) {
                                        val dialog = DialogExit(this@MyCreationActivity, "networked")
                                        dialog.show()
                                    } else {
                                        var index =
                                            DataHelper.arrBlackCentered.indexOfFirst { it.avt == avatar.pathAvatar }
                                        if (index > -1) {
                                            startActivity(
                                                Intent(
                                                    this@MyCreationActivity,
                                                    CustomviewActivity::class.java
                                                ).putExtra("data", index)
                                                    .putExtra("arr", avatar.arr)
                                                    .putExtra("checkEdit", true)
                                                    .putExtra("isFlipped", avatar.isFlipped)
                                                    .putExtra("fileName", File(avatar.path).name)
                                            )
                                        } else {
                                            val dialog = DialogExit(
                                                this@MyCreationActivity,
                                                "awaitdata"
                                            )
                                            dialog.show()

                                        }

                                    }

                                }
                            }
                        }
                    }

                    "longclick" -> {
                        this@MyCreationActivity.binding.rcvAvatar.setMargins(
                            15.dp(this@MyCreationActivity),
                            16.dp(this@MyCreationActivity),
                            15.dp(this@MyCreationActivity),
                            50
                        )
                        checkLongClick = true
                        this@MyCreationActivity.checkLongClick = true

                        if (arrCheckTick.indexOf(pos) > -1) {
                            arrCheckTick.remove(pos)
                        } else {
                            arrCheckTick.add(pos)
                        }
                        submitList(arrPathAvatar)
                        this@MyCreationActivity.binding.apply {
                            imvTickAll.show()
                            imvDelete.show()
                            llBottom.show()


                            if (arrCheckTick.size == arrPathAvatar.size) {
                                imvTickAll.setImageResource(R.drawable.imv_tick_all_true)
                            } else {
                                imvTickAll.setImageResource(R.drawable.imv_tick_all_false)
                            }
                        }
                    }

                    "tick" -> {
                        if (pos in arrCheckTick) {
                            arrCheckTick.remove(pos)
                            this@MyCreationActivity.binding.imvTickAll.setImageResource(R.drawable.imv_tick_all_false)

                        } else {
                            arrCheckTick.add(pos)
                            if (arrCheckTick.size == arrPathAvatar.size) {
                                this@MyCreationActivity.binding.imvTickAll.setImageResource(R.drawable.imv_tick_all_true)
                            }
                        }
                        submitList(arrPathAvatar)
                    }
                }
            }
        }
    }
    val adapterDesign by lazy {
        DesignAdapter().apply {
            onClick = { pos, type ->
                when (type) {
                    "item" -> {
                        startActivity(
                            newIntent(
                                this@MyCreationActivity,
                                ViewActivity::class.java
                            ).putExtra("data", arrPathDesign[pos])
                        )
                    }

                    "delete" -> {
                        var dialog = DialogExit(
                            this@MyCreationActivity,
                            "delete"
                        )
                        dialog.onClick = {
                            File(arrPathDesign[pos]).delete()
                            arrPathDesign.removeAt(pos)
                            submitList(arrPathDesign)
                            showToast(
                                this@MyCreationActivity,
                                R.string.file_deleted_successfully
                            )
                            checkNull()
                        }
                        dialog.show()
                    }

                    "longclick" -> {
                        this@MyCreationActivity.binding.rcvDesign.setMargins(
                            15.dp(this@MyCreationActivity),
                            16.dp(this@MyCreationActivity),
                            15.dp(this@MyCreationActivity),
                            50
                        )
                        checkLongClick = true
                        this@MyCreationActivity.checkLongClick = true
                        if (arrCheckTick.indexOf(pos) > -1) {
                            arrCheckTick.remove(pos)
                        } else {
                            arrCheckTick.add(pos)
                        }
                        submitList(arrPathDesign)
                        this@MyCreationActivity.binding.apply {
                            imvTickAll.show()
                            imvDelete.show()
                            llBottom.show()

                            if (arrCheckTick.size == arrPathDesign.size) {
                                imvTickAll.setImageResource(R.drawable.imv_tick_all_true)
                            } else {
                                imvTickAll.setImageResource(R.drawable.imv_tick_all_false)
                            }
                        }
                    }

                    "tick" -> {
                        if (pos in arrCheckTick) {
                            arrCheckTick.remove(pos)
                            this@MyCreationActivity.binding.imvTickAll.setImageResource(R.drawable.imv_tick_all_false)
                        } else {
                            arrCheckTick.add(pos)
                            if (arrCheckTick.size == arrPathDesign.size) {
                                this@MyCreationActivity.binding.imvTickAll.setImageResource(
                                    R.drawable.imv_tick_all_true
                                )
                            }
                        }
                        submitList(arrPathDesign)
                    }
                }
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_my_creation

    override fun initView() {
        binding.apply {
            tvTitle.isSelected = true
            tvNoItem.isSelected = true
//
            binding.imvBack.isSelected = true
            txtTelegram.isSelected = true
            txtWhatsapp.isSelected = true
            tvDownload.isSelected = true
            tvShare.isSelected = true

            rcvAvatar.itemAnimator = null
            rcvAvatar.adapter = adapterAvatar

            rcvDesign.itemAnimator = null
            rcvDesign.adapter = adapterDesign
            getData()
            adapterAvatar.submitList(arrPathAvatar)
            adapterDesign.submitList(arrPathDesign)
            checkNull()
        }
        updateLayoutSticker()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
        DataHelper.arrDataOnline.observe(this) {
            it?.let {
                when (it.loadingStatus) {
                    LoadingStatus.Loading -> {
                        checkCallingDataOnline = true
                    }

                    LoadingStatus.Success -> {
                        if (DataHelper.arrBlackCentered.isNotEmpty() && !DataHelper.arrBlackCentered[0].checkDataOnline) {
                            checkCallingDataOnline = false
                            val listA =
                                (it as DataResponse.DataSuccess).body ?: return@observe
                            checkCallingDataOnline = true
                            val sortedMap = listA
                                .toList() // Chuyển map -> list<Pair<String, List<X10>>>
                                .sortedBy { (_, list) ->
                                    list.firstOrNull()?.level ?: Int.MAX_VALUE
                                }
                                .toMap()
                            sortedMap.forEach { key, list ->
                                var a = arrayListOf<BodyPartModel>()
                                list.forEachIndexed { index, x10 ->
                                    var b = arrayListOf<ColorModel>()
                                    x10.colorArray.split(",").forEach { coler ->
                                        var c = arrayListOf<String>()
                                        if (coler == "") {
                                            for (i in 1..x10.quantity) {
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${i}.png")
                                            }
                                            b.add(
                                                ColorModel(
                                                    "#",
                                                    c
                                                )
                                            )
                                        } else {
                                            for (i in 1..x10.quantity) {
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${coler}/${i}.png")
                                            }
                                            b.add(
                                                ColorModel(
                                                    coler,
                                                    c
                                                )
                                            )
                                        }
                                    }
                                    a.add(
                                        BodyPartModel(
                                            "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/${x10.parts}/nav.png",
                                            b
                                        )
                                    )
                                }
                                var dataModel =
                                    CustomModel(
                                        "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/avatar.png",
                                        a,
                                        true
                                    )
                                dataModel.bodyPart.forEach { mbodyPath ->
                                    if (mbodyPath.icon.substringBeforeLast("/")
                                            .substringAfterLast("/")
                                            .substringAfter("-") == "1"
                                    ) {
                                        mbodyPath.listPath.forEach {
                                            if (it.listPath[0] != "dice") {
                                                it.listPath.add(0, "dice")
                                            }
                                        }
                                    } else {
                                        mbodyPath.listPath.forEach {
                                            if (it.listPath[0] != "none") {
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

    override fun onRestart() {
        super.onRestart()

        arrPathAvatar.clear()
        arrPathDesign.clear()
        adapterDesign.submitList(arrPathDesign)
        adapterAvatar.submitList(arrPathAvatar)
        getData()
        hideLongClick()
    }

    var checkLongClick = false
    fun hideLongClick() {
        this@MyCreationActivity.binding.rcvAvatar.setMargins(
            15.dp(this@MyCreationActivity),
            16.dp(this@MyCreationActivity),
            15.dp(this@MyCreationActivity),
            0
        )
        this@MyCreationActivity.binding.rcvDesign.setMargins(
            15.dp(this@MyCreationActivity),
            16.dp(this@MyCreationActivity),
            15.dp(this@MyCreationActivity),
            0
        )
        checkLongClick = false
        binding.imvTickAll.setImageResource(R.drawable.imv_tick_all_false)
        binding.imvTickAll.visibility = View.GONE
        binding.llBottom.visibility = View.GONE
        binding.imvDelete.visibility = View.GONE
        if (checkAvatar)
            binding.layoutSticker.show() else binding.layoutSticker.hide()
        adapterAvatar.checkLongClick = false
        adapterDesign.checkLongClick = false
        adapterDesign.arrCheckTick.clear()
        adapterAvatar.arrCheckTick.clear()
        adapterAvatar.submitList(arrPathAvatar)
        adapterDesign.submitList(arrPathDesign)
        updateLayoutSticker()
        checkNull()
        showSystemUI()
    }

    fun checkNull() {
        if (checkAvatar) {
            if (arrPathAvatar.isEmpty()) {
                binding.llNull.show()
                updateLayoutSticker()
            } else {
                binding.llNull.hide()
                updateLayoutSticker()
            }
        } else {
            if (arrPathDesign.isEmpty()) {
                binding.llNull.show()
            } else {
                binding.llNull.hide()
            }
        }
    }

    fun getData() {
        arrPathDesign.clear()
        arrPathAvatar.clear()
        if (File(filesDir, "design").exists()) {
            File(filesDir, "design").listFiles()?.sortedByDescending { it.name }?.forEach {
                arrPathDesign.add(it.path)
            }
//            arrPathDesign
        }
        if (File(filesDir, "avatar").exists()) {
            File(filesDir, "avatar").listFiles()?.sortedByDescending { it.name }?.forEach {
                arrPathAvatar.add(it.path)
            }
//            arrPathAvatar
        }
    }

    override fun onBackPressed() {
        startActivity(
            newIntent(
                this@MyCreationActivity,
                MainActivity::class.java
            )
        )
    }

    @SuppressLint("ResourceAsColor")
    override fun initAction() {
        binding.apply {
            root.onSingleClick { hideLongClick() }

            rcvAvatar.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(
                    recyclerView: RecyclerView, motionEvent: MotionEvent
                ): Boolean {
                    return when {
                        motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(
                            motionEvent.x, motionEvent.y
                        ) != null -> false

                        else -> {
                            hideLongClick()
                            true
                        }
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                override fun onTouchEvent(
                    recyclerView: RecyclerView,
                    motionEvent: MotionEvent
                ) {
                }
            })
            rcvDesign.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(
                    recyclerView: RecyclerView, motionEvent: MotionEvent
                ): Boolean {
                    return when {
                        motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(
                            motionEvent.x, motionEvent.y
                        ) != null -> false

                        else -> {
                            hideLongClick()
                            true
                        }
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                override fun onTouchEvent(
                    recyclerView: RecyclerView,
                    motionEvent: MotionEvent
                ) {
                }
            })
            imvBack.onSingleClick {
                startActivity(
                    newIntent(
                        this@MyCreationActivity,
                        MainActivity::class.java
                    )
                )
            }
            btnDownload.onClick {
                if (adapterAvatar.arrCheckTick.isEmpty() && adapterDesign.arrCheckTick.isEmpty()) {
                    showToast(
                        this@MyCreationActivity,
                        R.string.you_have_not_selected_anything_yet
                    )
                    return@onClick
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (checkAvatar) {
                        if (adapterAvatar.arrCheckTick.isEmpty()) {
                            showToast(
                                this@MyCreationActivity,
                                R.string.you_have_not_selected_anything_yet
                            )
                        } else {
                            adapterAvatar.arrCheckTick.forEach {
                                saveFileToExternalStorage(
                                    this@MyCreationActivity,
                                    arrPathAvatar[it],
                                    ""
                                ) { check, path ->
                                    if (check) {
                                        scanMediaFile(this@MyCreationActivity, File(path))
                                    }
                                }
                            }
                            Toast.makeText(
                                this@MyCreationActivity,
                                getString(R.string.download_successfully) + " " + CONST.NAME_SAVE_FILE,
                                Toast.LENGTH_SHORT
                            ).show()
                            hideLongClick()
                        }
                    } else {
                        if (adapterDesign.arrCheckTick.isEmpty()) {
                            showToast(
                                this@MyCreationActivity,
                                R.string.you_have_not_selected_anything_yet
                            )
                        } else {
                            adapterDesign.arrCheckTick.forEach {
                                saveFileToExternalStorage(
                                    this@MyCreationActivity,
                                    arrPathDesign[it],
                                    ""
                                ) { check, path ->
                                    if (check) {
                                        scanMediaFile(this@MyCreationActivity, File(path))
                                    }
                                }
                            }
                            Toast.makeText(
                                this@MyCreationActivity,
                                getString(R.string.download_successfully) + " " + CONST.NAME_SAVE_FILE,
                                Toast.LENGTH_SHORT
                            ).show()
                            hideLongClick()
                        }
                    }
                } else {
                    handlePermissionRequest(isStorage = true)
                }

            }

            btnShareAll.onClick {
                if (checkAvatar) {
                    if (adapterAvatar.arrCheckTick.isEmpty()) {
                        showToast(
                            this@MyCreationActivity,
                            R.string.you_have_not_selected_anything_yet
                        )
                    } else {
                        var listPath = arrayListOf<String>()
                        adapterAvatar.arrCheckTick.forEach {
                            listPath.add(arrPathAvatar[it])
                        }
                        shareListFiles(
                            this@MyCreationActivity,
                            listPath
                        )
                        hideLongClick()
                    }
                } else {
                    if (adapterDesign.arrCheckTick.isEmpty()) {
                        showToast(
                            this@MyCreationActivity,
                            R.string.you_have_not_selected_anything_yet
                        )
                    } else {
                        var listPath = arrayListOf<String>()
                        adapterDesign.arrCheckTick.forEach {
                            listPath.add(arrPathDesign[it])
                        }
                        shareListFiles(
                            this@MyCreationActivity,
                            listPath
                        )
                        hideLongClick()
                    }
                }
            }
            btnTelegram.onSingleClick {  // Assuming btnTelegram exists in your layout
                handleTelegram()
            }
            btnWhatsApp.onSingleClick {  // Assuming btnWhatsapp exists in your layout
                handleWhatsapp()
            }

            imvTickAll.onClickCustom {
                if (checkAvatar) {
                    if (arrPathAvatar.size == adapterAvatar.arrCheckTick.size) {
                        imvTickAll.setImageResource(R.drawable.imv_tick_all_false)
                        adapterAvatar.arrCheckTick.clear()
                        adapterAvatar.submitList(arrPathAvatar)
                    } else {
                        imvTickAll.setImageResource(R.drawable.imv_tick_all_true)
                        adapterAvatar.arrCheckTick.clear()
                        arrPathAvatar.forEachIndexed { pos, _ ->
                            adapterAvatar.arrCheckTick.add(pos)
                        }
                        adapterAvatar.submitList(arrPathAvatar)
                    }
                } else {
                    if (arrPathDesign.size == adapterDesign.arrCheckTick.size) {
                        imvTickAll.setImageResource(R.drawable.imv_tick_all_false)
                        adapterDesign.arrCheckTick.clear()
                        adapterDesign.submitList(arrPathDesign)
                    } else {
                        imvTickAll.setImageResource(R.drawable.imv_tick_all_true)
                        adapterDesign.arrCheckTick.clear()
                        arrPathDesign.forEachIndexed { pos, _ ->
                            adapterDesign.arrCheckTick.add(pos)
                        }
                        adapterDesign.submitList(arrPathDesign)
                    }
                }
            }
            imvDelete.onSingleClick {
                if (checkAvatar) {
                    if (adapterAvatar.arrCheckTick.isEmpty()) {
                        showToast(
                            this@MyCreationActivity,
                            R.string.you_have_not_selected_anything_yet
                        )

                    } else {
                        var dialog = DialogExit(
                            this@MyCreationActivity,
                            "delete"
                        )
                        dialog.onClick = {
                            adapterAvatar.arrCheckTick.forEach { pos ->
                                viewModel.deleteAvatar(arrPathAvatar[pos])
                                File(arrPathAvatar[pos]).delete()
                            }
                            getData()
//                            arrPathAvatar.remove()
                            hideLongClick()
                            showToast(
                                this@MyCreationActivity,
                                R.string.file_deleted_successfully
                            )
                        }
                        dialog.show()
                    }
                } else {
                    if (adapterDesign.arrCheckTick.isEmpty()) {
                        showToast(
                            this@MyCreationActivity,
                            R.string.you_have_not_selected_anything_yet
                        )
                        return@onSingleClick
                    } else {
                        var dialog = DialogExit(
                            this@MyCreationActivity,
                            "delete"
                        )
                        dialog.onClick = {
                            adapterDesign.arrCheckTick.forEach { pos ->
                                viewModel.deleteAvatar(arrPathDesign[pos])
                                File(arrPathDesign[pos]).delete()
                            }
                            getData()
                            hideLongClick()
                            showToast(
                                this@MyCreationActivity,
                                R.string.file_deleted_successfully
                            )
                        }
                        dialog.show()
                    }
                }
            }

            selectDesign.onSingleClick {
                if (checkAvatar) {
                    checkAvatar = false
                    btnDesign.show()
                    btnAvatar.hide()
                    rcvDesign.show()
                    rcvAvatar.hide()
                    updateLayoutSticker()
                    hideLongClick()
                }
            }
            selectAvatar.onSingleClick {
                if (!checkAvatar) {
                    checkAvatar = true
                    btnAvatar.show()
                    btnDesign.hide()
                    rcvAvatar.show()
                    rcvDesign.hide()
                    updateLayoutSticker()
                    hideLongClick()
                }
            }
        }
    }

    private fun handleTelegram() {

        val listPath =
            if (checkLongClick) {
                adapterAvatar.arrCheckTick.map { arrPathAvatar[it] } as ArrayList
            } else {
                arrPathAvatar as ArrayList
            }
        if (listPath.isEmpty()) {
            showToast(
                this@MyCreationActivity,
                R.string.you_have_not_selected_anything_yet
            )
            return
        }

        val uriList = getUrisFromPathsTelegram(this@MyCreationActivity, listPath)
        if (uriList.isEmpty()) {
            showToast(
                this@MyCreationActivity,
                R.string.image_error_please_try_again
            )
            return
        }
        TelegramSharing.importToTelegram(this@MyCreationActivity, uriList)

        Log.d("telegramPath", "${uriList}")
        hideLongClick()

    }

    private fun handleWhatsapp() {
        val listPath =
            if (checkLongClick) {
                adapterAvatar.arrCheckTick.map { arrPathAvatar[it] } as ArrayList
            } else {
                arrPathAvatar as ArrayList
            }


        if (listPath.isEmpty()) {
            showToast(
                this@MyCreationActivity,
                R.string.you_have_not_selected_anything_yet
            )
            return
        }

        if (listPath.size < 3) {
            showToast(this, R.string.limit_3_items)
            return
        }
        if (listPath.size > 30) {
            showToast(this, R.string.limit_30_items)
            return
        }

        val dialog = CreateNameDialog(this)
        dialog.show()

        dialog.onYesClick = { packageName ->
            Log.d("whatApp", "${listPath}")

            addToWhatsappActivity(this, packageName, listPath) { pack ->
                pack?.let { addToWhatsapp(it) } ?: run {
                    showToast(
                        this@MyCreationActivity,
                        R.string.save_failed
                    )
                }
            }
            dialog.dismiss()

        }
        dialog.onNoClick = {
            dialog.dismiss()
        }
        hideLongClick()
    }

    private fun getUrisFromPathsTelegram(
        context: Context,
        paths: ArrayList<String>
    ): ArrayList<Uri> {
        val uriList = ArrayList<Uri>()
        // Xóa cache cũ trước để tránh chiếm bộ nhớ
        clearTelegramCache(context)

        paths.forEachIndexed { index, path ->
            try {
                val originalFile = File(path)
                if (!originalFile.exists()) return@forEachIndexed

                val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
                    ?: return@forEachIndexed

                // Giữ aspect ratio khi resize
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                bitmap.recycle()

                // Tên file cố định theo index để tránh trùng
                val cacheFile = File(context.cacheDir, "tg_sticker_$index.png")
                FileOutputStream(cacheFile).use { out ->
                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                resizedBitmap.recycle()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cacheFile
                )
                uriList.add(uri)

            } catch (e: Exception) {
                Log.e("TelegramSharing", "Error processing file $path: ${e.message}", e)
            }
        }

        return uriList
    }

    private fun clearTelegramCache(context: Context) {
        try {
            context.cacheDir.listFiles { file ->
                file.name.startsWith("tg_sticker_")
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e("TelegramSharing", "Error clearing cache: ${e.message}")
        }
    }

    private fun getUrisFromPaths(
        context: Context,
        paths: ArrayList<String>
    ): ArrayList<Uri> {
        val uriList = ArrayList<Uri>()

        for (path in paths) {
            val originalFile = File(path)
            if (!originalFile.exists()) continue

            // Decode bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalFile.absolutePath, options)

            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options, 512, 512)

            val bitmap =
                BitmapFactory.decodeFile(originalFile.absolutePath, options) ?: continue

            // Resize chính xác 512x512
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)

            // Chuyển sang WEBP để nhẹ hơn (Telegram thích .webp hơn .png)
            val stickerFile =
                File(context.cacheDir, "sticker_${System.currentTimeMillis()}.webp")

            FileOutputStream(stickerFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
            }

            // Tạo Uri bằng FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",  // Đảm bảo đúng trong AndroidManifest
                stickerFile
            )

            // QUAN TRỌNG: Grant quyền đọc cho Telegram (và mọi app bên ngoài)
            context.grantUriPermission(
                "org.telegram.messenger", // package name của Telegram
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            uriList.add(uri)

            // Dọn bộ nhớ
            resizedBitmap.recycle()
            bitmap.recycle()
        }

        return uriList
    }

    // Hàm tính sample size để giảm bộ nhớ khi decode
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun addToWhatsappActivity(
        context: Activity,
        packageName: String,
        list: ArrayList<String>,
        onResult: (StickerPack?) -> Unit
    ) {
        if (list.isEmpty()) return
        val uriList = getUrisFromPaths(context, list)
        val packId = IdGenerator.generateIdFromUrl(context, packageName)
        val stickerPack = StickerPack(
            packId,
            packageName,
            uriList,
            context
        )
        StickerBook.addPackIfNotAlreadyAdded(stickerPack)
        onResult(stickerPack)
    }

    private fun handlePermissionRequest(isStorage: Boolean) {
        val permissions = if (isStorage) {
            permissionViewModel.getStoragePermissions()
        } else {
            permissionViewModel.getNotificationPermissions()
        }
        // Kiểm tra đã có permission chưa
        if (PermissionHelper.checkPermissions(permissions, this@MyCreationActivity)) {
            performDownload()
            return
        }
        // Kiểm tra nếu đã từ chối nhiều lần → gợi ý vào Settings
        if (permissionViewModel.needGoToSettings(sharedPreference, isStorage)) {
            val dialogRes =
                if (isStorage) R.string.reques_storage else R.string.content_dialog_notification
            showDialogNotifiListener(dialogRes)
            return
        }

        // Request permission bình thường
        val requestCode =
            if (isStorage) CONST.REQUEST_STORAGE_PERMISSION else CONST.REQUEST_NOTIFICATION_PERMISSION
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    private fun performDownload() {
        adapterAvatar.arrCheckTick.forEach {
            saveFileToExternalStorage(
                this@MyCreationActivity,
                arrPathAvatar[it],
                ""
            ) { check, path ->
                if (check) {
                    scanMediaFile(
                        this@MyCreationActivity,
                        File(path)
                    )
                }
            }
        }
        Toast.makeText(
            this@MyCreationActivity,
            getString(R.string.download_successfully) + " " + CONST.NAME_SAVE_FILE,
            Toast.LENGTH_SHORT
        ).show()
        hideLongClick()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val isGranted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            CONST.REQUEST_STORAGE_PERMISSION -> {
                permissionViewModel.updateStorageGranted(sharedPreference, isGranted)

                if (isGranted) {
                    performDownload()
                }
            }
        }
    }

    private fun updateLayoutSticker() {
        binding.layoutSticker.visibility =
            if (checkAvatar && !arrPathAvatar.isEmpty()) View.VISIBLE else View.GONE
    }

}
