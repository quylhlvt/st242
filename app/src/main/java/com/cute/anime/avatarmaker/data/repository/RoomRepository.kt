package com.cute.anime.avatarmaker.data.repository

import android.content.Context
import android.util.Log
import com.cute.anime.avatarmaker.data.model.AvatarModel
import com.cute.anime.avatarmaker.data.room.BaseRoomDBHelper

class RoomRepository(context: Context) : BaseRoomDBHelper(context) {


    fun addAvatar(theme: AvatarModel): Long {
        return try {
            db.dbDao().addAvatar(theme)
        } catch (e: Exception) {
            Log.d("TAG", "exception_of_app setCallPhone from roomDB: ${e} ")
            -1
        }
    }

    fun deleteAvatar(theme: String): Int {
        return try {
            db.dbDao().deleteTheme(theme)
        } catch (e: Exception) {
            Log.d("TAG", "exception_of_app setCallPhone from roomDB: ${e} ")
            -1
        }
    }

    fun getAvatar(path: String): AvatarModel? {
        return try {
            db.dbDao().getTheme(path)
        } catch (e: Exception) {
            Log.d("TAG", "exception_of_app setCallPhone from roomDB: ${e} ")
            null
        }
    }
}