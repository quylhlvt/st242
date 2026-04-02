package com.cute.anime.avatarmaker.custom.listener.listenerdraw

import android.view.MotionEvent
import com.cute.anime.avatarmaker.custom.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}