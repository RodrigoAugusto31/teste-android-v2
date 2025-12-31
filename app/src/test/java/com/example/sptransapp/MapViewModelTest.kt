package com.example.sptransapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.sptransapp.domain.model.Corredor
import com.example.sptransapp.domain.model.Linha
import com.example.sptransapp.domain.model.Onibus
import com.example.sptransapp.domain.model.Parada
import com.example.sptransapp.domain.model.Previsao
import com.example.sptransapp.domain.repository.OnibusRepository
import com.example.sptransapp.presentation.viewmodel.MapViewModel
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var repository: OnibusRepository
    private lateinit var viewModel: MapViewModel

    private val onibusObserver: Observer<List<Onibus>> = mockk(relaxed = true)
    private val loadingObserver: Observer<Boolean> = mockk(relaxed = true)
    private val errorObserver: Observer<String?> = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = mockk()
        viewModel = MapViewModel(repository, testDispatcher)

        viewModel.onibusList.observeForever(onibusObserver)
        viewModel.isLoading.observeForever(loadingObserver)
        viewModel.errorMessage.observeForever(errorObserver)
    }

    @Test
    fun `buscarOnibus DEVE atualizar lista e repetir APOS delay QUANDO sucesso`() =
        runTest(testDispatcher) {
            val listaOnibus = listOf(mockk<Onibus>())
            coEvery { repository.buscarPosicoes() } returns listaOnibus

            viewModel.buscarOnibus()

            advanceTimeBy(100)
            verify { loadingObserver.onChanged(true) }
            verify { onibusObserver.onChanged(listaOnibus) }
            verify { loadingObserver.onChanged(false) }

            advanceTimeBy(15_001)

            coVerify(atLeast = 2) { repository.buscarPosicoes() }

            viewModel.stopRefresh()
        }

    @Test
    fun `buscarOnibus DEVE setar mensagem de erro QUANDO repositorio falha`() =
        runTest(testDispatcher) {
            val errorMsg = "Erro de conexão"
            coEvery { repository.buscarPosicoes() } throws Exception(errorMsg)

            viewModel.buscarOnibus()
            advanceTimeBy(100)

            verify { loadingObserver.onChanged(true) }
            verify { errorObserver.onChanged("Erro na atualização: $errorMsg") }
            verify { loadingObserver.onChanged(false) }

            viewModel.stopRefresh()
        }

    @Test
    fun `pesquisarLinha NAO DEVE chamar repositorio QUANDO termo eh vazio`() =
        runTest(testDispatcher) {
            viewModel.pesquisarLinha("")

            coVerify(exactly = 0) { repository.buscarLinhas(any()) }
            assertEquals(null, viewModel.isLoading.value)
        }

    @Test
    fun `pesquisarLinha DEVE retornar linhas QUANDO sucesso`() =
        runTest(testDispatcher) {
            val termo = "8000"
            val listaLinhas = listOf(mockk<Linha>())
            coEvery { repository.buscarLinhas(termo) } returns listaLinhas

            viewModel.pesquisarLinha(termo)
            advanceTimeBy(100)

            assertEquals(listaLinhas, viewModel.linhasEncontradas.value)
            assertNull(viewModel.errorMessage.value)
        }

    @Test
    fun `pesquisarLinha DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.buscarLinhas(any()) } throws Exception("Erro Busca")

            viewModel.pesquisarLinha("teste")
            advanceTimeBy(100)

            assertEquals("Erro na busca: Erro Busca", viewModel.errorMessage.value)
        }

    @Test
    fun `carregarOnibusDaLinha DEVE carregar paradas E iniciar refresh de onibus`() =
        runTest(testDispatcher) {
            val codigoLinha = 123
            val listaParadas = listOf(mockk<Parada>())
            val listaOnibus = listOf(mockk<Onibus>())

            coEvery { repository.buscarParadasPorLinha(codigoLinha) } returns listaParadas
            coEvery { repository.buscarPosicoesPorLinha(codigoLinha) } returns listaOnibus

            viewModel.carregarOnibusDaLinha(codigoLinha)
            advanceTimeBy(100)

            assertEquals(listaParadas, viewModel.paradasList.value)
            assertEquals(listaOnibus, viewModel.onibusList.value)

            viewModel.stopRefresh()
        }

    @Test
    fun `carregarOnibusDaLinha DEVE ignorar erro de paradas MAS continuar buscando onibus`() =
        runTest(testDispatcher) {
            val codigoLinha = 123

            coEvery { repository.buscarParadasPorLinha(codigoLinha) } throws Exception("Erro Parada")
            coEvery { repository.buscarPosicoesPorLinha(codigoLinha) } returns emptyList()

            viewModel.carregarOnibusDaLinha(codigoLinha)
            advanceTimeBy(100)

            coVerify { repository.buscarPosicoesPorLinha(codigoLinha) }

            viewModel.stopRefresh()
        }

    @Test
    fun `buscarPrevisaoDaParada DEVE atualizar previsoes QUANDO sucesso`() =
        runTest(testDispatcher) {
            val codigoParada = 999
            val listaPrevisoes = listOf(mockk<Previsao>())
            coEvery { repository.buscarPrevisaoParada(codigoParada) } returns listaPrevisoes

            viewModel.buscarPrevisaoDaParada(codigoParada)
            advanceTimeBy(100)

            assertEquals(listaPrevisoes, viewModel.previsoes.value)
        }

    @Test
    fun `buscarPrevisaoDaParada DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.buscarPrevisaoParada(any()) } throws Exception("Fail")

            viewModel.buscarPrevisaoDaParada(1)
            advanceTimeBy(100)

            assertEquals("Erro na previsão: Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `buscarListaCorredores DEVE atualizar lista QUANDO sucesso`() =
        runTest(testDispatcher) {
            val listaCorredores = listOf(mockk<Corredor>())
            coEvery { repository.buscarCorredores() } returns listaCorredores

            viewModel.buscarListaCorredores()
            advanceTimeBy(100)

            assertEquals(listaCorredores, viewModel.corredores.value)
        }

    @Test
    fun `buscarListaCorredores DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.buscarCorredores() } throws Exception("Fail")
            viewModel.buscarListaCorredores()
            advanceTimeBy(100)
            assertEquals("Erro ao buscar corredores: Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `carregarMapaCorredores DEVE atualizar kmlData QUANDO sucesso`() =
        runTest(testDispatcher) {
            val inputStream = mockk<InputStream>()
            coEvery { repository.buscarKmlCorredores() } returns inputStream

            viewModel.carregarMapaCorredores()
            advanceTimeBy(100)

            assertEquals(inputStream, viewModel.kmlData.value)
        }

    @Test
    fun `carregarMapaCorredores DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.buscarKmlCorredores() } throws Exception("KML Fail")
            viewModel.carregarMapaCorredores()
            advanceTimeBy(100)
            assertEquals("Erro ao baixar mapa de corredores: KML Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `carregarMapaGeral DEVE atualizar kmlData QUANDO sucesso`() =
        runTest(testDispatcher) {
            val inputStream = mockk<InputStream>()
            coEvery { repository.buscarKmlGeral() } returns inputStream

            viewModel.carregarMapaGeral()
            advanceTimeBy(100)

            assertEquals(inputStream, viewModel.kmlData.value)
        }

    @Test
    fun `carregarMapaGeral DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.buscarKmlGeral() } throws Exception("General Fail")
            viewModel.carregarMapaGeral()
            advanceTimeBy(100)
            assertEquals("Erro ao baixar mapa geral: General Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `stopRefresh DEVE cancelar o job`() =
        runTest(testDispatcher) {
            coEvery { repository.buscarPosicoes() } returns emptyList()
            viewModel.buscarOnibus()
            advanceTimeBy(100)

            viewModel.stopRefresh()

            clearMocks(repository, answers = false)
            advanceTimeBy(20_000)
            coVerify(exactly = 0) { repository.buscarPosicoes() }
        }
}
