package com.example.sptransapp.data.database.entity

import androidx.room.Entity
import com.example.sptransapp.domain.model.Stop

@Entity(tableName = "stops", primaryKeys = ["stopCode", "lineRelationCode"])
data class StopEntity(
    val stopCode: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lineRelationCode: Int,
) {
    fun toDomain() = Stop(stopCode, name, latitude, longitude)
}

fun Stop.toEntity(lineCode: Int) =
    StopEntity(
        stopCode = this.stopCode,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        lineRelationCode = lineCode,
    )
