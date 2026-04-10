package com.anime.couple.couplemaker.data.repository

import android.util.Log
import com.anime.couple.couplemaker.data.callapi.ApiHelper
import com.anime.couple.couplemaker.data.model.CharacterResponse
import com.anime.couple.couplemaker.utils.CONST
import com.anime.couple.couplemaker.utils.DataHelper
import javax.inject.Inject

class ApiRepository @Inject constructor(private val apiHelper: ApiHelper) {
    suspend fun getFigure(): CharacterResponse? {
        try {
            CONST.BASE_URL = CONST.BASE_URL_1
            return apiHelper.apiMermaid1.getAllData()
//            return null
        } catch (e: Exception) {
            Log.d(DataHelper.TAG, "getFigure: $e")
            try {
                CONST.BASE_URL = CONST.BASE_URL_2
//                return null
                return apiHelper.apiMermaid2.getAllData()
            } catch (e: Exception) {
                Log.d(DataHelper.TAG, "getFigure: $e")
                return null
            }
        }
    }
}