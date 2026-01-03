package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class VehiclePredictionDto(
    @SerializedName("p") val prefix: String?,
    @SerializedName("t") val arrivalTime: String?,
)
