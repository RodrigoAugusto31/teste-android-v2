package com.example.sptransapp.domain.model

data class Line(
    val lineCode: Int,
    val fullSign: String,
    val name: String,
    val direction: Int,
) {
    override fun toString(): String = "$fullSign - $name"
}
