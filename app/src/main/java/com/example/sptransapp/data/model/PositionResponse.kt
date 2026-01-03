package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class PositionResponse(
    @SerializedName("hr") val referenceTime: String?,
    @SerializedName("l") val lines: List<LineDto>,
    @SerializedName("vs") val vehicles: List<VehicleDto>?,
)
