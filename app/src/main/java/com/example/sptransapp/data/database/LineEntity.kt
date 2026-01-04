package com.example.sptransapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sptransapp.domain.model.Line

@Entity(tableName = "favorite_lines")
data class LineEntity(
    @PrimaryKey val lineCode: Int,
    val fullSign: String,
    val name: String,
    val direction: Int,
) {
    fun toDomainModel(): Line =
        Line(
            lineCode = lineCode,
            fullSign = fullSign,
            name = name,
            direction = direction,
        )
}

fun Line.toEntity(): LineEntity =
    LineEntity(
        lineCode = this.lineCode,
        fullSign = this.fullSign,
        name = this.name,
        direction = this.direction,
    )
