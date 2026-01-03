package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class StopPredictionDto(
    @SerializedName("cp") val stopCode: Int?,
    @SerializedName("np") val stopName: String?,
    @SerializedName("l") val lines: List<LinePredictionDto>?,
)
