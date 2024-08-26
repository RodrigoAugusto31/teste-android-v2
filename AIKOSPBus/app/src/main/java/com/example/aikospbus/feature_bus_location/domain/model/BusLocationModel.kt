package com.example.aikospbus.feature_bus_location.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aikospbus.feature_bus_location.data.remote.dto.VehicleDto

@Entity(tableName = "busLocation")
data class BusLocationModel(
    @PrimaryKey(autoGenerate = false)
    val horaConsulta: String,            // Horário da consulta da posição
    val vehicleDtos: List<VehicleDto>          // Lista de veículos na linha
)
