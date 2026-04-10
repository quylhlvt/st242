package com.anime.couple.couplemaker.ui.permision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.anime.couple.couplemaker.base.AbsBaseActivity
import com.anime.couple.couplemaker.ui.main.MainActivity
import com.anime.couple.couplemaker.utils.CONST
import com.anime.couple.couplemaker.utils.SharedPreferenceUtils
import com.anime.couple.couplemaker.utils.changeText
import com.anime.couple.couplemaker.utils.checkPermision
import com.anime.couple.couplemaker.utils.music.MusicLocal
import com.anime.couple.couplemaker.utils.onSingleClick
import com.anime.couple.couplemaker.utils.showDialogNotifiListener
import com.anime.couple.couplemaker.utils.showToast
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ActivityPermissionBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : AbsBaseActivity<ActivityPermissionBinding>() {

    private val viewModel: PermissionViewModel by viewModels()

    @Inject
    lateinit var sharedPreferenceUtils: SharedPreferenceUtils

    override fun getLayoutId(): Int = R.layout.activity_permission

    override fun initView() {

        MusicLocal.isInSplashOrTutorial = true

//        binding.btnContinue.gradientVertical(
//            "#01579B".toColorInt(),
//            "#01579B".toColorInt()
//        )
//        binding.actionBar.txtCustom.text = getString(R.string.permission)
//        binding.actionBar.txtCustom.isSelected = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.rl4.visibility = View.VISIBLE
            binding.rl2.visibility = View.GONE
        } else {
            binding.rl4.visibility = View.GONE
            binding.rl2.visibility = View.VISIBLE
        }

        val space = " "
        binding.tvTitle.text = TextUtils.concat(
            changeText(
                this,
                getString(R.string.allow),
                R.color.app_color2,
                R.font.fredoka_one_regular
            ),
            space,
            changeText(
                this,
                getString(R.string.app_name),
                R.color.app_color2,
                R.font.fredoka_one_regular
            ),
            space,
            changeText(
                this,
                getString(R.string.request_permission_to_use_notifications_to_notify_you),
                R.color.app_color2,
                R.font.fredoka_one_regular
            )
        )

        checkPer()
    }

    override fun initAction() {
        binding.btnContinue.onSingleClick {

                    sharedPreferenceUtils.putBooleanValue(CONST.PERMISON, true)
                    val intent = Intent(this@PermissionActivity, MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()

        }

        binding.swiVibrate2.onSingleClick {

            handlePermissionRequest(isStorage = true)
        }

        binding.swiVibrate4.onSingleClick {
            handlePermissionRequest(isStorage = false)
        }
    }

    private fun handlePermissionRequest(isStorage: Boolean) {
        val permissions = if (isStorage) {
            viewModel.getStoragePermissions()
        } else {
            viewModel.getNotificationPermissions()
        }

        if (checkPermissions(permissions)) {
            showToast(this, R.string.permission_granted)
            return
        }

        // Kiểm tra nếu đã từ chối nhiều lần và không còn show rationale → gợi ý vào Settings
        if (viewModel.needGoToSettings(sharedPreferenceUtils, isStorage)) {
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

    private fun checkPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val isGranted =
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            CONST.REQUEST_STORAGE_PERMISSION -> {
                viewModel.updateStorageGranted(sharedPreferenceUtils, isGranted)

                if (isGranted) {
                    binding.swiVibrate2.setImageResource(R.drawable.switch_on)
                    showToast(this, R.string.permission_granted)
                }
                // Không cần xử lý riêng "Don't ask again" ở đây vì đã kiểm tra trong handlePermissionRequest
            }

            CONST.REQUEST_NOTIFICATION_PERMISSION -> {
                viewModel.updateNotificationGranted(sharedPreferenceUtils, isGranted)

                if (isGranted) {
                    binding.swiVibrate4.setImageResource(R.drawable.switch_on)
                    showToast(this, R.string.permission_granted)
                }
            }
        }

        checkPer() // Cập nhật lại trạng thái switch
    }

    override fun onResume() {
        super.onResume()
        checkPer()
    }

    private fun checkPer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notiGranted = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            binding.swiVibrate4.setImageResource(if (notiGranted) R.drawable.switch_on else R.drawable.switch_off)
        } else {
            // Giả sử checkPermision() kiểm tra đúng quyền storage cũ (WRITE_EXTERNAL_STORAGE hoặc READ_EXTERNAL_STORAGE)
            val storageGranted = checkPermision(this)
            binding.swiVibrate2.setImageResource(if (storageGranted) R.drawable.switch_on else R.drawable.switch_off)
        }
    }
}