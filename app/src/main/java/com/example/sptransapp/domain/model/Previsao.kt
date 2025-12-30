package com.example.sptransapp.domain.model

data class Previsao(
    val linha: String,
    val destino: String,
    val horarioChegada: String,
    val prefixoVeiculo: String
) {
    override fun toString(): String {
        return "Linha $linha ($destino)\nChegada: $horarioChegada (Veículo $prefixoVeiculo)"
    }
}