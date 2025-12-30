package com.example.sptransapp.data.model

import com.google.gson.annotations.SerializedName

data class PosicaoResponse(
    @SerializedName("hr") val horaReferencia: String?,
    @SerializedName("l") val linhas: List<LinhaDto>,
    @SerializedName("vs") val veiculos: List<VeiculoDto>?
)

data class LinhaDto(
    @SerializedName("c") val letreiroCompleto: String,
    @SerializedName("cl") val codigoLinha: Int,
    @SerializedName("sl") val sentido: Int,
    @SerializedName("lt0") val destino: String,
    @SerializedName("lt1") val origem: String,
    @SerializedName("qv") val quantidadeVeiculos: Int,
    @SerializedName("vs") val veiculos: List<VeiculoDto>
)

data class VeiculoDto(
    @SerializedName("p") val prefixo: String,
    @SerializedName("a") val acessivel: Boolean,
    @SerializedName("ta") val dataHoraCaptura: String,
    @SerializedName("py") val latitude: Double,
    @SerializedName("px") val longitude: Double
)