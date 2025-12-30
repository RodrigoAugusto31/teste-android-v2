package com.example.sptransapp.domain.model

data class Linha(
    val codigoLinha: Int,
    val letreiroCompleto: String,
    val nome: String,
    val sentido: Int
) {
    override fun toString(): String = "$letreiroCompleto - $nome"
}