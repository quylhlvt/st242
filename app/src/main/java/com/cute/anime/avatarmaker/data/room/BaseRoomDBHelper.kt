package com.cute.anime.avatarmaker.data.room

import android.content.Context
import androidx.room.Room
import com.cute.anime.avatarmaker.utils.SingletonHolder


open class BaseRoomDBHelper(context: Context) {
    val db = Room.databaseBuilder(context, AppDB::class.java,"Avatar").build()
    companion object : SingletonHolder<BaseRoomDBHelper, Context>(::BaseRoomDBHelper)
}