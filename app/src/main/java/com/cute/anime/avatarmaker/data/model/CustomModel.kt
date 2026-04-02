package com.cute.anime.avatarmaker.data.model

data class CustomModel(
    var avt: String,
    var bodyPart: ArrayList<BodyPartModel>,
    var checkDataOnline : Boolean = false
)

data class BodyPartModel(
    var icon: String,
    var listPath: ArrayList<ColorModel>,
    var listThumbPath: ArrayList<String> = arrayListOf(),
    var listSinglePath: ArrayList<String> = arrayListOf()
)

data class ColorModel(
    var color: String,
    var listPath: ArrayList<String>
)