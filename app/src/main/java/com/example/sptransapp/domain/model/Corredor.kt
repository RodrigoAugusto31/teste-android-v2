package com.example.sptransapp.domain.model

data class Corredor(
    val codigo: Int,
    val nome: String
) {
    override fun toString(): String = nome
}