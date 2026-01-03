package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class LineSearchDto(
    @SerializedName("cl") val lineCode: Int,
    @SerializedName("lc") val isCircular: Boolean,
    @SerializedName("lt") val firstSign: String,
    @SerializedName("tl") val secondSign: Int,
    @SerializedName("sl") val direction: Int,
    @SerializedName("tp") val mainSign: String,
    @SerializedName("ts") val secondarySign: String,
)
