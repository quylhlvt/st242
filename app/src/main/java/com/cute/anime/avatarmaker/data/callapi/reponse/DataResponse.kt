package com.cute.anime.avatarmaker.data.callapi.reponse

sealed class DataResponse<T> constructor(val loadingStatus: LoadingStatus) {
    class DataLoading<T>(private val loadingType : LoadingStatus) : DataResponse<T>(loadingType)
    data class DataSuccess<T>(val body: T) : DataResponse<T>(LoadingStatus.Success)
    class DataIdle<T> : DataResponse<T>(LoadingStatus.Idle)
    class DataError<T> : DataResponse<T>(LoadingStatus.Error)
    class DataLoadingProgress<T>(val progress: Int) : DataResponse<T>(LoadingStatus.Loading)
    class DataErrorResponse<T>(val throwable: Exception?) : DataResponse<T>(LoadingStatus.Error)
}