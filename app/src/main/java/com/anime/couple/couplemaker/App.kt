package com.anime.couple.couplemaker

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.anime.couple.couplemaker.utils.music.MusicLocal
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
@HiltAndroidApp
class App : Application()  {
    companion object {
        lateinit var instance:App
            private set
        val context: Context
            get() = instance.applicationContext
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // App vào foreground → mở lại nhạc
                if (!MusicLocal.isInSplashOrTutorial && MusicLocal.home)
                    MusicLocal.play(context)

            }
            override fun onStop(owner: LifecycleOwner) {
                // App ra background → tạm dừng nhạc
                MusicLocal.pause()
            }
        })
    }

}