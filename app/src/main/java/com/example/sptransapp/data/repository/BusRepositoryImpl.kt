package com.example.sptransapp.data.repository

import android.content.Context
import com.example.sptransapp.BuildConfig
import com.example.sptransapp.R
import com.example.sptransapp.data.api.SPTransApi
import com.example.sptransapp.data.utils.KmlHelper
import com.example.sptransapp.domain.model.Corridor
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.repository.BusRepository

class BusRepositoryImpl(
    private val api: SPTransApi,
    private val context: Context
) : BusRepository {

    override suspend fun getPositions(): List<Bus> {
        var response = api.getPositions()

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Falha ao buscar posições")
        }

        val data = response.body() ?: return emptyList()
        val domainBusList = mutableListOf<Bus>()

        data.lines.forEach { line ->
            line.vehicles.forEach { vehicle ->
                domainBusList.add(
                    Bus(
                        prefix = vehicle.prefix,
                        latitude = vehicle.latitude,
                        longitude = vehicle.longitude,
                        fullSign = line.fullSign,
                        destination = line.destination,
                        isAccessible = vehicle.isAccessible,
                    ),
                )
            }
        }

        return domainBusList
    }

    override suspend fun searchLines(term: String): List<Line> {
        val response = api.searchLines(term)

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Falha ao buscar posições")
        }

        return response.body()?.map { dto ->
            Line(
                lineCode = dto.lineCode,
                fullSign = "${dto.firstSign}-${dto.secondSign}",
                name =
                    if (dto.direction == 1) {
                        "${dto.mainSign} -> ${dto.secondarySign}"
                    } else {
                        "${dto.secondarySign} -> ${dto.secondarySign}"
                    },
                direction = dto.direction,
            )
        } ?: emptyList()
    }

    override suspend fun getPositionsByLine(lineCode: Int): List<Bus> {
        var response = api.getPositionsByLine(lineCode)

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Falha ao buscar posições")
        }

        if (!response.isSuccessful || response.body() == null) {
            val login = api.authenticate(BuildConfig.SPTRANS_TOKEN)
            if (login.isSuccessful && login.body() == true) {
                response = api.getPositionsByLine(lineCode)
            } else {
                return emptyList()
            }
        }

        val data = response.body() ?: return emptyList()
        val domainBusList = mutableListOf<Bus>()

        if (data.vehicles != null) {
            data.vehicles.forEach { vehicle ->
                domainBusList.add(
                    Bus(
                        prefix = vehicle.prefix,
                        latitude = vehicle.latitude,
                        longitude = vehicle.longitude,
                        fullSign = context.getString(R.string.selected_line),
                        destination = context.getString(R.string.destination_label),
                        isAccessible = vehicle.isAccessible,
                    ),
                )
            }
        } else {
            data.lines.forEach { line ->
                line.vehicles.forEach { vehicle ->
                    domainBusList.add(
                        Bus(
                            prefix = vehicle.prefix,
                            latitude = vehicle.latitude,
                            longitude = vehicle.longitude,
                            fullSign = line.fullSign,
                            destination = line.destination,
                            isAccessible = vehicle.isAccessible,
                        ),
                    )
                }
            }
        }

        return domainBusList
    }

    override suspend fun getStopsByLine(lineCode: Int): List<Stop> {
        val response = api.getStopsByLine(lineCode)

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Falha ao buscar posições")
        }

        return response.body()?.map { dto ->
            Stop(
                stopCode = dto.stopCode,
                name = "${dto.stopName} - ${dto.address ?: ""}",
                latitude = dto.latitude,
                longitude = dto.longitude,
            )
        } ?: emptyList()
    }

    override suspend fun getStopPredictions(lineCode: Int): List<Prediction> {
        val response = api.getStopPredictions(lineCode)

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Falha ao buscar posições")
        }

        val data = response.body() ?: return emptyList()
        val predictionList = mutableListOf<Prediction>()

        data.stop?.lines?.forEach { linha ->
            linha.vehicles?.forEach { vehicle ->
                predictionList.add(
                    Prediction(
                        line = linha.fullSign ?: "",
                        destination = linha.destination ?: "",
                        arrivalTime = vehicle.arrivalTime ?: "--:--",
                        vehiclePrefix = vehicle.prefix ?: "",
                    ),
                )
            }
        }
        return predictionList
    }

    override suspend fun getCorridors(): List<Corridor> {
        val response = api.getCorridors()

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Falha ao buscar posições")
        }

        return response.body()?.map { dto ->
            Corridor(
                code = dto.code ?: 0,
                name = dto.name ?: context.getString(R.string.no_name),
            )
        } ?: emptyList()
    }

    override suspend fun getCorridorsKml(): java.io.InputStream? {
        val response = api.getCorridorsKml()

        if (response.isSuccessful && response.body() != null) {
            return KmlHelper.extractKmlFromKmz(response.body()!!)
        }
        return null
    }

    override suspend fun getGeneralKml(): java.io.InputStream? {
        val response = api.getGeneralKml()
        if (response.isSuccessful && response.body() != null) {
            return KmlHelper.extractKmlFromKmz(response.body()!!)
        }
        return null
    }

    override suspend fun getOtherLanesKml(): java.io.InputStream? {
        val response = api.getOtherLanesKml()
        if (response.isSuccessful && response.body() != null) {
            return KmlHelper.extractKmlFromKmz(response.body()!!)
        }
        return null
    }
}
