package com.cute.anime.avatarmaker.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cute.anime.avatarmaker.data.model.AvatarModel
import javax.inject.Singleton

@Singleton
@Database(entities = [AvatarModel::class], version = 1, exportSchema = false)
abstract class AppDB: RoomDatabase() {
    abstract fun dbDao(): Dao
}