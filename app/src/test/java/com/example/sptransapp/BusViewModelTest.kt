package com.example.sptransapp

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.usecase.GetBusPositionsByLineUseCase
import com.example.sptransapp.domain.usecase.GetBusPositionsUseCase
import com.example.sptransapp.domain.usecase.SearchLinesUseCase
import com.example.sptransapp.presentation.viewmodel.BusViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BusViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var getBusPositionsUseCase: GetBusPositionsUseCase
    private lateinit var getBusPositionsByLineUseCase: GetBusPositionsByLineUseCase
    private lateinit var searchLinesUseCase: SearchLinesUseCase
    private lateinit var context: Context
    private lateinit var viewModel: BusViewModel

    @Before
    fun setup() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        getBusPositionsUseCase = mockk()
        getBusPositionsByLineUseCase = mockk()
        searchLinesUseCase = mockk()
        context = mockk(relaxed = true)

        viewModel =
            BusViewModel(
                getBusPositionsUseCase,
                getBusPositionsByLineUseCase,
                searchLinesUseCase,
                context,
            )
    }

    @After
    fun tearDown() {
        unmockkStatic(Dispatchers::class)
    }

    @Test
    fun `fetchInitialBuses DEVE iniciar polling de TODOS os onibus`() =
        runTest(testDispatcher) {
            val buses = listOf(mockk<Bus>())
            coEvery { getBusPositionsUseCase() } returns buses

            val job = launch(UnconfinedTestDispatcher()) { viewModel.busList.collect {} }

            viewModel.fetchInitialBuses()

            advanceTimeBy(1000)

            assertEquals(buses, viewModel.busList.value)

            advanceTimeBy(15_001)
            coVerify(atLeast = 2) { getBusPositionsUseCase() }

            job.cancel()
        }

    @Test
    fun `loadBusesByLine DEVE iniciar polling filtrado por linha`() =
        runTest(testDispatcher) {
            val line = mockk<Line> { every { lineCode } returns 123 }
            val buses = listOf(mockk<Bus>())

            coEvery { getBusPositionsByLineUseCase(123) } returns buses

            val job = launch(UnconfinedTestDispatcher()) { viewModel.busList.collect {} }

            viewModel.loadBusesByLine(line)

            advanceTimeBy(1000)

            assertEquals(buses, viewModel.busList.value)
            coVerify { getBusPositionsByLineUseCase(123) }

            job.cancel()
        }

    @Test
    fun `searchLine DEVE retornar linhas QUANDO sucesso`() =
        runTest(testDispatcher) {
            val query = "8000"
            val lines = listOf(mockk<Line>())
            coEvery { searchLinesUseCase(query) } returns lines

            val observer = mockk<Observer<List<Line>>>(relaxed = true)
            viewModel.foundLines.observeForever(observer)

            viewModel.searchLine(query)

            advanceTimeBy(1000)

            verify { observer.onChanged(lines) }
            viewModel.foundLines.removeObserver(observer)
        }

    @Test
    fun `searchLine DEVE setar erro QUANDO falha`() =
        runTest(testDispatcher) {
            val query = "Erro"
            val errorMsg = "Network Error"
            coEvery { searchLinesUseCase(query) } throws Exception(errorMsg)
            every { context.getString(R.string.search_error_message, errorMsg) } returns "Erro Formatado"

            val observer = mockk<Observer<String?>>(relaxed = true)
            viewModel.errorMessage.observeForever(observer)

            viewModel.searchLine(query)

            advanceTimeBy(1000)

            verify { observer.onChanged("Erro Formatado") }
            viewModel.errorMessage.removeObserver(observer)
        }
}
