package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class LinePredictionDto(
    @SerializedName("c") val fullSign: String?,
    @SerializedName("lt0") val destination: String?,
    @SerializedName("vs") val vehicles: List<VehiclePredictionDto>?,
)
