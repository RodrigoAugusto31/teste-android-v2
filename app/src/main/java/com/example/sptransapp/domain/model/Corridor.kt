package com.example.sptransapp.domain.model

data class Corridor(
    val code: Int,
    val name: String,
) {
    override fun toString(): String = name
}
