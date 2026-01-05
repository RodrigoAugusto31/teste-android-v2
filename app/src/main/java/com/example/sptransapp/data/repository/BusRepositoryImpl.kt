package com.example.sptransapp.data.repository

import android.content.Context
import com.example.sptransapp.BuildConfig
import com.example.sptransapp.R
import com.example.sptransapp.data.api.SPTransApi
import com.example.sptransapp.data.database.dao.CorridorDao
import com.example.sptransapp.data.database.dao.LineDao
import com.example.sptransapp.data.database.dao.StopDao
import com.example.sptransapp.data.database.entity.toEntity
import com.example.sptransapp.data.utils.KmlHelper
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Corridor
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.repository.BusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class BusRepositoryImpl(
    private val api: SPTransApi,
    private val lineDao: LineDao,
    private val stopDao: StopDao,
    private val corridorDao: CorridorDao,
    private val context: Context,
) : BusRepository {
    override suspend fun getPositions(): List<Bus> {
        val response = api.getPositions()

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

    override fun getStopsByLine(lineCode: Int): Flow<List<Stop>> =
        flow {
            val localData = stopDao.getStopsByLine(lineCode).first()
            if (localData.isNotEmpty()) {
                emit(localData.map { it.toDomain() })
            }

            try {
                val response = api.getStopsByLine(lineCode)
                if (response.isSuccessful && response.body() != null) {
                    val apiStops =
                        response.body()!!.map { dto ->
                            Stop(
                                stopCode = dto.stopCode,
                                name = "${dto.stopName} - ${dto.address ?: ""}",
                                latitude = dto.latitude,
                                longitude = dto.longitude,
                            )
                        }

                    stopDao.insertAll(apiStops.map { it.toEntity(lineCode) })
                    emit(apiStops)
                }
            } catch (e: Exception) {
                if (localData.isEmpty()) throw e
            }
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

    override fun getCorridors(): Flow<List<Corridor>> =
        flow {
            val localData = corridorDao.getAllCorridors().first()
            if (localData.isNotEmpty()) {
                emit(localData.map { it.toDomain() })
            }

            try {
                val response = api.getCorridors()
                if (response.isSuccessful && response.body() != null) {
                    val apiCorridors =
                        response.body()!!.map { dto ->
                            Corridor(dto.code ?: 0, dto.name ?: "")
                        }
                    corridorDao.insertAll(apiCorridors.map { it.toEntity() })
                    emit(apiCorridors)
                }
            } catch (e: Exception) {
                if (localData.isEmpty()) throw e
            }
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

    override fun getFavoriteLines(): Flow<List<Line>> =
        lineDao.getAllFavorites().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun isFavorite(lineCode: Int): Flow<Boolean> = lineDao.isFavorite(lineCode)

    override suspend fun toggleFavorite(line: Line) {
        val isFav = lineDao.isFavorite(line.lineCode).first()
        if (isFav) {
            lineDao.delete(line.toEntity())
        } else {
            lineDao.insert(line.toEntity())
        }
    }
}
