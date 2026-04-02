package com.cute.anime.avatarmaker.ui.succes

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.ui.main.MainActivity
import com.cute.anime.avatarmaker.ui.my_creation.MyCreationActivity
import com.cute.anime.avatarmaker.ui.permision.PermissionViewModel
import com.cute.anime.avatarmaker.utils.CONST
import com.cute.anime.avatarmaker.utils.PermissionHelper
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import com.cute.anime.avatarmaker.utils.newIntent
import com.cute.anime.avatarmaker.utils.onClick
import com.cute.anime.avatarmaker.utils.onSingleClick
import com.cute.anime.avatarmaker.utils.saveFileToExternalStorage
import com.cute.anime.avatarmaker.utils.scanMediaFile
import com.cute.anime.avatarmaker.utils.showDialogNotifiListener
import com.cute.anime.avatarmaker.utils.showToast
import com.bumptech.glide.Glide
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.databinding.ActivitySuccessBinding
import com.cute.anime.avatarmaker.utils.shareListFiles
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlin.getValue
@AndroidEntryPoint
class SuccessActivity : AbsBaseActivity<ActivitySuccessBinding>() {
    var path = ""
    override fun getLayoutId(): Int = R.layout.activity_success
    private val permissionViewModel: PermissionViewModel by viewModels()
    @Inject
    lateinit var sharedPreference: SharedPreferenceUtils
    override fun initView() {

        path = intent.getStringExtra("path").toString()
        Glide.with(applicationContext).load(path).into(binding.imv)
        binding.apply {
            tvDownload.isSelected = true
            tvMyWork.isSelected = true
            tvTitle.isSelected = true
            binding.imvBack.isSelected = true

//            tvContent.isSelected = true
        }
      }

    override fun initAction() {
        binding.apply {
            imvBack.onSingleClick { finish() }
            imvShare.onClick {
                shareListFiles(
                    this@SuccessActivity,
                    arrayListOf(path)
                )
            }

            imvHome.onSingleClick {
                    startActivity(
                        newIntent(
                            applicationContext,
                            MainActivity::class.java
                        )
                    )
                    finish()
                }

            btnMyWork.onSingleClick {
                    startActivity(
                        newIntent(
                            applicationContext,
                            MyCreationActivity::class.java
                        )
                    )
                    finish()
                }

            btnDownload.onClick {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    performDownload()
                } else {
                    handlePermissionRequest(isStorage = true)}

            }
        }
    }
    private fun handlePermissionRequest(isStorage: Boolean) {
        val permissions = if (isStorage) {
            permissionViewModel.getStoragePermissions()
        } else {
            permissionViewModel.getNotificationPermissions()
        }

        // Kiểm tra đã có permission chưa
        if (PermissionHelper.checkPermissions(permissions, this@SuccessActivity)) {
            performDownload()
            return
        }

        // Kiểm tra nếu đã từ chối nhiều lần → gợi ý vào Settings
        if (permissionViewModel.needGoToSettings(sharedPreference, isStorage)) {
            val dialogRes = if (isStorage) R.string.reques_storage else R.string.content_dialog_notification
            showDialogNotifiListener(dialogRes)
            return
        }

        // Request permission bình thường
        val requestCode = if (isStorage) CONST.REQUEST_STORAGE_PERMISSION else CONST.REQUEST_NOTIFICATION_PERMISSION
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    private fun performDownload() {
        saveFileToExternalStorage(
            applicationContext,
            path,
            ""
        ) { check, path ->
            if (check) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.download_successfully) + " " + CONST.NAME_SAVE_FILE,
                    Toast.LENGTH_SHORT
                ).show()
                scanMediaFile(this@SuccessActivity, File(path))
            } else {
                showToast(this@SuccessActivity, R.string.download_failed)
            }
        }
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

}