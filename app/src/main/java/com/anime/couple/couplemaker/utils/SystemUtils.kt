package com.anime.couple.couplemaker.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import java.io.File
import java.util.Locale

object SystemUtils {
    var defaultFontScale: Float = 1f
    private var myLocale: Locale? = null
    fun saveLocale(context: Context, lang: String?) {
        setPreLanguage(context, lang)
    }
    fun TextView.gradientVertical(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int
    ) {
        post {
            paint.shader = LinearGradient(
                0f, 0f,
                0f, height.toFloat(),
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
            invalidate()
        }
    }
    @SuppressLint("CheckResult")
    fun ImageView.loadImageFromFile(path: String) {
        val file = File(path)
        val request = Glide.with(context)
            .load(file)

        request.signature(ObjectKey(file.lastModified()))

        request.into(this)
    }
    fun TextView.gradientHorizontal(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int
    ) {
        post {
            // Đảm bảo TextView đã layou
            // t xong (height/width > 0)
            if (width <= 0 || height <= 0) return@post

            paint.shader = LinearGradient(
                0f,          // startX: bắt đầu từ trái
                0f,          // startY
                width.toFloat(),  // endX: kết thúc ở bên phải
                0f,          // endY: giữ nguyên chiều cao (không dọc)
                intArrayOf(startColor, endColor),  // Mảng màu (từ start → end)
                null,                              // positions (null = phân bố đều)
                Shader.TileMode.CLAMP
            )
            invalidate()
        }
    }

    fun setLocale(context: Context) {
        val language = getPreLanguage(context)
        if (language == "") {
            val config = Configuration()
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            config.locale = locale
            context.resources
                .updateConfiguration(config, context.resources.displayMetrics)
        } else {
            changeLang(language, context)
        }
    }

    fun changeLang(lang: String?, context: Context) {
        if (lang.equals("", ignoreCase = true)) {
            return
        }
        myLocale = Locale(lang)
        saveLocale(context, lang)
        Locale.setDefault(myLocale)
        val config = Configuration()
        config.locale = myLocale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun getPreLanguage(mContext: Context): String? {
        val preferences = mContext.getSharedPreferences("data2", Context.MODE_PRIVATE)
        return preferences.getString("KEY_LANGUAGE_2", "en")
    }

    fun setPreLanguage(context: Context, language: String?) {
        if (language == null || language == "") {
        } else {
            val preferences = context.getSharedPreferences("data2", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("KEY_LANGUAGE_2", language)
            editor.apply()
        }
    }
}