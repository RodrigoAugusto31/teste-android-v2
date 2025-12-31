package com.example.sptransapp.data.repository

import android.util.Log
import com.example.sptransapp.BuildConfig
import com.example.sptransapp.data.api.SPTransApi
import com.example.sptransapp.data.utils.KmlHelper
import com.example.sptransapp.domain.model.Corredor
import com.example.sptransapp.domain.model.Linha
import com.example.sptransapp.domain.model.Onibus
import com.example.sptransapp.domain.model.Parada
import com.example.sptransapp.domain.model.Previsao
import com.example.sptransapp.domain.repository.OnibusRepository

class OnibusRepositoryImpl(
    private val api: SPTransApi
) : OnibusRepository {

    override suspend fun buscarPosicoes(): List<Onibus> {
        var response = api.getPositions()

        if (!response.isSuccessful || response.body() == null) {
            Log.d("REPO", "Tentando re-autenticar...")
            val login = api.authenticate(BuildConfig.SPTRANS_TOKEN)

            if (login.isSuccessful && login.body() == true) {
                response = api.getPositions()
            } else {
                throw Exception("Falha ao autenticar na SPTrans")
            }
        }

        val dados = response.body() ?: return emptyList()
        val listaOnibusDomain = mutableListOf<Onibus>()

        dados.linhas.forEach { linha ->
            linha.veiculos.forEach { veiculo ->
                listaOnibusDomain.add(
                    Onibus(
                        prefixo = veiculo.prefixo,
                        latitude = veiculo.latitude,
                        longitude = veiculo.longitude,
                        letreiro = linha.letreiroCompleto,
                        sentido = linha.destino,
                        isAcessivel = veiculo.acessivel
                    )
                )
            }
        }

        return listaOnibusDomain
    }

    override suspend fun buscarLinhas(termo: String): List<Linha> {
        val response = api.buscarLinhas(termo)

        return response.body()?.map { dto ->
            Linha(
                codigoLinha = dto.codigoLinha,
                letreiroCompleto = "${dto.letreiroPrimeiro}-${dto.letreiroSegundo}",
                nome = if (dto.sentido == 1) "${dto.letreiroPrincipal} -> ${dto.letreiroSecundario}"
                else "${dto.letreiroSecundario} -> ${dto.letreiroPrincipal}",
                sentido = dto.sentido
            )
        } ?: emptyList()
    }

    override suspend fun buscarPosicoesPorLinha(codigoLinha: Int): List<Onibus> {
        var response = api.getPosicoesPorLinha(codigoLinha)

        if (!response.isSuccessful || response.body() == null) {
            val login = api.authenticate(BuildConfig.SPTRANS_TOKEN)
            if (login.isSuccessful && login.body() == true) {
                response = api.getPosicoesPorLinha(codigoLinha)
            } else {
                return emptyList()
            }
        }

        val dados = response.body() ?: return emptyList()
        val listaOnibusDomain = mutableListOf<Onibus>()

        if (dados.veiculos != null) {
            dados.veiculos.forEach { veiculo ->
                listaOnibusDomain.add(
                    Onibus(
                        prefixo = veiculo.prefixo,
                        latitude = veiculo.latitude,
                        longitude = veiculo.longitude,
                        letreiro = "Linha Selecionada",
                        sentido = "Destino",
                        isAcessivel = veiculo.acessivel
                    )
                )
            }
        }
        else {
            dados.linhas.forEach { linha ->
                linha.veiculos.forEach { veiculo ->
                    listaOnibusDomain.add(
                        Onibus(
                            prefixo = veiculo.prefixo,
                            latitude = veiculo.latitude,
                            longitude = veiculo.longitude,
                            letreiro = linha.letreiroCompleto,
                            sentido = linha.destino,
                            isAcessivel = veiculo.acessivel
                        )
                    )
                }
            }
        }

        return listaOnibusDomain
    }

    override suspend fun buscarParadasPorLinha(codigoLinha: Int): List<Parada> {
        val response = api.getParadasPorLinha(codigoLinha)

        return response.body()?.map { dto ->
            Parada(
                codigo = dto.codigoParada,
                nome = "${dto.nomeParada} - ${dto.endereco ?: ""}",
                latitude = dto.latitude,
                longitude = dto.longitude
            )
        } ?: emptyList()
    }

    override suspend fun buscarPrevisaoParada(codigoParada: Int): List<Previsao> {
        val response = api.getPrevisaoParada(codigoParada)

        val dados = response.body() ?: return emptyList()
        val listaPrevisoes = mutableListOf<Previsao>()

        dados.parada?.linhas?.forEach { linha ->
            linha.veiculos?.forEach { veiculo ->
                listaPrevisoes.add(
                    Previsao(
                        linha = linha.letreiro ?: "",
                        destino = linha.destino ?: "",
                        horarioChegada = veiculo.previsaoChegada ?: "--:--",
                        prefixoVeiculo = veiculo.prefixo ?: ""
                    )
                )
            }
        }
        return listaPrevisoes
    }

    override suspend fun buscarCorredores(): List<Corredor> {
        val response = api.getCorredores()

        return response.body()?.map { dto ->
            Corredor(
                codigo = dto.codigo ?: 0,
                nome = dto.nome ?: "Sem Nome"
            )
        } ?: emptyList()
    }

    override suspend fun buscarKmlCorredores(): java.io.InputStream? {
        val response = api.getCorredoresKMZ()
        if (response.isSuccessful && response.body() != null) {
            return KmlHelper.extractKmlFromKmz(response.body()!!)
        }
        return null
    }

    override suspend fun buscarKmlGeral(): java.io.InputStream? {
        val response = api.getKmzGeral()
        if (response.isSuccessful && response.body() != null) {
            return KmlHelper.extractKmlFromKmz(response.body()!!)
        }
        return null
    }
}