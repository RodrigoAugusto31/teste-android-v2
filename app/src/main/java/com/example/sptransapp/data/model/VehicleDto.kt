package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class VehicleDto(
    @SerializedName("p") val prefix: String,
    @SerializedName("a") val isAccessible: Boolean,
    @SerializedName("ta") val timestamp: String,
    @SerializedName("py") val latitude: Double,
    @SerializedName("px") val longitude: Double,
)
