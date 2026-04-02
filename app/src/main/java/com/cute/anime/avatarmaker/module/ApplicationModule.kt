package com.cute.anime.avatarmaker.module

import android.content.Context
import com.cute.anime.avatarmaker.data.callapi.ApiHelper
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.data.repository.RoomRepository
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {
    @Singleton
    @Provides
    fun providerSharedPreference(@ApplicationContext appContext: Context): SharedPreferenceUtils {
        return SharedPreferenceUtils.Companion.getInstance(appContext)
    }
    @Singleton
    @Provides
    fun providerApi(@ApplicationContext context: Context): ApiHelper {
        return ApiHelper(context)
    }
    @Singleton
    @Provides
    fun providerApiRepository(apiHelper: ApiHelper): ApiRepository {
        return ApiRepository(apiHelper)
    }
    @Singleton
    @Provides
    fun providerRepository(@ApplicationContext context: Context): RoomRepository {
        return RoomRepository(context)
    }
}