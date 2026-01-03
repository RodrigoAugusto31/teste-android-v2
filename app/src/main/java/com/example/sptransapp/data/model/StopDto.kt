package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class StopDto(
    @SerializedName("cp") val stopCode: Int,
    @SerializedName("np") val stopName: String,
    @SerializedName("ed") val address: String?,
    @SerializedName("py") val latitude: Double,
    @SerializedName("px") val longitude: Double,
)
