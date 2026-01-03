package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class CorridorDto(
    @SerializedName("cc") val code: Int?,
    @SerializedName("nc") val name: String?,
)
