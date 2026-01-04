package com.example.sptransapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sptransapp.domain.model.Corridor

@Entity(tableName = "corridors")
data class CorridorEntity(
    @PrimaryKey val code: Int,
    val name: String
) {
    fun toDomain() = Corridor(code, name)
}

fun Corridor.toEntity() = CorridorEntity(code, name)
