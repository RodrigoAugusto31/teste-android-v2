package com.example.sptransapp

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.usecase.GetFavoritesUseCase
import com.example.sptransapp.domain.usecase.SearchLinesUseCase
import com.example.sptransapp.presentation.viewmodel.LinesViewModel
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LinesViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var searchLinesUseCase: SearchLinesUseCase
    private lateinit var getFavoritesUseCase: GetFavoritesUseCase
    private lateinit var context: Context
    private lateinit var viewModel: LinesViewModel

    @Before
    fun setup() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        searchLinesUseCase = mockk()
        getFavoritesUseCase = mockk()
        context = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Dispatchers::class)
    }

    private fun initViewModel() {
        viewModel = LinesViewModel(searchLinesUseCase, getFavoritesUseCase, context)
    }

    @Test
    fun `init DEVE carregar favoritos automaticamente`() =
        runTest(testDispatcher) {
            val favorites = listOf(mockk<Line>())
            every { getFavoritesUseCase() } returns flowOf(favorites)

            initViewModel()

            val observer = mockk<Observer<List<Line>>>(relaxed = true)
            viewModel.displayedLines.observeForever(observer)

            advanceUntilIdle()

            verify { observer.onChanged(favorites) }
            viewModel.displayedLines.removeObserver(observer)
        }

    @Test
    fun `searchLine DEVE buscar na API QUANDO query nao for vazia`() =
        runTest(testDispatcher) {
            every { getFavoritesUseCase() } returns flowOf(emptyList())
            initViewModel()

            val query = "8000"
            val searchResults = listOf(mockk<Line>())
            coEvery { searchLinesUseCase(query) } returns searchResults

            val observer = mockk<Observer<List<Line>>>(relaxed = true)
            viewModel.displayedLines.observeForever(observer)

            clearMocks(observer)

            viewModel.searchLine(query)
            advanceUntilIdle()

            verify { observer.onChanged(searchResults) }
            viewModel.displayedLines.removeObserver(observer)
        }

    @Test
    fun `searchLine DEVE recarregar favoritos QUANDO query for vazia`() =
        runTest(testDispatcher) {
            val favorites = listOf(mockk<Line>())
            every { getFavoritesUseCase() } returns flowOf(favorites)

            initViewModel()
            val observer = mockk<Observer<List<Line>>>(relaxed = true)
            viewModel.displayedLines.observeForever(observer)

            viewModel.searchLine("")
            advanceUntilIdle()

            verify(atLeast = 1) { observer.onChanged(favorites) }

            viewModel.displayedLines.removeObserver(observer)
        }
}
