package com.cute.anime.avatarmaker.data.callapi

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

open class BaseRetrofitHelper() {
    var okHttpClient: OkHttpClient? = null
    init {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val builder = OkHttpClient.Builder().writeTimeout(6 * 1000, TimeUnit.MILLISECONDS).readTimeout(6 * 1000, TimeUnit.MILLISECONDS).addInterceptor(interceptor)
        okHttpClient = builder.build()
    }
}