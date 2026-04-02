package com.cute.anime.avatarmaker.ui.customview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cute.anime.avatarmaker.data.model.AvatarModel
import com.cute.anime.avatarmaker.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class CustomviewViewModel @Inject constructor(
    var app: Application,
    var roomRepository: RoomRepository
) : AndroidViewModel(app){
    fun addAvatar(data : AvatarModel){
        viewModelScope.launch(Dispatchers.IO) {
            roomRepository.addAvatar(data)
        }
    }
    fun getAvatar(path : String, func: (AvatarModel?)-> Unit){
        viewModelScope.launch(Dispatchers.IO) {
            var a = roomRepository.getAvatar(path)
            withContext(Dispatchers.Main){
                func(a)
            }
        }
    }
    fun deleteAvatar(data : String){
        viewModelScope.launch(Dispatchers.IO) {
            roomRepository.deleteAvatar(data)
        }
    }
}