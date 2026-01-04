package com.example.sptransapp

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.usecase.GetStopPredictionsUseCase
import com.example.sptransapp.domain.usecase.GetStopsUseCase
import com.example.sptransapp.domain.usecase.SearchLinesUseCase
import com.example.sptransapp.presentation.viewmodel.StopsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StopsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var searchLinesUseCase: SearchLinesUseCase
    private lateinit var getStopsUseCase: GetStopsUseCase
    private lateinit var getStopPredictionsUseCase: GetStopPredictionsUseCase
    private lateinit var context: Context
    private lateinit var viewModel: StopsViewModel

    @Before
    fun setup() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        searchLinesUseCase = mockk()
        getStopsUseCase = mockk()
        getStopPredictionsUseCase = mockk()
        context = mockk(relaxed = true)

        viewModel =
            StopsViewModel(
                searchLinesUseCase,
                getStopsUseCase,
                getStopPredictionsUseCase,
                context,
            )
    }

    @After
    fun tearDown() {
        unmockkStatic(Dispatchers::class)
    }

    @Test
    fun `searchLine DEVE retornar linhas encontradas`() =
        runTest(testDispatcher) {
            val lines = listOf(mockk<Line>())
            coEvery { searchLinesUseCase("termo") } returns lines

            val observer = mockk<Observer<List<Line>>>(relaxed = true)
            viewModel.foundLines.observeForever(observer)

            viewModel.searchLine("termo")

            advanceUntilIdle()

            verify { observer.onChanged(lines) }
            viewModel.foundLines.removeObserver(observer)
        }

    @Test
    fun `loadStops DEVE atualizar lista de paradas`() =
        runTest(testDispatcher) {
            val stops = listOf(mockk<Stop>())
            every { getStopsUseCase(123) } returns flowOf(stops)

            val observer = mockk<Observer<List<Stop>>>(relaxed = true)
            viewModel.stopList.observeForever(observer)

            viewModel.loadStops(123)
            advanceUntilIdle()

            verify { observer.onChanged(stops) }
            viewModel.stopList.removeObserver(observer)
        }

    @Test
    fun `loadStops DEVE capturar erro do fluxo`() =
        runTest(testDispatcher) {
            val errorMsg = "Erro API"
            every { getStopsUseCase(any()) } returns flow { throw Exception(errorMsg) }
            every { context.getString(any(), any()) } returns "Erro Formatado"

            val observer = mockk<Observer<String?>>(relaxed = true)
            viewModel.errorMessage.observeForever(observer)

            viewModel.loadStops(123)
            advanceUntilIdle()

            verify { observer.onChanged("Erro Formatado") }
            viewModel.errorMessage.removeObserver(observer)
        }

    @Test
    fun `fetchStopPredictions DEVE atualizar previsoes`() =
        runTest(testDispatcher) {
            val predictions = listOf(mockk<Prediction>())
            coEvery { getStopPredictionsUseCase(55) } returns predictions

            val observer = mockk<Observer<List<Prediction>>>(relaxed = true)
            viewModel.predictions.observeForever(observer)

            viewModel.fetchStopPredictions(55)
            advanceUntilIdle()

            verify { observer.onChanged(predictions) }
            viewModel.predictions.removeObserver(observer)
        }

    @Test
    fun `clearMapData DEVE limpar listas`() =
        runTest(testDispatcher) {
            viewModel.clearMapData()

            assertEquals(emptyList<Stop>(), viewModel.stopList.value)
            assertEquals(emptyList<Prediction>(), viewModel.predictions.value)
        }
}
