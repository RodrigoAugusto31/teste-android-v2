package com.example.sptransapp.domain.model

data class Onibus(
    val prefixo: String,
    val latitude: Double,
    val longitude: Double,
    val letreiro: String,
    val sentido: String,
    val isAcessivel: Boolean,
)
