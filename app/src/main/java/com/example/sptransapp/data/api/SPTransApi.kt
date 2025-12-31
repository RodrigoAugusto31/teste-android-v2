package com.example.sptransapp.data.api

import com.example.sptransapp.data.model.CorredorDto
import com.example.sptransapp.data.model.LinhaBuscaDto
import com.example.sptransapp.data.model.ParadaDto
import com.example.sptransapp.data.model.PosicaoResponse
import com.example.sptransapp.data.model.PrevisaoResponse
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
    suspend fun getPositions(): Response<PosicaoResponse>

    @GET("Linha/Buscar")
    suspend fun buscarLinhas(
        @Query("termosBusca") termos: String,
    ): Response<List<LinhaBuscaDto>>

    @GET("Posicao/Linha")
    suspend fun getPosicoesPorLinha(
        @Query("codigoLinha") codigoLinha: Int,
    ): Response<PosicaoResponse>

    @GET("Parada/BuscarParadasPorLinha")
    suspend fun getParadasPorLinha(
        @Query("codigoLinha") codigoLinha: Int,
    ): Response<List<ParadaDto>>

    @GET("Previsao/Parada")
    suspend fun getPrevisaoParada(
        @Query("codigoParada") codigoParada: Int,
    ): Response<PrevisaoResponse>

    @GET("Corredor")
    suspend fun getCorredores(): Response<List<CorredorDto>>

    @GET("KMZ/Corredor")
    suspend fun getCorredoresKMZ(): Response<ResponseBody>

    @GET("KMZ")
    suspend fun getKmzGeral(): Response<ResponseBody>

    @GET("KMZ/OutrasVias")
    suspend fun getOutrasViasKMZ(): Response<ResponseBody>
}
