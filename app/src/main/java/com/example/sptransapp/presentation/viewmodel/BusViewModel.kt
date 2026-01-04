package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.usecase.GetBusPositionsByLineUseCase
import com.example.sptransapp.domain.usecase.GetBusPositionsUseCase
import com.example.sptransapp.domain.usecase.SearchLinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class BusViewModel
    @Inject
    constructor(
        private val getBusPositionsUseCase: GetBusPositionsUseCase,
        private val getBusPositionsByLineUseCase: GetBusPositionsByLineUseCase,
        private val searchLinesUseCase: SearchLinesUseCase,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private sealed class BusFilter {
            object All : BusFilter()

            data class ByLine(
                val lineCode: Int,
            ) : BusFilter()
        }

        private val _currentFilter = MutableStateFlow<BusFilter?>(null)

        private val _foundLines = MutableLiveData<List<Line>>()
        val foundLines: LiveData<List<Line>> = _foundLines

        private val _selectedLine = MutableLiveData<Line?>()
        val selectedLine: LiveData<Line?> = _selectedLine

        private val _kmlData = MutableLiveData<InputStream?>()
        val kmlData: LiveData<InputStream?> = _kmlData

        private val _isLoadingFlow = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoadingFlow

        private val _errorMessage = MutableLiveData<String?>()
        val errorMessage: LiveData<String?> = _errorMessage

        var shouldMoveCamera = false

        @OptIn(ExperimentalCoroutinesApi::class)
        val busList: StateFlow<List<Bus>> =
            _currentFilter
                .flatMapLatest { filter ->
                    if (filter == null) {
                        flow { emit(emptyList()) }
                    } else {
                        flow {
                            while (true) {
                                _isLoadingFlow.value = true
                                try {
                                    val result =
                                        when (filter) {
                                            is BusFilter.All -> getBusPositionsUseCase()
                                            is BusFilter.ByLine -> getBusPositionsByLineUseCase(filter.lineCode)
                                        }
                                    emit(result)
                                    _errorMessage.postValue(null)
                                } catch (_: Exception) {
                                    _errorMessage.postValue(null)
                                } finally {
                                    _isLoadingFlow.value = false
                                }
                                delay(15_000)
                            }
                        }
                    }
                }.flowOn(Dispatchers.IO)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )

        fun fetchInitialBuses() {
            shouldMoveCamera = false
            _selectedLine.value = null
            _currentFilter.value = BusFilter.All
        }

        fun searchLine(query: String) {
            if (query.isBlank()) return
            viewModelScope.launch {
                _isLoadingFlow.value = true
                try {
                    val result =
                        withContext(Dispatchers.IO) {
                            searchLinesUseCase(query)
                        }
                    _foundLines.value = result
                } catch (e: Exception) {
                    _errorMessage.value = context.getString(R.string.search_error_message, e.message)
                } finally {
                    _isLoadingFlow.value = false
                }
            }
        }

        fun loadBusesByLine(line: Line) {
            shouldMoveCamera = true
            _selectedLine.value = line
            _currentFilter.value = BusFilter.ByLine(line.lineCode)
        }

        fun clearFilter() {
            _selectedLine.value = null
            fetchInitialBuses()
        }

        fun clearSearchResults() {
            _foundLines.value = emptyList()
        }
    }
