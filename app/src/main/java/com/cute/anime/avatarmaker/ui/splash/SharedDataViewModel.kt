package com.cute.anime.avatarmaker.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cute.anime.avatarmaker.data.callapi.reponse.LoadingStatus
import com.cute.anime.avatarmaker.data.repository.ApiRepository
import com.cute.anime.avatarmaker.utils.DataHelper
import com.cute.anime.avatarmaker.utils.DataHelper.getData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedDataViewModel @Inject constructor(
    application: Application,
    private val apiRepository: ApiRepository
) : AndroidViewModel(application) {

    private val _dataLoadingState = MutableLiveData<DataLoadingState>()
    val dataLoadingState: LiveData<DataLoadingState> = _dataLoadingState

    private var isDataLoading = false
    private var isDataLoaded = false

    init {
        loadData()
    }

    fun loadData() {
        // Tránh load nhiều lần đồng thời
        if (isDataLoading || isDataLoaded) return

        isDataLoading = true
        _dataLoadingState.postValue(DataLoadingState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Gọi getData từ DataHelper
                getApplication<Application>().getData(apiRepository)

                // Observe DataHelper.arrDataOnline để biết khi nào xong
                observeDataHelperResponse()
            } catch (e: Exception) {
                isDataLoading = false
                _dataLoadingState.postValue(DataLoadingState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private fun observeDataHelperResponse() {
        DataHelper.arrDataOnline.observeForever { response ->
            response?.let {
                when (it.loadingStatus) {
                    LoadingStatus.Loading -> {
                        _dataLoadingState.postValue(DataLoadingState.Loading)
                    }

                    LoadingStatus.Success -> {
                        // Kiểm tra xem data đã được process chưa
                        if (DataHelper.arrBlackCentered.isNotEmpty()) {
                            isDataLoading = false
                            isDataLoaded = true
                            _dataLoadingState.postValue(DataLoadingState.Success)

                            // Remove observer sau khi xong
                            DataHelper.arrDataOnline.removeObserver { }
                        }
                    }

                    LoadingStatus.Error -> {
                        isDataLoading = false
                        _dataLoadingState.postValue(DataLoadingState.Error("Failed to load data"))
                    }

                    else -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    fun isDataReady(): Boolean {
        return isDataLoaded && DataHelper.arrBlackCentered.isNotEmpty()
    }

    fun forceReload() {
        isDataLoaded = false
        isDataLoading = false
        loadData()
    }

    sealed class DataLoadingState {
        object Loading : DataLoadingState()
        object Success : DataLoadingState()
        data class Error(val message: String) : DataLoadingState()
    }


}