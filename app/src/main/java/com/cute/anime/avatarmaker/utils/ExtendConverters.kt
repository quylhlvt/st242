package com.cute.anime.avatarmaker.utils

import android.graphics.RectF
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.roundToInt


@TypeConverter
    fun fromList(value: ArrayList<ArrayList<Int>>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toList(value: String): ArrayList<ArrayList<Int>> {
        val type = object : TypeToken<ArrayList<ArrayList<Int>>>() {}.type
        return Gson().fromJson(value, type)
    }

fun toRect(r: RectF, array: FloatArray) {
    r[Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY] =
        Float.NEGATIVE_INFINITY
    var i = 1
    while (i < array.size) {
        val x = (array[i - 1] * 10).roundToInt() / 10f
        val y = (array[i] * 10).roundToInt() / 10f
        r.left = if (x < r.left) x else r.left
        r.top = if (y < r.top) y else r.top
        r.right = if (x > r.right) x else r.right
        r.bottom = if (y > r.bottom) y else r.bottom
        i += 2
    }
    r.sort()
}
