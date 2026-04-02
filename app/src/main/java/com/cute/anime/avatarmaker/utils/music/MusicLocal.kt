package com.cute.anime.avatarmaker.utils.music

import android.content.Context
import android.media.MediaPlayer
import androidx.core.content.edit
import com.cute.anime.avatarmaker.R

object MusicLocal {
    private const val PREF_NAME = "music_preferences"
    private const val KEY_MUSIC_STATUS = "music_status"

    private var music: MediaPlayer? = null
    var isInSplashOrTutorial = false
    var home = false

    // 1. Load trạng thái từ SharedPreferences
    private fun loadStatus(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MUSIC_STATUS, true) // ĐỔI THÀNH true - mặc định BẬT khi cài mới
    }

    // 2. Lưu trạng thái
    private fun saveStatus(context: Context, status: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_MUSIC_STATUS, status) }
    }

    // 3. Chạy nhạc
    fun play(context: Context) {
        val savedStatus = loadStatus(context)

        // Chỉ phát nếu user đã bật và đã vào MainActivity
        if (!savedStatus || !home) return

        if (music == null) {
            try {
                music = MediaPlayer.create(context.applicationContext, R.raw.theme)?.apply {
                    isLooping = true
                }
            } catch (e: Exception) {
                music = null
            }
        }

        try {
            if (music?.isPlaying == false) {
                music?.start()
            }
        } catch (e: Exception) {
        }
    }

    // 4. Tạm dừng nhạc
    fun pause() {
        try {
            if (music?.isPlaying == true) {
                music?.pause()
            }
        } catch (e: Exception) {
        }
    }

    // 5. Bật/tắt nhạc + lưu trạng thái
    fun toggle(context: Context, enable: Boolean) {
        saveStatus(context, enable) // lưu ngay

        if (enable) play(context) else pause()
    }

    // Check trạng thái hiện tại
    fun status(context: Context): Boolean {
        return loadStatus(context)
    }

    // Giải phóng khi cần
    fun release() {
        try {
            music?.apply {
                if (isPlaying) stop()
                release()
            }
            music = null
        } catch (e: Exception) {
        }
    }
}