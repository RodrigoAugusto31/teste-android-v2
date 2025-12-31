package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class CorredorDto(
    @SerializedName("cc") val codigo: Int?,
    @SerializedName("nc") val nome: String?
)