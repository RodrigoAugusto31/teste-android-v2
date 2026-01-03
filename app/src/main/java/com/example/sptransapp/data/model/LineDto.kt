package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class LineDto(
    @SerializedName("c") val fullSign: String,
    @SerializedName("cl") val lineCode: Int,
    @SerializedName("sl") val direction: Int,
    @SerializedName("lt0") val destination: String,
    @SerializedName("lt1") val origin: String,
    @SerializedName("qv") val vehicleCount: Int,
    @SerializedName("vs") val vehicles: List<VehicleDto>,
)
