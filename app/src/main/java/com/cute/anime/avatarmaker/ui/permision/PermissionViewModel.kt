package com.cute.anime.avatarmaker.ui.permision

import androidx.lifecycle.ViewModel
import com.cute.anime.avatarmaker.utils.PermissionHelper
import com.cute.anime.avatarmaker.utils.SharedPreferenceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted.asStateFlow()

    private val _notificationGranted = MutableStateFlow(false)
    val notificationGranted: StateFlow<Boolean> = _notificationGranted.asStateFlow()

    fun updateStorageGranted(sharedPrefs: SharedPreferenceUtils, granted: Boolean) {
        _storageGranted.value = granted
        val currentCount = sharedPrefs.getStoragePermission()
        sharedPrefs.setStoragePermission(if (granted) 0 else currentCount + 1)
    }

    fun updateNotificationGranted(sharedPrefs: SharedPreferenceUtils, granted: Boolean) {
        _notificationGranted.value = granted
        val currentCount = sharedPrefs.getNotificationPermission()
        sharedPrefs.setNotificationPermission(if (granted) 0 else currentCount + 1)
    }

    fun needGoToSettings(sharedPrefs: SharedPreferenceUtils, isStorage: Boolean): Boolean {
        return if (isStorage) {
            sharedPrefs.getStoragePermission() >= 2 && !_storageGranted.value
        } else {
            sharedPrefs.getNotificationPermission() >= 2 && !_notificationGranted.value
        }
    }

    fun getStoragePermissions(): Array<String> = PermissionHelper.storagePermission
    fun getNotificationPermissions(): Array<String> = PermissionHelper.notificationPermission
}