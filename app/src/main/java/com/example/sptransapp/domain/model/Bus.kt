package com.example.sptransapp.domain.model

data class Bus(
    val prefix: String,
    val latitude: Double,
    val longitude: Double,
    val fullSign: String,
    val destination: String,
    val isAccessible: Boolean,
)
