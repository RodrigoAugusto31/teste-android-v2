package com.example.sptransapp.domain.repository

import com.example.sptransapp.domain.model.Corridor
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.model.Prediction

interface BusRepository {
    suspend fun getPositions(): List<Bus>

    suspend fun searchLines(term: String): List<Line>

    suspend fun getPositionsByLine(lineCode: Int): List<Bus>

    suspend fun getStopsByLine(lineCode: Int): List<Stop>

    suspend fun getStopPredictions(lineCode: Int): List<Prediction>

    suspend fun getCorridors(): List<Corridor>

    suspend fun getCorridorsKml(): java.io.InputStream?

    suspend fun getGeneralKml(): java.io.InputStream?

    suspend fun getOtherLanesKml(): java.io.InputStream?
}
