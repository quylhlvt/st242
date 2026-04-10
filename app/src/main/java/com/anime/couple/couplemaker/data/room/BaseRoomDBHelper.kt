package com.anime.couple.couplemaker.data.room

import android.content.Context
import androidx.room.Room
import com.anime.couple.couplemaker.utils.SingletonHolder


open class BaseRoomDBHelper(context: Context) {
    val db = Room.databaseBuilder(context, AppDB::class.java,"Avatar").build()
    companion object : SingletonHolder<BaseRoomDBHelper, Context>(::BaseRoomDBHelper)
}