package com.cute.anime.avatarmaker.ui.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.data.callapi.reponse.DataResponse
import com.cute.anime.avatarmaker.data.callapi.reponse.LoadingStatus
import com.cute.anime.avatarmaker.data.model.BodyPartModel
import com.cute.anime.avatarmaker.data.model.ColorModel
import com.cute.anime.avatarmaker.data.model.CustomModel
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.dialog.DialogExit
import com.cute.anime.avatarmaker.ui.customview.CustomviewViewModel
import com.cute.anime.avatarmaker.utils.CONST
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.DataHelper.getData
import com.cute.anime.avatarmaker.utils.checkPermision
import com.cute.anime.avatarmaker.utils.checkUsePermision
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.requesPermission
import com.cute.anime.avatarmaker.utils.saveFileToExternalStorage
import com.cute.anime.avatarmaker.utils.scanMediaFile
import com.cute.anime.avatarmaker.utils.shareListFiles
import com.cute.anime.avatarmaker.utils.showToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ActivityViewBinding
import com.cute.anime.avatarmaker.ui.customview.CustomviewActivity
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import com.cute.anime.avatarmaker.utils.hide
import com.cute.anime.avatarmaker.utils.isInternetAvailable
import com.cute.anime.avatarmaker.utils.isNetworkConnected
import com.cute.anime.avatarmaker.utils.show
import com.cute.anime.avatarmaker.utils.showDialogNotifiListener
import com.cute.anime.avatarmaker.utils.toList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ViewActivity : AbsBaseActivity<ActivityViewBinding>() {
    @Inject
    lateinit var apiRepository: ApiRepository

    @Inject
    lateinit var sharedPreferenceUtils: SharedPreferenceUtils
    var checkCallingDataOnline = false
    val viewModel: CustomviewViewModel by viewModels()
    var path = ""
    override fun getLayoutId(): Int = R.layout.activity_view
    override fun onRestart() {
        super.onRestart()
        // Reload ảnh mới khi quay lại
        Glide.with(applicationContext)
            .load(path)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(binding.imv)
    }

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

    override fun initView() {
        binding.imvBack.isSelected = true

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
                            val listA = (it as DataResponse.DataSuccess).body ?: return@observe
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
                                            val halfQuantity = maxOf(1, x10.quantity / 2)  // ✅ không có màu → chia 2
                                            for (i in 1..halfQuantity) {
                                                c.add(CONST.BASE_URL + "${CONST.BASE_CONNECT}/${x10.position}/${x10.parts}/${i}.png")
                                            }
                                            b.add(ColorModel("#", c))
                                        } else {
                                            for (i in 1..x10.quantity) {  // ✅ có màu → full quantity
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
                                var dataModel =
                                    CustomModel(
                                        "${CONST.BASE_URL}${CONST.BASE_CONNECT}$key/avatar.png",
                                        a,
                                        true
                                    )
                                // ✅ SAU
                                dataModel.bodyPart.forEach { mbodyPath ->
                                    if (mbodyPath.icon.substringBeforeLast("/")
                                            .substringAfterLast("/").substringAfter("-") == "1"
                                    ) {
                                        mbodyPath.listPath.forEach {
                                            if (it.listPath.isNotEmpty() && it.listPath[0] != "dice") {
                                                it.listPath.add(0, "dice")
                                            }
                                        }
                                    } else {
                                        mbodyPath.listPath.forEach {
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
        path = intent.getStringExtra("data").toString()
        if (intent?.getStringExtra("type") == "avatar") {
            binding.imvEdit.show()
        } else {
            binding.imvEdit.hide()

        }


        Glide.with(applicationContext).load(path)
            .diskCacheStrategy(DiskCacheStrategy.NONE) // ❌ không cache disk
            .skipMemoryCache(true).into(binding.imv)

    }

    override fun initAction() {
        binding.apply {
            tvEditShare.isSelected = true
            tvDownload.isSelected = true
            imvBack.onSingleClick { finish() }
            imvEdit.onSingleClick {
                viewModel.getAvatar(path) { avatar ->
                    if (!isInternetAvailable(this@ViewActivity) && avatar!!.online == true) {
                        DialogExit(
                            this@ViewActivity,
                            "loadingnetwork"
                        ).show()
                        return@getAvatar
                    }else{
                        lifecycleScope.launch {
                            val hasInternet = withContext(Dispatchers.IO) {
                                isNetworkConnected(this@ViewActivity)
                            }
                            if (!hasInternet) {

                                        DialogExit(this@ViewActivity, "networked").show()

                            }else{
                                if (avatar != null) {
                                    var index =
                                        DataHelper.arrBlackCentered.indexOfFirst { it.avt == avatar.pathAvatar }
                                    if (index > -1) {
                                        var a = avatar.pathAvatar.split("/")
                                        var b = a[a.size - 2]
                                        Log.d("indexb", b)
                                        startActivity(
                                            Intent(
                                                applicationContext,
                                                CustomviewActivity::class.java
                                            ).putExtra("data", index)
                                                .putExtra("arr", avatar.arr)
                                                .putExtra("isFlipped", avatar.isFlipped)
                                                .putExtra("fileName", File(avatar.path).name)
                                        )
                                    } else {
                                        lifecycleScope.launch {
                                            val dialog = DialogExit(
                                                this@ViewActivity,
                                                "awaitdata"
                                            )
                                            dialog.show()

                                        }
                                    }
                                }}

                        }
                        }
                }
            }
            imvDelete.onSingleClick {
                var dialog = DialogExit(
                    this@ViewActivity,
                    "delete"
                )
                dialog.onClick = {
                    File(path).delete()
                    finish()
                }
                dialog.show()
            }
            btnDownload.onSingleClick {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    !checkPermision(application)
                ) {

                    val deniedCount = sharedPreferenceUtils.getStoragePermission1()
                    if (deniedCount >= 2) {
                        showDialogNotifiListener(R.string.reques_storage)
                    } else {
                        ActivityCompat.requestPermissions(
                            this@ViewActivity,
                            checkUsePermision(),
                            CONST.REQUEST_STORAGE_PERMISSION
                        )
                    }
                } else {
                    saveFileToExternalStorage(
                        applicationContext,
                        path,
                        "",
                    ) { check, path ->
                        if (check) {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.download_successfully) + " " + CONST.NAME_SAVE_FILE,
                                Toast.LENGTH_SHORT
                            ).show()
                            scanMediaFile(
                                this@ViewActivity,
                                File(path)
                            )
                        } else {
                            showToast(
                                this@ViewActivity,
                                R.string.download_failed
                            )
                        }
                    }

                }
            }
            btnEditShareAll.onSingleClick {
//                if (intent?.getStringExtra("type") == "avatar"){
//                viewModel.getAvatar(path) { avatar ->
//                    if (avatar != null) {
//                        var a =
//                            DataHelper.arrBlackCentered.indexOfFirst { it.avt == avatar.pathAvatar }
//                        if (a > -1) {
//                            var a = avatar.pathAvatar.split("/")
//                            var b = a[a.size - 2]
//
//                            startActivity(
//                                Intent(
//                                    applicationContext,
//                                    CustomviewActivity::class.java
//                                ).putExtra(
//                                    "data",
//                                    DataHelper.arrBlackCentered.indexOfFirst { it.avt == avatar.pathAvatar })
//                                    .putExtra(
//                                        "arr",
//                                        toList(avatar.arr)
//                                    ).putExtra("checkEdit", true)
//                                    .putExtra("fileName", File(avatar.path).name)
//                            )
//
//                        } else {
//                            showToast(
//                                applicationContext,
//                                R.string.please_check_your_network_connection
//                            )
//                        }
//
//                    } else {
//                        File(path).delete()
//                        showToast(
//                            applicationContext,
//                            R.string.image_error_please_try_again
//                        )
//                        finish()
//                    }
//                }
//            }else{
                shareListFiles(
                    this@ViewActivity,
                    arrayListOf(path)
                )
//            }

            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Bỏ requesPermission() wrapper, dùng trực tiếp requestCode
        if (requestCode == CONST.REQUEST_STORAGE_PERMISSION) {
            val isGranted = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            val currentCount = sharedPreferenceUtils.getStoragePermission1()
            sharedPreferenceUtils.setStoragePermission1(
                if (isGranted) 0 else currentCount + 1
            )

            if (isGranted) {
                saveFileToExternalStorage(applicationContext, path, "") { check, savedPath ->
                    if (check) {
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.download_successfully) + " " + CONST.NAME_SAVE_FILE,
                            Toast.LENGTH_SHORT
                        ).show()
                        scanMediaFile(this@ViewActivity, File(savedPath))
                    } else {
                        showToast(this@ViewActivity, R.string.download_failed)
                    }
                }
            }
        }
    }
}