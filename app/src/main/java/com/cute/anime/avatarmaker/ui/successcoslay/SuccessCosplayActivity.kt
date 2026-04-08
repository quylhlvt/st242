package com.cute.anime.avatarmaker.ui.successcoslay

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cute.anime.avatarmaker.R
import com.cute.anime.avatarmaker.base.AbsBaseActivity
import com.cute.anime.avatarmaker.databinding.ActivitySuccessBinding
import com.cute.anime.avatarmaker.databinding.ActivitySuccessCosplayBinding
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
import com.cute.anime.avatarmaker.utils.shareListFiles
import com.cute.anime.avatarmaker.utils.showDialogNotifiListener
import com.cute.anime.avatarmaker.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlin.getValue


@AndroidEntryPoint
class SuccessCosplayActivity : AbsBaseActivity<ActivitySuccessCosplayBinding>() {
    var path1 = ""
    var path2 = ""
    override fun getLayoutId(): Int = R.layout.activity_success_cosplay

    override fun initView() {
        val matchPercent = intent.getIntExtra("matchPercent", 0)
        path1 = intent.getStringExtra("cosplayBitmapPath").toString()
        path2 = intent.getStringExtra("currentBitmapPath").toString()

        Glide.with(applicationContext).load(path1).into(binding.imgCharacter1)
        Glide.with(applicationContext).load(path2).into(binding.imgCharacter2)

        binding.tvMatchPercent.text = "$matchPercent/100"

        // Luôn hiện 5 sao, fill theo phần trăm
        val starCount = when (matchPercent) {
            0 -> 0
            in 1..20 -> 1
            in 21..40 -> 2
            in 41..70 -> 3
            in 71..90 -> 4
            in 91..100 -> 5
            else -> 0
        }
        binding.ll1.rating = starCount.toFloat()

        binding.progressTrack.post {
            val trackW = binding.progressTrack.width.toFloat()
            val fillMarginStartPx = 18 * resources.displayMetrics.density
            val fillW = trackW - fillMarginStartPx
            val targetScale = matchPercent / 100f
            val adjustedScale = targetScale * fillW / trackW

            binding.progressFill.pivotX = 0f
            binding.progressFill.pivotY = binding.progressFill.height / 2f
            binding.progressFill.scaleX = adjustedScale
            binding.progressFill.scaleY = 1f

            val starW = binding.imgStar.width.toFloat()
            binding.imgStar.translationX = fillMarginStartPx + fillW * targetScale - starW / 2f
        }

        binding.tvTitle.isSelected = true
        binding.imvBack.isSelected = true
        binding.tv2.isSelected = true
    }
    override fun onBackPressed() {
    }
    override fun initAction() {
        binding.apply {
            btnTryAgain.onSingleClick {
                setResult(RESULT_OK)
                finish() }
            imvHome.onSingleClick {
                startActivity(newIntent(applicationContext, MainActivity::class.java))
                finish()
            }
        }
    }




}