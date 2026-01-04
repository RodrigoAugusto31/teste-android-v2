package com.example.sptransapp.domain.repository

import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Corridor
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.model.Stop
import kotlinx.coroutines.flow.Flow

interface BusRepository {
    suspend fun getPositions(): List<Bus>

    suspend fun searchLines(term: String): List<Line>

    suspend fun getPositionsByLine(lineCode: Int): List<Bus>

    fun getStopsByLine(lineCode: Int): Flow<List<Stop>>

    suspend fun getStopPredictions(lineCode: Int): List<Prediction>

    fun getCorridors(): Flow<List<Corridor>>

    suspend fun getCorridorsKml(): java.io.InputStream?

    suspend fun getGeneralKml(): java.io.InputStream?

    suspend fun getOtherLanesKml(): java.io.InputStream?

    fun getFavoriteLines(): Flow<List<Line>>

    fun isFavorite(lineCode: Int): Flow<Boolean>

    suspend fun toggleFavorite(line: Line)
}
