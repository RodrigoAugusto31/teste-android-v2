package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    @SerializedName("hr") val referenceTime: String?,
    @SerializedName("p") val stop: StopPredictionDto?,
)
