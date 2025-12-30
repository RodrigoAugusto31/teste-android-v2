package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class LinhaBuscaDto(
    @SerializedName("cl") val codigoLinha: Int,
    @SerializedName("lc") val circular: Boolean,
    @SerializedName("lt") val letreiroPrimeiro: String,
    @SerializedName("tl") val letreiroSegundo: Int,
    @SerializedName("sl") val sentido: Int,
    @SerializedName("tp") val letreiroPrincipal: String,
    @SerializedName("ts") val letreiroSecundario: String
)