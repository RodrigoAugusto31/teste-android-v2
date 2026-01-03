package com.example.sptransapp.data.api

import com.example.sptransapp.data.model.CorridorDto
import com.example.sptransapp.data.model.LineSearchDto
import com.example.sptransapp.data.model.StopDto
import com.example.sptransapp.data.model.PositionResponse
import com.example.sptransapp.data.model.PredictionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SPTransApi {
    @POST("Login/Autenticar")
    suspend fun authenticate(
        @Query("token") token: String,
    ): Response<Boolean>

    @GET("Posicao")
    suspend fun getPositions(): Response<PositionResponse>

    @GET("Linha/Buscar")
    suspend fun searchLines(
        @Query("termosBusca") termos: String,
    ): Response<List<LineSearchDto>>

    @GET("Posicao/Linha")
    suspend fun getPositionsByLine(
        @Query("codigoLinha") codigoLinha: Int,
    ): Response<PositionResponse>

    @GET("Parada/BuscarParadasPorLinha")
    suspend fun getStopsByLine(
        @Query("codigoLinha") codigoLinha: Int,
    ): Response<List<StopDto>>

    @GET("Previsao/Parada")
    suspend fun getStopPredictions(
        @Query("codigoParada") codigoParada: Int,
    ): Response<PredictionResponse>

    @GET("Corredor")
    suspend fun getCorridors(): Response<List<CorridorDto>>

    @GET("KMZ/Corredor")
    suspend fun getCorridorsKml(): Response<ResponseBody>

    @GET("KMZ")
    suspend fun getGeneralKml(): Response<ResponseBody>

    @GET("KMZ/OutrasVias")
    suspend fun getOtherLanesKml(): Response<ResponseBody>
}
