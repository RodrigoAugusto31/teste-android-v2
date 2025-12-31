package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class PrevisaoResponse(
    @SerializedName("hr") val horaReferencia: String?,
    @SerializedName("p") val parada: ParadaPrevisaoDto?,
)

data class ParadaPrevisaoDto(
    @SerializedName("cp") val codigoParada: Int?,
    @SerializedName("np") val nomeParada: String?,
    @SerializedName("l") val linhas: List<LinhaPrevisaoDto>?,
)

data class LinhaPrevisaoDto(
    @SerializedName("c") val letreiro: String?,
    @SerializedName("lt") val destino: String?,
    @SerializedName("vs") val veiculos: List<VeiculoPrevisaoDto>?,
)

data class VeiculoPrevisaoDto(
    @SerializedName("p") val prefixo: String?,
    @SerializedName("t") val previsaoChegada: String?,
)
