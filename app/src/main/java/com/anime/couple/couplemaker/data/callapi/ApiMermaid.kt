package com.anime.couple.couplemaker.data.callapi

import com.anime.couple.couplemaker.data.model.CharacterResponse
import retrofit2.http.GET

interface ApiMermaid {
    @GET("api/app/st225_couplemakerkisscreator")
    suspend fun getAllData(): CharacterResponse
}