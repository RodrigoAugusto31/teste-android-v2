package com.example.sptransapp

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.usecase.CheckIfFavoriteUseCase
import com.example.sptransapp.domain.usecase.GetBusPositionsByLineUseCase
import com.example.sptransapp.domain.usecase.GetMapLayerUseCase
import com.example.sptransapp.domain.usecase.GetStopPredictionsUseCase
import com.example.sptransapp.domain.usecase.GetStopsUseCase
import com.example.sptransapp.domain.usecase.ToggleFavoriteUseCase
import com.example.sptransapp.presentation.viewmodel.LineDetailsViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class LineDetailsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var getBusPositionsByLineUseCase: GetBusPositionsByLineUseCase
    private lateinit var getStopPredictionsUseCase: GetStopPredictionsUseCase
    private lateinit var getStopsUseCase: GetStopsUseCase
    private lateinit var getMapLayerUseCase: GetMapLayerUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var checkIfFavoriteUseCase: CheckIfFavoriteUseCase
    private lateinit var context: Context
    private lateinit var viewModel: LineDetailsViewModel

    @Before
    fun setup() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        getBusPositionsByLineUseCase = mockk()
        getStopPredictionsUseCase = mockk()
        getStopsUseCase = mockk()
        getMapLayerUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        checkIfFavoriteUseCase = mockk()
        context = mockk(relaxed = true)

        viewModel =
            LineDetailsViewModel(
                getBusPositionsByLineUseCase,
                getStopPredictionsUseCase,
                getStopsUseCase,
                getMapLayerUseCase,
                toggleFavoriteUseCase,
                checkIfFavoriteUseCase,
                context,
            )
    }

    @After
    fun tearDown() {
        unmockkStatic(Dispatchers::class)
    }

    @Test
    fun `startMonitoring DEVE carregar paradas, checar favoritos E iniciar loop de onibus`() =
        runTest(testDispatcher) {
            val lineCode = 123
            val stops = listOf(mockk<Stop>())
            val buses = listOf(mockk<Bus>())

            every { getStopsUseCase(lineCode) } returns flowOf(stops)
            every { checkIfFavoriteUseCase(lineCode) } returns flowOf(true)
            coEvery { getBusPositionsByLineUseCase(lineCode) } returns buses

            val stopObserver = mockk<Observer<List<Stop>>>(relaxed = true)
            val favObserver = mockk<Observer<Boolean>>(relaxed = true)
            viewModel.stopList.observeForever(stopObserver)
            viewModel.isFavorite.observeForever(favObserver)

            val job = launch(UnconfinedTestDispatcher()) { viewModel.busList.collect {} }

            viewModel.startMonitoring(lineCode)

            advanceTimeBy(1000)

            verify { stopObserver.onChanged(stops) }
            verify { favObserver.onChanged(true) }

            assertEquals(buses, viewModel.busList.value)

            viewModel.stopList.removeObserver(stopObserver)
            viewModel.isFavorite.removeObserver(favObserver)
            job.cancel()
        }

    @Test
    fun `fetchStopPredictions DEVE atualizar previsoes com sucesso`() =
        runTest(testDispatcher) {
            val stopCode = 999
            val predictions = listOf(mockk<Prediction>())
            coEvery { getStopPredictionsUseCase(stopCode) } returns predictions

            val observer = mockk<Observer<List<Prediction>>>(relaxed = true)
            viewModel.predictions.observeForever(observer)

            viewModel.fetchStopPredictions(stopCode)
            advanceUntilIdle()

            verify { observer.onChanged(predictions) }
            viewModel.predictions.removeObserver(observer)
        }

    @Test
    fun `loadMapLayer DEVE atualizar kmlData com sucesso`() =
        runTest(testDispatcher) {
            val layerId = 10
            val inputStream = mockk<InputStream>()
            coEvery { getMapLayerUseCase(layerId) } returns inputStream

            val observer = mockk<Observer<InputStream?>>(relaxed = true)
            viewModel.kmlData.observeForever(observer)

            viewModel.loadMapLayer(layerId)
            advanceUntilIdle()

            verify { observer.onChanged(inputStream) }
            viewModel.kmlData.removeObserver(observer)
        }

    @Test
    fun `toggleFavorite DEVE chamar useCase`() =
        runTest(testDispatcher) {
            val line = mockk<Line>()
            coEvery { toggleFavoriteUseCase(line) } just Runs

            viewModel.toggleFavorite(line)
            advanceUntilIdle()

            coVerify { toggleFavoriteUseCase(line) }
        }
}
