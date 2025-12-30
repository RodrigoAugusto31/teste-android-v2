package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class ParadaDto(
    @SerializedName("cp") val codigoParada: Int,
    @SerializedName("np") val nomeParada: String,
    @SerializedName("ed") val endereco: String?,
    @SerializedName("py") val latitude: Double,
    @SerializedName("px") val longitude: Double
)