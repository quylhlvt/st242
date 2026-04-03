package com.cute.anime.avatarmaker.data.callapi

import com.cute.anime.avatarmaker.data.model.CharacterResponse
import retrofit2.http.GET

interface ApiMermaid {
    @GET("api/app/st225_couplemakerkisscreator")
    suspend fun getAllData(): CharacterResponse
}