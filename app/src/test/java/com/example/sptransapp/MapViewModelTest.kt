package com.example.sptransapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.sptransapp.domain.model.Corridor
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.repository.BusRepository
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

    private lateinit var repository: BusRepository
    private lateinit var viewModel: MapViewModel

    private val onibusObserver: Observer<List<Bus>> = mockk(relaxed = true)
    private val loadingObserver: Observer<Boolean> = mockk(relaxed = true)
    private val errorObserver: Observer<String?> = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = mockk()
        viewModel = MapViewModel(repository, testDispatcher)

        viewModel.busList.observeForever(onibusObserver)
        viewModel.isLoading.observeForever(loadingObserver)
        viewModel.errorMessage.observeForever(errorObserver)
    }

    @Test
    fun `buscarOnibus DEVE atualizar lista e repetir APOS delay QUANDO sucesso`() =
        runTest(testDispatcher) {
            val listaOnibus = listOf(mockk<Bus>())
            coEvery { repository.getPositions() } returns listaOnibus

            viewModel.fetchBuses()

            advanceTimeBy(100)
            verify { loadingObserver.onChanged(true) }
            verify { onibusObserver.onChanged(listaOnibus) }
            verify { loadingObserver.onChanged(false) }

            advanceTimeBy(15_001)

            coVerify(atLeast = 2) { repository.getPositions() }

            viewModel.stopRefresh()
        }

    @Test
    fun `buscarOnibus DEVE setar mensagem de erro QUANDO repositorio falha`() =
        runTest(testDispatcher) {
            val errorMsg = "Erro de conexão"
            coEvery { repository.getPositions() } throws Exception(errorMsg)

            viewModel.fetchBuses()
            advanceTimeBy(100)

            verify { loadingObserver.onChanged(true) }
            verify { errorObserver.onChanged("Erro na atualização: $errorMsg") }
            verify { loadingObserver.onChanged(false) }

            viewModel.stopRefresh()
        }

    @Test
    fun `pesquisarLinha NAO DEVE chamar repositorio QUANDO termo eh vazio`() =
        runTest(testDispatcher) {
            viewModel.searchLine("")

            coVerify(exactly = 0) { repository.searchLines(any()) }
            assertEquals(null, viewModel.isLoading.value)
        }

    @Test
    fun `pesquisarLinha DEVE retornar linhas QUANDO sucesso`() =
        runTest(testDispatcher) {
            val termo = "8000"
            val listaLinhas = listOf(mockk<Line>())
            coEvery { repository.searchLines(termo) } returns listaLinhas

            viewModel.searchLine(termo)
            advanceTimeBy(100)

            assertEquals(listaLinhas, viewModel.foundLines.value)
            assertNull(viewModel.errorMessage.value)
        }

    @Test
    fun `pesquisarLinha DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.searchLines(any()) } throws Exception("Erro Busca")

            viewModel.searchLine("teste")
            advanceTimeBy(100)

            assertEquals("Erro na busca: Erro Busca", viewModel.errorMessage.value)
        }

    @Test
    fun `carregarOnibusDaLinha DEVE carregar paradas E iniciar refresh de onibus`() =
        runTest(testDispatcher) {
            val codigoLinha = 123
            val listaParadas = listOf(mockk<Stop>())
            val listaOnibus = listOf(mockk<Bus>())

            coEvery { repository.getStopsByLine(codigoLinha) } returns listaParadas
            coEvery { repository.getPositionsByLine(codigoLinha) } returns listaOnibus

            viewModel.loadBusesByLine(codigoLinha)
            advanceTimeBy(100)

            assertEquals(listaParadas, viewModel.stopList.value)
            assertEquals(listaOnibus, viewModel.busList.value)

            viewModel.stopRefresh()
        }

    @Test
    fun `carregarOnibusDaLinha DEVE ignorar erro de paradas MAS continuar buscando onibus`() =
        runTest(testDispatcher) {
            val codigoLinha = 123

            coEvery { repository.getStopsByLine(codigoLinha) } throws Exception("Erro Stop")
            coEvery { repository.getPositionsByLine(codigoLinha) } returns emptyList()

            viewModel.loadBusesByLine(codigoLinha)
            advanceTimeBy(100)

            coVerify { repository.getPositionsByLine(codigoLinha) }

            viewModel.stopRefresh()
        }

    @Test
    fun `buscarPrevisaoDaParada DEVE atualizar previsoes QUANDO sucesso`() =
        runTest(testDispatcher) {
            val codigoParada = 999
            val listaPrevisoes = listOf(mockk<Prediction>())
            coEvery { repository.getStopPredictions(codigoParada) } returns listaPrevisoes

            viewModel.fetchStopPredictions(codigoParada)
            advanceTimeBy(100)

            assertEquals(listaPrevisoes, viewModel.predictions.value)
        }

    @Test
    fun `buscarPrevisaoDaParada DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.getStopPredictions(any()) } throws Exception("Fail")

            viewModel.fetchStopPredictions(1)
            advanceTimeBy(100)

            assertEquals("Erro na previsão: Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `buscarListaCorredores DEVE atualizar lista QUANDO sucesso`() =
        runTest(testDispatcher) {
            val listaCorredores = listOf(mockk<Corridor>())
            coEvery { repository.getCorridors() } returns listaCorredores

            viewModel.fetchCorridors()
            advanceTimeBy(100)

            assertEquals(listaCorredores, viewModel.corridors.value)
        }

    @Test
    fun `buscarListaCorredores DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.getCorridors() } throws Exception("Fail")
            viewModel.fetchCorridors()
            advanceTimeBy(100)
            assertEquals("Erro ao buscar corredores: Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `carregarMapaCorredores DEVE atualizar kmlData QUANDO sucesso`() =
        runTest(testDispatcher) {
            val inputStream = mockk<InputStream>()
            coEvery { repository.getCorridorsKml() } returns inputStream

            viewModel.loadCorridorsMap()
            advanceTimeBy(100)

            assertEquals(inputStream, viewModel.kmlData.value)
        }

    @Test
    fun `carregarMapaCorredores DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.getCorridorsKml() } throws Exception("KML Fail")
            viewModel.loadCorridorsMap()
            advanceTimeBy(100)
            assertEquals("Erro ao baixar mapa de corredores: KML Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `carregarMapaGeral DEVE atualizar kmlData QUANDO sucesso`() =
        runTest(testDispatcher) {
            val inputStream = mockk<InputStream>()
            coEvery { repository.getGeneralKml() } returns inputStream

            viewModel.loadGeneralMap()
            advanceTimeBy(100)

            assertEquals(inputStream, viewModel.kmlData.value)
        }

    @Test
    fun `carregarMapaGeral DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            coEvery { repository.getGeneralKml() } throws Exception("General Fail")
            viewModel.loadGeneralMap()
            advanceTimeBy(100)
            assertEquals("Erro ao baixar mapa geral: General Fail", viewModel.errorMessage.value)
        }

    @Test
    fun `stopRefresh DEVE cancelar o job`() =
        runTest(testDispatcher) {
            coEvery { repository.getPositions() } returns emptyList()
            viewModel.fetchBuses()
            advanceTimeBy(100)

            viewModel.stopRefresh()

            clearMocks(repository, answers = false)
            advanceTimeBy(20_000)
            coVerify(exactly = 0) { repository.getPositions() }
        }
}
