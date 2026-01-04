package com.example.sptransapp.domain.model

data class Prediction(
    val line: String,
    val destination: String,
    val arrivalTime: String,
    val vehiclePrefix: String,
) {
    override fun toString(): String = "Line $line ($destination)\nChegada: $arrivalTime (Veículo $vehiclePrefix)"
}
