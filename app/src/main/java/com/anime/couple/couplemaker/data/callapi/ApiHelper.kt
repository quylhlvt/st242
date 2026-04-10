package com.anime.couple.couplemaker.data.callapi

import android.annotation.SuppressLint
import android.content.Context
import com.anime.couple.couplemaker.utils.CONST
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


@SuppressLint("CheckResult")
class ApiHelper(context: Context) : BaseRetrofitHelper() {
    var apiMermaid1: ApiMermaid
    var apiMermaid2: ApiMermaid

    init {
        GsonBuilder().setLenient().create()
        val retrofit1 = Retrofit.Builder().baseUrl(CONST.BASE_URL_1)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory()).client(okHttpClient!!).build()
        apiMermaid1 = retrofit1.create(ApiMermaid::class.java)

        val retrofit2 = Retrofit.Builder().baseUrl(CONST.BASE_URL_2)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory()).client(okHttpClient!!).build()
        apiMermaid2 = retrofit2.create(ApiMermaid::class.java)
    }
}