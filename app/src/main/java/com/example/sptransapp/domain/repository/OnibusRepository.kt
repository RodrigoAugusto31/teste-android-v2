package com.example.sptransapp.domain.repository

import com.example.sptransapp.domain.model.Corredor
import com.example.sptransapp.domain.model.Linha
import com.example.sptransapp.domain.model.Onibus
import com.example.sptransapp.domain.model.Parada
import com.example.sptransapp.domain.model.Previsao

interface OnibusRepository {
    suspend fun buscarPosicoes(): List<Onibus>

    suspend fun buscarLinhas(termo: String): List<Linha>

    suspend fun buscarPosicoesPorLinha(codigoLinha: Int): List<Onibus>

    suspend fun buscarParadasPorLinha(codigoLinha: Int): List<Parada>

    suspend fun buscarPrevisaoParada(codigoParada: Int): List<Previsao>

    suspend fun buscarCorredores(): List<Corredor>

    suspend fun buscarKmlCorredores(): java.io.InputStream?

    suspend fun buscarKmlGeral(): java.io.InputStream?

    suspend fun buscarKmlOutrasVias(): java.io.InputStream?
}
